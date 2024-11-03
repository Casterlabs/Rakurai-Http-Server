package co.casterlabs.rhs.protocol.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import co.casterlabs.commons.io.streams.LimitedInputStream;
import co.casterlabs.commons.io.streams.NonCloseableOutputStream;
import co.casterlabs.rhs.HttpServerBuilder;
import co.casterlabs.rhs.HttpStatus;
import co.casterlabs.rhs.HttpVersion;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.RHSProtocol;
import co.casterlabs.rhs.protocol.http.HttpProtocol.HttpProtoHandler;
import co.casterlabs.rhs.protocol.http.HttpResponse.ResponseContent;
import co.casterlabs.rhs.util.DropConnectionException;
import co.casterlabs.rhs.util.HttpException;

public class HttpProtocol extends RHSProtocol<HttpSession, HttpResponse, HttpProtoHandler> {
    static final byte[] HTTP_1_1_CONTINUE_LINE = "HTTP/1.1 100 Continue\r\n\r\n".getBytes(RHSConnection.CHARSET);

    @Override
    public String name() {
        return "http";
    }

    @Override
    public HttpSession accept(RHSConnection connection) throws IOException, HttpException, DropConnectionException {
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
                if ("chunked".equalsIgnoreCase(connection.headers.getSingle("Transfer-Encoding"))) {
                    bodyInput = new ChunkedInputStream(connection);
                    connection.logger.debug("Detected chunked body.");
                    break;
                }

            case HTTP_1_0: {
                // If there's a Content-Length header then there's a body.
                String contentLength = connection.headers.getSingle("Content-Length");
                if (contentLength != null) {
                    long lengthL = Long.parseLong(contentLength);
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

    @Override
    public boolean process(HttpSession session, HttpResponse response, RHSConnection connection, HttpServerBuilder config) throws IOException, HttpException {
        boolean kaRequested = false;

        switch (connection.httpVersion) {
            case HTTP_1_1: {
                if (!connection.expectFulfilled) {
                    String expect = connection.headers.getSingle("Expect");
                    if ("100-continue".equalsIgnoreCase(expect)) {
                        // Immediately write a CONTINUE so that the client will send the body.
                        connection.output.write(HTTP_1_1_CONTINUE_LINE);
                    }
                    connection.expectFulfilled = true;
                }
                // Fall through.
            }

            case HTTP_1_0: {
                String connectionHeader = connection.headers.getSingleOrDefault("Connection", "").toLowerCase();
                if (connectionHeader.contains("keep-alive")) {
                    kaRequested = true;
                }
                break;
            }

            case HTTP_0_9:
                break;
        }

        if (kaRequested && session.hasBody()) {
            // Eat any remaining body bytes.
            InputStream bodyStream = session.getRequestBodyStream();
            while (bodyStream.available() != -1) {
                bodyStream.skip(Long.MAX_VALUE); // Skip as much as possible.
            }
        }

        long length = response.content.getLength();
        String contentEncoding = null;
        ResponseMode responseMode = null;

        if (connection.httpVersion == HttpVersion.HTTP_1_0) {
            if (length == -1) {
                responseMode = ResponseMode.CLOSE_ON_COMPLETE;
            } else {
                responseMode = ResponseMode.FIXED_LENGTH;
            }
        } else {
            contentEncoding = CompressionUtil.pickEncoding(connection, response);

            if (length == -1 || contentEncoding != null) {
                // Compressed responses should always be chunked.
                response.header("Transfer-Encoding", "chunked");
                responseMode = ResponseMode.CHUNKED;
            } else {
                responseMode = ResponseMode.FIXED_LENGTH;
                response.header("Content-Length", String.valueOf(length));
            }

            if (kaRequested) {
                // Add the keepalive headers.
                response.header("Connection", "keep-alive");
                response.header("Keep-Alive", "timeout=" + RHSConnection.HTTP_PERSISTENT_TIMEOUT);
            } else {
                // Let the client know that we will be closing the socket.
                response.header("Connection", "close");
            }

            // Write out a Date header for HTTP/1 requests with a non-100 status code.
            if ((connection.httpVersion.value >= 1) && (response.status.statusCode() >= 200)) {
                response.header("Date", RHSConnection.getHttpTime());
                response.header("Server", config.serverHeader());
            }

            if (contentEncoding != null) {
                response.header("Content-Encoding", contentEncoding);
                response.header("Vary", "Accept-Encoding");
            }
        }

        connection.respond(response.status, response.headers);

        if (!connection.method.equalsIgnoreCase("HEAD")) {
            OutputStream out = null;
            try (ResponseContent responseContent = response.content) {
                if (responseMode == ResponseMode.CHUNKED) {
                    out = new ChunkedOutputStream(connection.output);
                } else {
                    out = new NonCloseableOutputStream(connection.output);
                }

                // Write out the response, defaulting to non-encoded responses.
                CompressionUtil.writeWithEncoding(contentEncoding, out, responseContent);
            } finally {
                // Chunked output streams have special close implementations that don't actually
                // close the underlying connection, they just signal that this is the end of the
                // request.
                out.close();
            }
        }

        return kaRequested && responseMode != ResponseMode.CLOSE_ON_COMPLETE;
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

        public HttpResponse handle(HttpSession session) throws HttpException, DropConnectionException;

    }

}
