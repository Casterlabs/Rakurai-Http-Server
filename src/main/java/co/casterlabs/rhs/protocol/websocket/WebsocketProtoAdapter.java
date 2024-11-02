package co.casterlabs.rhs.protocol.websocket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.RHSConnectionWriter;
import co.casterlabs.rhs.protocol.RHSProtoAdapter;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtoAdapter.WebsocketHandler;
import co.casterlabs.rhs.util.DropConnectionException;
import co.casterlabs.rhs.util.HttpException;
import co.casterlabs.rhs.util.TaskExecutor;
import co.casterlabs.rhs.util.TaskExecutor.TaskUrgency;

public class WebsocketProtoAdapter extends RHSProtoAdapter<WebsocketSession, WebsocketListener, WebsocketHandler> {
    public static final long READ_TIMEOUT = TimeUnit.SECONDS.toMillis(15);

    private static final byte[] HTTP_1_1_CONTINUE_LINE = "HTTP/1.1 100 Continue\r\n\r\n".getBytes(RHSConnection.CHARSET);

    private static final byte[] WS_VERSION_REJECT = "HTTP/1.1 400 426 Upgrade Required\r\nSec-WebSocket-Version: 13\r\n\r\n".getBytes(RHSConnection.CHARSET);
    private static final byte[] WS_ISE = "HTTP/1.1 400 500 Internal Server Error\r\n\r\n".getBytes(RHSConnection.CHARSET);

    @Override
    public String name() {
        return "websocket";
    }

    @Override
    public WebsocketSession accept(RHSConnection connection) throws IOException, HttpException, DropConnectionException {
        int wsVersion = Integer.parseInt(connection.headers.getSingleOrDefault("Sec-WebSocket-Version", "-1"));
        switch (wsVersion) {
            // Supported.
            case 13:
                break;

            // Not supported.
            default: {
                connection.logger.warn("Rejected websocket version: %s", wsVersion);
                connection.output.write(WS_VERSION_REJECT);
                throw new DropConnectionException();
            }
        }

        String wsProtocol = connection.headers.getSingle("Sec-WebSocket-Protocol");
        if (wsProtocol != null) {
            wsProtocol = wsProtocol.split(",")[0].trim(); // First.
        }

        {
            String expect = connection.headers.getSingle("Expect");
            if ("100-continue".equalsIgnoreCase(expect)) {
                // Immediately write a CONTINUE so that the client will send the body.
                connection.output.write(HTTP_1_1_CONTINUE_LINE);
            }
            connection.expectFulfilled = true;
        }

        connection.logger.trace("Accepted websocket version: %s", wsVersion);

        return new WebsocketSession(connection, wsVersion, wsProtocol);
    }

    @Override
    public boolean process(WebsocketSession session, WebsocketListener listener, RHSConnection connection, TaskExecutor executor) throws IOException, HttpException, DropConnectionException {
        Websocket websocket = null;

        try {
            // Upgrade the connection.
            RHSConnectionWriter.writeString("HTTP/1.1 101 Switching Protocols\r\n", connection.output);
            connection.logger.trace("Response status line: HTTP/1.1 101 Switching Protocols");

            RHSConnectionWriter.writeString("Connection: Upgrade\r\n", connection.output);
            RHSConnectionWriter.writeString("Upgrade: websocket\r\n", connection.output);

            // Generate the key and send it out.
            try {
                String clientKey = connection.headers.getSingle("Sec-WebSocket-Key");

                if (clientKey != null) {
                    MessageDigest hash = MessageDigest.getInstance("SHA-1");
                    hash.reset();
                    hash.update(
                        clientKey
                            .concat("258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                            .getBytes(StandardCharsets.UTF_8)
                    );

                    String acceptKey = Base64.getEncoder().encodeToString(hash.digest());
                    RHSConnectionWriter.writeString("Sec-WebSocket-Accept: ", connection.output);
                    RHSConnectionWriter.writeString(acceptKey, connection.output);
                    RHSConnectionWriter.writeString("\r\n", connection.output);
                }
            } catch (NoSuchAlgorithmException e) {
                // Shouldn't happen.
                connection.logger.exception(e);
                connection.output.write(WS_ISE);
            }

            {
                // Select the first WS protocol, if any are requested.
                String protocol = session.protocol();
                if (protocol != null) {
                    RHSConnectionWriter.writeString("Sec-WebSocket-Protocol: ", connection.output);
                    RHSConnectionWriter.writeString(protocol, connection.output);
                    RHSConnectionWriter.writeString("\r\n", connection.output);
                }
            }

            // Write the separation line.
            RHSConnectionWriter.writeString("\r\n", connection.output);
            connection.logger.trace("WebSocket upgrade complete, ready to process frames.");

            switch (session.websocketVersion()) {
                case 13:
                    websocket = new ImplWebsocket13(session, listener, connection);
                    break;

                default:
                    // Shouldn't happen.
                    throw new DropConnectionException();
            }

            final Websocket $websocket_pointer = websocket;

            final Thread readThread = executor.execute($websocket_pointer::process, TaskUrgency.IMMEDIATE);
            final Thread pingThread = executor.execute(() -> {
                try {
                    while (true) {
                        $websocket_pointer.ping();
                        Thread.sleep(READ_TIMEOUT / 2);
                    }
                } catch (Exception ignored) {
                    readThread.interrupt();
                }
            }, TaskUrgency.IMMEDIATE);

            readThread.join();
            pingThread.interrupt(); // Cancel that task in case it's still running.
        } catch (InterruptedException ignored) {
            // NOOP
        } finally {
            websocket.close();
            listener.onClose(null);
        }
        return false;
    }

    @Override
    public WebsocketListener handle(WebsocketSession session, WebsocketHandler handler) {
        return handler.handle(session);
    }

    public static interface WebsocketHandler {

        public WebsocketListener handle(WebsocketSession session);

    }

}
