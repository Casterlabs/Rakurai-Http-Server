package co.casterlabs.rhs.protocol.websocket;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.http.HttpSession;

public class WebsocketSession extends HttpSession {
    private final int websocketVersion;
    private final @Nullable String websocketProtocol;

    WebsocketSession(RHSConnection connection, int websocketVersion, @Nullable String websocketProtocol) {
        super(connection, null /* no body */);
        this.websocketVersion = websocketVersion;
        this.websocketProtocol = websocketProtocol;
    }

    public int websocketVersion() {
        return this.websocketVersion;
    }

    public @Nullable String protocol() {
        return this.websocketProtocol;
    }

    @Deprecated
    @Override
    public HttpSessionBody body() {
        throw new UnsupportedOperationException("Websockets do not contain request bodies.");
    }

}
