package co.casterlabs.rhs.protocol.websocket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.HttpStatus;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.DropConnectionException;
import co.casterlabs.rhs.protocol.HttpException;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.RHSProtocol;
import co.casterlabs.rhs.protocol.http.HeaderValue;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol.WebsocketHandler;
import co.casterlabs.rhs.util.TaskExecutor.Task;
import co.casterlabs.rhs.util.TaskExecutor.TaskType;

public class WebsocketProtocol extends RHSProtocol<WebsocketSession, WebsocketListener, WebsocketHandler> {
    private static final long PING_INTERVAL = TimeUnit.SECONDS.toMillis(5);

    private static final HashSet<String> ACCEPTED_VERSIONS = new HashSet<>(Arrays.asList("13"));

    private static final HttpStatus HTTP_1_1_UPGRADE_REJECT = HttpStatus.adapt(400, "Failed to Upgrade");
    private static final Map<String, String> WS_VERSION_REJECT_HEADERS = Map.of("Sec-WebSocket-Version", String.join(",", ACCEPTED_VERSIONS));

    @Override
    public String name() {
        return "websocket";
    }

    @Override
    public @Nullable WebsocketSession accept(RHSConnection connection) throws IOException, HttpException, DropConnectionException {
        if (!connection.method.equalsIgnoreCase("GET")) {
            connection.logger.trace("Rejecting websocket upgrade, method was %s.", connection.method);
            connection.respond(HTTP_1_1_UPGRADE_REJECT);
            return null;
        }

        int wsVersion = connection.headers.getOrDefault("Sec-WebSocket-Version", Collections.emptyList())
            .stream()
            .map((h) -> h.delimited(","))
            .flatMap(Collection::stream)
            .map((h) -> h.raw())
            .map(String::trim)
            .filter((s) -> ACCEPTED_VERSIONS.contains(s))
            .mapToInt(Integer::parseInt)
            .findFirst()
            .orElse(-1);

        if (wsVersion == -1) {
            connection.logger.warn("Rejected websocket versions: %s", connection.headers.getOrDefault("Sec-WebSocket-Version", Collections.emptyList()));
            connection.respond(StandardHttpStatus.UPGRADE_REQUIRED, WS_VERSION_REJECT_HEADERS);
            return null;
        }

        String wsProtocol = connection.headers.containsKey("Sec-WebSocket-Protocol") ? connection.headers
            .getSingle("Sec-WebSocket-Protocol")
            .delimited(",")
            .get(0) // First value
            .raw() : null;

        connection.logger.trace("Accepted websocket version: %s", wsVersion);

        return new WebsocketSession(connection, wsVersion, wsProtocol);
    }

    @Override
    public boolean process(WebsocketSession session, WebsocketListener listener, RHSConnection connection) throws IOException, HttpException, DropConnectionException {
        Websocket websocket = null;

        try {
            connection.logger.trace("Response status line: HTTP/1.1 101 Switching Protocols");

            Map<String, String> responseHeaders = new HashMap<>();
            responseHeaders.put("Upgrade", "websocket");
            responseHeaders.put("Connection", "Upgrade");

            // Generate the key and send it out.
            HeaderValue clientKey = connection.headers.getSingle("Sec-WebSocket-Key");
            if (clientKey != null) {
                MessageDigest hash = MessageDigest.getInstance("SHA-1");
                hash.reset();
                hash.update(
                    clientKey
                        .raw()
                        .concat("258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                        .getBytes(StandardCharsets.UTF_8)
                );

                String acceptKey = Base64.getEncoder().encodeToString(hash.digest());
                responseHeaders.put("Sec-WebSocket-Accept", acceptKey);
            }

            // Select the first WS protocol, if any are requested.
            String wsProtocol = session.protocol();
            if (wsProtocol != null) {
                responseHeaders.put("Sec-WebSocket-Protocol", wsProtocol);
            }

            // Upgrade the connection.
            connection.respond(StandardHttpStatus.SWITCHING_PROTOCOLS, responseHeaders);
            connection.logger.trace("WebSocket upgrade complete, ready to process frames.");

            switch (session.websocketVersion()) {
                case 13:
                    websocket = new _ImplWebsocket13(session, listener, connection);
                    break;

                default:
                    // Shouldn't happen.
                    throw new DropConnectionException();
            }

            final Websocket $websocket_pointer = websocket;

            final Task readTask = connection.config.taskExecutor().execute($websocket_pointer::process, TaskType.HEAVY_IO); // This calls onOpen().
            final Task pingTask = connection.config.taskExecutor().execute(() -> {
                try {
                    while (true) {
                        $websocket_pointer.ping();
                        Thread.sleep(PING_INTERVAL);
                    }
                } catch (Exception ignored) {
                    readTask.interrupt();
                }
            }, TaskType.MEDIUM_IO);

            readTask.waitFor();
            pingTask.interrupt(); // Cancel that task in case it's still running.
        } catch (NoSuchAlgorithmException e) {
            // Shouldn't happen.
            connection.logger.exception(e);
            connection.respond(StandardHttpStatus.INTERNAL_ERROR);
        } catch (InterruptedException ignored) {
            // NOOP
        } finally {
            if (websocket != null) {
                websocket.close();
            }
            try {
                listener.onClose(websocket);
            } catch (Throwable t) {
                connection.logger.warn("An exception occurred whilst closing listener:\n%s", t);
            }
        }
        return false;
    }

    @Override
    public WebsocketListener handle(WebsocketSession session, WebsocketHandler handler) {
        return handler.handle(session);
    }

    public static interface WebsocketHandler {

        /**
         * This is called in a {@link TaskType#LIGHT_IO} context.
         */
        public WebsocketListener handle(WebsocketSession session);

    }

}
