package co.casterlabs.rhs.protocol.websocket;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.HttpStatus;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public abstract class WebsocketResponse {

    /* ---------------- */
    /* Rejected         */
    /* ---------------- */

    public static WebsocketResponse reject(@NonNull HttpStatus status) {
        return new RejectedWebsocketResponse(status);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class RejectedWebsocketResponse extends WebsocketResponse {
        final HttpStatus status;
    }

    /* ---------------- */
    /* Accepted         */
    /* ---------------- */

    /**
     * @param    acceptedProtocol can be null if you do not wish to specify a
     *                            protocol. Note that some WebSocket clients might
     *                            expect you to pick one. See
     *                            {@link WebsocketSession#protocols()}.
     * 
     * @implNote                  Defaults to a max payload length of 16mb
     */
    public static WebsocketResponse accept(@NonNull WebsocketListener listener, @Nullable String acceptedProtocol) {
        final int DEFAULT_MAX = 16 /*mb*/ * 1024 * 1024;
        return accept(listener, acceptedProtocol, DEFAULT_MAX);
    }

    /**
     * @param maxPayloadLength must be greater than 64kb (65535 bytes) and must be
     *                         smaller than Integer.MAX_VALUE
     * @param acceptedProtocol can be null if you do not wish to specify a protocol.
     *                         Note that some WebSocket clients might expect you to
     *                         pick one. See {@link WebsocketSession#protocols()}.
     */
    public static WebsocketResponse accept(@NonNull WebsocketListener listener, @Nullable String acceptedProtocol, int maxPayloadLength) {
        assert maxPayloadLength > 65535 : "Max payload length must be larger than 64kb (65535 bytes).";
        assert maxPayloadLength < Integer.MAX_VALUE : "Max payload length must be smaller than Integer.MAX_VALUE.";
        return new AcceptedWebsocketResponse(listener, acceptedProtocol, maxPayloadLength);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class AcceptedWebsocketResponse extends WebsocketResponse {
        final WebsocketListener listener;
        final @Nullable String acceptedProtocol;
        final int maxPayloadLength;
    }

}
