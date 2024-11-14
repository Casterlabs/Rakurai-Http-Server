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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.HttpStatus;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.DropConnectionException;
import co.casterlabs.rhs.protocol.HttpException;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.RHSProtocol;
import co.casterlabs.rhs.protocol.http.HeaderValue;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol.WebsocketHandler;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse.AcceptedWebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse.RejectedWebsocketResponse;
import co.casterlabs.rhs.util.TaskExecutor.Task;
import co.casterlabs.rhs.util.TaskExecutor.TaskType;

public class WebsocketProtocol extends RHSProtocol<WebsocketSession, WebsocketResponse, WebsocketHandler> {
    private static final long PING_INTERVAL = TimeUnit.SECONDS.toMillis(5);

    private static final HashSet<String> ACCEPTED_VERSIONS = new HashSet<>(Arrays.asList("13"));
    private static final Map<String, String> WS_VERSION_REJECT_HEADERS = Map.of("Sec-WebSocket-Version", String.join(",", ACCEPTED_VERSIONS));

    private static final HttpStatus HTTP_1_1_UPGRADE_REJECT = HttpStatus.adapt(400, "Failed to Upgrade");
    private static final HttpStatus WS_SUBPROTOCOL_REJECT = HttpStatus.adapt(400, "Unsupported Subprotocol");

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
            .filter((s) -> ACCEPTED_VERSIONS.contains(s))
            .mapToInt(Integer::parseInt)
            .findFirst()
            .orElse(-1);

        if (wsVersion == -1) {
            connection.logger.warn("Rejected websocket versions: %s", connection.headers.getOrDefault("Sec-WebSocket-Version", Collections.emptyList()));
            connection.respond(StandardHttpStatus.UPGRADE_REQUIRED, WS_VERSION_REJECT_HEADERS);
            return null;
        }

        List<String> wsProtocols = connection.headers.getOrDefault("Sec-WebSocket-Protocol", Collections.emptyList())
            .stream()
            .map((h) -> h.delimited(","))
            .flatMap(Collection::stream)
            .map((h) -> h.raw())
            .collect(Collectors.toList());

        connection.logger.trace("Accepting websocket version: %s", wsVersion);

        return new WebsocketSession(connection, wsVersion, wsProtocols);
    }

    @Override
    public boolean process(WebsocketSession session, WebsocketResponse abstractResponse, RHSConnection connection) throws IOException, HttpException, DropConnectionException, InterruptedException {
        if (abstractResponse instanceof RejectedWebsocketResponse) {
            connection.respond(((RejectedWebsocketResponse) abstractResponse).status);
            return false;
        }

        AcceptedWebsocketResponse response = (AcceptedWebsocketResponse) abstractResponse;
        connection.logger.trace("Response status line: HTTP/1.1 101 Switching Protocols");

        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Upgrade", "websocket");
        responseHeaders.put("Connection", "Upgrade");

        if (!session.protocols().isEmpty()) {
            if (response.acceptedProtocol == null) {
                throw new HttpException(WS_SUBPROTOCOL_REJECT);
            }

            responseHeaders.put("Sec-WebSocket-Protocol", response.acceptedProtocol);
            connection.logger.debug("Using protocol %s.", response.acceptedProtocol);
        } // Otherwise, ignore.

        // Generate the key and send it out.
        try {
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
        } catch (NoSuchAlgorithmException e) {
            throw new HttpException(StandardHttpStatus.INTERNAL_ERROR);
        }

        final Websocket websocket = pick(session, response, connection);
        try (websocket) {
            // Upgrade the connection.
            connection.respond(StandardHttpStatus.SWITCHING_PROTOCOLS, responseHeaders);
            connection.logger.trace("WebSocket upgrade complete, ready to process frames.");

            final Task readTask = connection.config.taskExecutor().execute(() -> {
                try {
                    websocket.process(); // This calls onOpen().
                } catch (IOException ignored) {}
            }, TaskType.HEAVY_IO);

            final Task pingTask = connection.config.taskExecutor().execute(() -> {
                try {
                    while (true) {
                        websocket.ping();
                        Thread.sleep(PING_INTERVAL);
                    }
                } catch (Exception ignored) {
                    readTask.interrupt();
                }
            }, TaskType.MEDIUM_IO);

            readTask.waitFor();
            pingTask.interrupt(); // Cancel that task in case it's still running.

            return false;
        } finally {
            try {
                response.listener.onClose(websocket);
            } catch (Throwable t) {
                connection.logger.warn("An exception occurred whilst closing listener:\n%s", t);
            }
        }
    }

    private Websocket pick(WebsocketSession session, AcceptedWebsocketResponse response, RHSConnection connection) {
        switch (session.websocketVersion()) {
            case 13:
                return new _ImplWebsocket13(session, response, connection);

            default:
                // Shouldn't happen.
                throw new DropConnectionException();
        }
    }

    @Override
    public WebsocketResponse handle(WebsocketSession session, WebsocketHandler handler) {
        return handler.handle(session);
    }

    public static interface WebsocketHandler {

        /**
         * This is called in a {@link TaskType#LIGHT_IO} context.
         */
        public WebsocketResponse handle(WebsocketSession session);

    }

}
