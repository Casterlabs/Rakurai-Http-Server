package co.casterlabs.rhs.protocol.websocket;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.HttpVersion;
import co.casterlabs.rhs.TLSVersion;
import co.casterlabs.rhs.protocol.HeaderValue;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.uri.SimpleUri;
import co.casterlabs.rhs.util.CaseInsensitiveMultiMap;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class WebsocketSession {
    protected final RHSConnection connection;

    private final int websocketVersion;
    private final List<String> websocketProtocols;

    WebsocketSession(RHSConnection connection, int websocketVersion, List<String> websocketProtocols) {
        this.connection = connection;
        this.websocketVersion = websocketVersion;
        this.websocketProtocols = websocketProtocols;
    }

    public int websocketVersion() {
        return this.websocketVersion;
    }

    public List<String> acceptedProtocols() {
        return this.websocketProtocols;
    }

    public @Nullable String firstProtocol() {
        return this.websocketProtocols.isEmpty() ? null : this.websocketProtocols.get(0);
    }

    public FastLogger logger() {
        return this.connection.logger;
    }

    public CaseInsensitiveMultiMap<HeaderValue> headers() {
        return this.connection.headers;
    }

    public SimpleUri uri() {
        return this.connection.uri;
    }

    // Server info
    public int serverPort() {
        return this.connection.serverPort;
    }

    // Misc
    public HttpVersion httpVersion() {
        return this.connection.httpVersion;
    }

    /**
     * @return null, if TLS was not used.
     */
    public @Nullable TLSVersion tlsVersion() {
        return this.connection.tlsVersion;
    }

    public String remoteNetworkAddress() {
        return this.connection.remoteAddress;
    }

    public final List<String> hops() {
        return this.connection.hops();
    }

    @Override
    public final String toString() {
        return new StringBuilder()
            .append("WebsocketSession(")
            .append("\n    wsVersion=").append(this.websocketVersion())
            .append("\n    acceptedProtocols=").append(this.acceptedProtocols())
            .append("\n    httpVersion=").append(this.httpVersion())
            .append("\n    uri=").append(this.uri())
            .append("\n    headers=").append(this.headers())
            .append("\n    serverPort=").append(this.serverPort())
            .append("\n    remoteIpAddress=").append(this.remoteNetworkAddress())
            .append("\n    hops=").append(this.hops())
            .append("\n)")
            .toString();
    }

}
