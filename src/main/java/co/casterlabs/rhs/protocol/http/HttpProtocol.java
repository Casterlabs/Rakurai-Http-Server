package co.casterlabs.rhs.protocol.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.io.streams.LimitedInputStream;
import co.casterlabs.commons.io.streams.NonCloseableOutputStream;
import co.casterlabs.rhs.HttpStatus;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.DropConnectionException;
import co.casterlabs.rhs.protocol.HttpException;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.RHSProtocol;
import co.casterlabs.rhs.protocol.http.HttpProtocol.HttpProtoHandler;
import co.casterlabs.rhs.util.TaskExecutor.TaskType;

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
        try (response.content) {
            boolean kaRequested = false;

            switch (connection.httpVersion) {
                case HTTP_0_9:
                    break;

                case HTTP_1_0:
                    kaRequested = connection.headers.getOrDefault("Connection", Collections.emptyList())
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
                    kaRequested = connection.headers.getOrDefault("Connection", Collections.emptyList())
                        .stream()
                        .map((h) -> h.delimited(","))
                        .flatMap(Collection::stream)
                        .map((h) -> h.raw())
                        .filter((s) -> s.equalsIgnoreCase("close"))
                        .findAny()
                        .isEmpty();
                    break;
            }

            if (kaRequested && session.body().hasBody()) {
                // Eat any remaining body bytes.
                InputStream bodyStream = session.body().stream();
                while (bodyStream.read() != -1) {
                    bodyStream.skip(Long.MAX_VALUE); // Skip as much as possible.
                }
            }

            long length = response.content.length();
            String contentEncoding = null;
            ResponseMode responseMode = null;
            switch (connection.httpVersion) {
                case HTTP_0_9:
                    responseMode = ResponseMode.CLOSE_ON_COMPLETE;
                    break;

                case HTTP_1_0:
                    if (length == -1) {
                        responseMode = ResponseMode.CLOSE_ON_COMPLETE;
                    } else {
                        responseMode = ResponseMode.FIXED_LENGTH;
                        response.headers.put("Content-Length", String.valueOf(length));

                        if (kaRequested) {
                            response.headers.put("Connection", "keep-alive");
                            response.headers.put("Keep-Alive", "timeout=" + RHSConnection.HTTP_PERSISTENT_TIMEOUT);
                        }
                    }
                    break;

                case HTTP_1_1:
                    contentEncoding = _CompressionUtil.pickEncoding(connection, response);

                    if (length == -1 || contentEncoding != null) {
                        // Compressed responses should always be chunked.
                        response.headers.put("Transfer-Encoding", "chunked");
                        responseMode = ResponseMode.CHUNKED;
                    } else {
                        responseMode = ResponseMode.FIXED_LENGTH;
                        response.headers.put("Content-Length", String.valueOf(length));
                    }

                    if (kaRequested) {
                        // Add the keepalive headers.
                        response.headers.put("Connection", "keep-alive");
                        response.headers.put("Keep-Alive", "timeout=" + RHSConnection.HTTP_PERSISTENT_TIMEOUT);
                    } else {
                        // Let the client know that we will be closing the socket.
                        response.headers.put("Connection", "close");
                    }

                    if (contentEncoding != null) {
                        response.headers.put("Content-Encoding", contentEncoding);

                        if (response.headers.containsKey("Vary")) {
                            // We need to append instead.
                            response.headers.put("Vary", String.join(", ", response.headers.get("Vary"), "Accept-Encoding"));
                        } else {
                            response.headers.put("Vary", "Accept-Encoding");
                        }
                    }
                    break;
            }

            switch (connection.method) {
                case "HEAD":
                    // We must reply with the actual status code and content headers
                    // but SHOULD NOT send a body.
                    connection.respond(response.status, response.headers);
                    break;

                case "OPTIONS":
                    // We must reply with NO_CONTENT but NOT the body headers.
                    response.headers.remove("Transfer-Encoding");
                    response.headers.remove("Content-Length");
                    connection.respond(StandardHttpStatus.NO_CONTENT, response.headers);
                    break;

                default:
                    // Chunked output streams have special close implementations that don't actually
                    // close the underlying connection, they just signal that this is the end of the
                    // request.
                    try (OutputStream out = responseMode == ResponseMode.CHUNKED ? //
                        new _ChunkedOutputStream(connection.output) : // Chunked response
                        new NonCloseableOutputStream(connection.output) // Non-encoded response
                    ) {
                        connection.respond(response.status, response.headers);
                        _CompressionUtil.writeWithEncoding(contentEncoding, connection.guessedMtu, out, response.content);
                    }
                    break;
            }

            return kaRequested && responseMode != ResponseMode.CLOSE_ON_COMPLETE;
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
