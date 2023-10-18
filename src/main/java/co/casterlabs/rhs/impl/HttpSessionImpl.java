package co.casterlabs.rhs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.protocol.HttpVersion;
import co.casterlabs.rhs.server.HttpServerBuilder;
import co.casterlabs.rhs.session.TLSVersion;
import co.casterlabs.rhs.session.WebsocketSession;
import co.casterlabs.rhs.util.HeaderMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@RequiredArgsConstructor
class HttpSessionImpl extends WebsocketSession {
    private final @NonNull HeaderMap headers;

    private final @NonNull String uri;
    private final @NonNull String queryString;
    private final @NonNull Map<String, List<String>> allQueryParameters;
    private final @NonNull Map<String, String> queryParameters;

    private final int port;

    private final @Nullable TLSVersion tlsVersion;
    private final @NonNull HttpVersion version;
    private final @NonNull String method;
    private final @NonNull String remoteAddress;

    private final @Nullable InputStream bodyIn;

    public HttpSessionImpl rhsPostConstruct(HttpServerBuilder config, FastLogger parentLogger) {
        super.postConstruct(config, parentLogger);
        return this;
    }

    // Request headers
    @Override
    public HeaderMap getHeaders() {
        return this.headers;
    }

    // URI
    @Override
    public String getUri() {
        return this.uri;
    }

    @Override
    public String getQueryString() {
        return this.queryString;
    }

    @Override
    public Map<String, String> getQueryParameters() {
        return this.queryParameters;
    }

    @Override
    public Map<String, List<String>> getAllQueryParameters() {
        return this.allQueryParameters;
    }

    // Request body
    @Override
    public boolean hasBody() {
        return this.bodyIn != null;
    }

    @Override
    public @Nullable InputStream getRequestBodyStream() throws IOException {
        return this.bodyIn;
    }

    // Server info
    @Override
    public int getPort() {
        return this.port;
    }

    // Misc
    @Override
    public String getRawMethod() {
        return this.method;
    }

    @Override
    public HttpVersion getVersion() {
        return this.version;
    }

    @Override
    public @Nullable TLSVersion getTLSVersion() {
        return this.tlsVersion;
    }

    @Override
    public String getNetworkIpAddress() {
        return this.remoteAddress;
    }

}
