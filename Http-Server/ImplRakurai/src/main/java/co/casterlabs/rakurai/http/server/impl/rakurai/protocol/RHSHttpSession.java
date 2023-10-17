package co.casterlabs.rakurai.http.server.impl.rakurai.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.collections.HeaderMap;
import co.casterlabs.rakurai.io.IOUtil;
import co.casterlabs.rakurai.io.http.HttpVersion;
import co.casterlabs.rakurai.io.http.server.config.HttpServerBuilder;
import co.casterlabs.rakurai.io.http.server.websocket.WebsocketSession;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@RequiredArgsConstructor
public class RHSHttpSession extends WebsocketSession {
    private final @NonNull HeaderMap headers;

    private final @NonNull String uri;
    private final @NonNull String queryString;
    private final @NonNull Map<String, List<String>> allQueryParameters;
    private final @NonNull Map<String, String> queryParameters;

    private final int port;

    private final @NonNull HttpVersion version;
    private final @NonNull String method;
    private final @NonNull String remoteAddress;

    private final @Nullable InputStream bodyIn;
    private byte[] cachedBody;

    public RHSHttpSession rhsPostConstruct(HttpServerBuilder config, FastLogger parentLogger) {
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

    @Override
    public Map<String, String> parseFormBody() throws IOException {
        if (!this.hasBody()) return null;

        return null; // TODO
    }

    @Override
    public @Nullable byte[] getRequestBodyBytes() throws IOException {
        if (!this.hasBody()) return null;

        if (this.cachedBody == null) {
            this.cachedBody = IOUtil.readInputStreamBytes(this.getRequestBodyStream());
        }

        return this.cachedBody;
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
    public String getNetworkIpAddress() {
        return this.remoteAddress;
    }

}
