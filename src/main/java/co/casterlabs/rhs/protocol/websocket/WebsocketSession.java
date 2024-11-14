package co.casterlabs.rhs.protocol.websocket;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.http.HttpSession;

public class WebsocketSession extends HttpSession {
    private final int websocketVersion;
    private final List<String> websocketProtocols;

    WebsocketSession(RHSConnection connection, int websocketVersion, List<String> websocketProtocols) {
        super(connection, null /* no body */);
        this.websocketVersion = websocketVersion;
        this.websocketProtocols = websocketProtocols;
    }

    public int websocketVersion() {
        return this.websocketVersion;
    }

    public List<String> protocols() {
        return this.websocketProtocols;
    }

    public @Nullable String firstProtocol() {
        return this.websocketProtocols.isEmpty() ? null : this.websocketProtocols.get(0);
    }

    @Deprecated
    @Override
    public HttpSessionBody body() {
        throw new UnsupportedOperationException("Websockets do not contain request bodies.");
    }

}
