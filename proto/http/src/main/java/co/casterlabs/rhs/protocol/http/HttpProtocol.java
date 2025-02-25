package co.casterlabs.rhs.protocol.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.io.streams.LimitedInputStream;
import co.casterlabs.commons.io.streams.NonCloseableOutputStream;
import co.casterlabs.rhs.HttpStatus;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.HeaderValue;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.RHSProtocol;
import co.casterlabs.rhs.protocol.exceptions.DropConnectionException;
import co.casterlabs.rhs.protocol.exceptions.HttpException;
import co.casterlabs.rhs.protocol.http.HttpProtocol.HttpProtoHandler;

public class HttpProtocol extends RHSProtocol<HttpSession, HttpResponse, HttpProtoHandler> {

    @Override
    public String name() {
        return "http";
    }

    @Override
    public @Nullable HttpSession accept(RHSConnection connection) throws IOException, HttpException, DropConnectionException {
        // Retrieve the body, if any.
        InputStream bodyInput = null;
        switch (connection.httpVersion) {
            case HTTP_1_1:
                // HTTP/1.1 handshaking header check.
                if (connection.uri.host.length() == 0) {
                    throw new HttpException(HttpStatus.adapt(400, "Missing Host header"));
                }

                // Look for a chunked body, otherwise fall through to normal fixed-length
                // behavior (1.0).
                if (connection.headers.getSingleOrDefault("Transfer-Encoding", HeaderValue.EMPTY).raw().equalsIgnoreCase("chunked")) {
                    bodyInput = new _ChunkedInputStream(connection);
                    connection.logger.debug("Detected chunked body.");
                    break;
                }

            case HTTP_1_0: {
                // If there's a Content-Length header then there's a body.
                HeaderValue contentLength = connection.headers.getSingle("Content-Length");
                if (contentLength != null) {
                    long lengthL = Long.parseLong(contentLength.raw());
                    if (lengthL == 0) break;

                    bodyInput = new LimitedInputStream(connection.input, lengthL);
                    connection.logger.debug("Detected fixed-length body.");
                }
                break;
            }

            default:
                break;
        }

        return new HttpSession(connection, bodyInput);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean process(HttpSession session, HttpResponse response, RHSConnection connection) throws IOException, HttpException, InterruptedException {
        boolean keepAliveEnabled = connection.keepAliveSeconds > 0;

        try (response.content) {
            boolean shouldKeepAlive = false;

            switch (connection.httpVersion) {
                case HTTP_0_9:
                    break;

                case HTTP_1_0:
                    shouldKeepAlive = connection.headers.getOrDefault("Connection", Collections.emptyList())
                        .stream()
                        .map((h) -> h.delimited(","))
                        .flatMap(Collection::stream)
                        .map((h) -> h.raw())
                        .filter((s) -> s.equalsIgnoreCase("keep-alive"))
                        .findAny()
                        .isPresent();
                    break;

                case HTTP_1_1:
                    // Keep Alive is default in HTTP/1.1. So we look for a connection close instead.
                    shouldKeepAlive = connection.headers.getOrDefault("Connection", Collections.emptyList())
                        .stream()
                        .map((h) -> h.delimited(","))
                        .flatMap(Collection::stream)
                        .map((h) -> h.raw())
                        .filter((s) -> s.equalsIgnoreCase("close"))
                        .findAny()
                        .isEmpty();
                    break;
            }

            if (!keepAliveEnabled) {
                shouldKeepAlive = false;
            }

            if (shouldKeepAlive && session.body().present()) {
                // Eat any remaining body bytes.
                InputStream bodyStream = session.body().stream();
                while (bodyStream.read() != -1) {
                    bodyStream.skip(Long.MAX_VALUE); // Skip as much as possible.
                }
            }

            long length = response.content.length();
            String contentEncoding = null;
            ResponseMode responseMode = null;
            Map<String, String> responseHeaders = new HashMap<>(response.headers); // Clone.

            switch (connection.httpVersion) {
                case HTTP_0_9:
                    responseMode = ResponseMode.CLOSE_ON_COMPLETE;
                    break;

                case HTTP_1_0:
                    if (length == -1) {
                        responseMode = ResponseMode.CLOSE_ON_COMPLETE;
                    } else {
                        responseMode = ResponseMode.FIXED_LENGTH;
                        responseHeaders.put("Content-Length", String.valueOf(length));

                        if (shouldKeepAlive) {
                            responseHeaders.put("Connection", "keep-alive");
                            responseHeaders.put("Keep-Alive", "timeout=" + connection.keepAliveSeconds);
                        }
                    }
                    break;

                case HTTP_1_1:
                    contentEncoding = _CompressionUtil.pickEncoding(connection, response);

                    if (length == -1 || contentEncoding != null) {
                        // Compressed responses should always be chunked.
                        responseHeaders.put("Transfer-Encoding", "chunked");
                        responseMode = ResponseMode.CHUNKED;
                    } else {
                        responseMode = ResponseMode.FIXED_LENGTH;
                        responseHeaders.put("Content-Length", String.valueOf(length));
                    }

                    if (shouldKeepAlive) {
                        // Add the keepalive headers.
                        responseHeaders.put("Connection", "keep-alive");
                        responseHeaders.put("Keep-Alive", "timeout=" + connection.keepAliveSeconds);
                    } else {
                        // Let the client know that we will be closing the socket.
                        responseHeaders.put("Connection", "close");
                    }

                    if (contentEncoding != null) {
                        responseHeaders.put("Content-Encoding", contentEncoding);

                        if (responseHeaders.containsKey("Vary")) {
                            // We need to append instead.
                            responseHeaders.put("Vary", String.join(", ", responseHeaders.get("Vary"), "Accept-Encoding"));
                        } else {
                            responseHeaders.put("Vary", "Accept-Encoding");
                        }
                    }
                    break;
            }

            switch (connection.method) {
                case "HEAD":
                    // We must reply with the actual status code and content headers
                    // but SHOULD NOT send a body.
                    connection.respond(response.status, responseHeaders);
                    break;

                case "OPTIONS":
                    // We must reply with NO_CONTENT but NOT the body headers.
                    responseHeaders.remove("Transfer-Encoding");
                    responseHeaders.remove("Content-Length");
                    connection.respond(StandardHttpStatus.NO_CONTENT, responseHeaders);
                    break;

                default:
                    // Chunked output streams have special close implementations that don't actually
                    // close the underlying connection, they just signal that this is the end of the
                    // request.
                    try (OutputStream out = responseMode == ResponseMode.CHUNKED ? //
                        new _ChunkedOutputStream(connection.output) : // Chunked response
                        new NonCloseableOutputStream(connection.output) // Non-encoded response
                    ) {
                        connection.respond(response.status, responseHeaders);
                        _CompressionUtil.writeWithEncoding(contentEncoding, connection.guessedMtu, out, response.content);
                    }
                    break;
            }

            return shouldKeepAlive && responseMode != ResponseMode.CLOSE_ON_COMPLETE;
        }
    }

    private static enum ResponseMode {
        CLOSE_ON_COMPLETE,
        FIXED_LENGTH,
        CHUNKED
    }

    @Override
    public HttpResponse handle(HttpSession session, HttpProtoHandler handler) throws DropConnectionException, HttpException {
        return handler.handle(session);
    }

    public static interface HttpProtoHandler {

        /**
         * This is called in a {@link TaskType#LIGHT_IO} context.
         */
        public HttpResponse handle(HttpSession session) throws HttpException, DropConnectionException;

    }

}
