package co.casterlabs.rhs.protocol.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.io.streams.StreamUtil;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.deserialization.JsonParser;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rhs.HttpMethod;
import co.casterlabs.rhs.HttpVersion;
import co.casterlabs.rhs.TLSVersion;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.util.CaseInsensitiveMultiMap;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HttpSession {
    protected final RHSConnection connection;
    private final @Nullable InputStream bodyIn;

    // Request headers
    public CaseInsensitiveMultiMap<HeaderValue> headers() {
        return this.connection.headers;
    }

    // URI
    public SimpleUri uri() {
        return connection.uri;
    }

    // Request body
    private final HttpSessionBody body = new HttpSessionBody();

    public HttpSessionBody body() {
        return this.body;
    }

    public class HttpSessionBody {
        private byte[] cachedBody;

        public @Nullable String mimeType() {
            if (!this.hasBody()) return null;
            return HttpSession.this.headers().getSingle("Content-Type").withoutDirectives();
        }

        public @Nullable Charset charset() {
            if (!this.hasBody()) return null;

            CaseInsensitiveMultiMap<String> directives = HttpSession.this.headers().getSingle("Content-Type").directives();
            return Charset.forName(
                directives
                    .getSingleOrDefault("charset", "UTF-8")
                    .replace('_', '-')
            );
        }

        public boolean hasBody() {
            return HttpSession.this.bodyIn != null;
        }

        public @Nullable String string() throws IOException {
            return new String(this.bytes(), this.charset());
        }

        public @NonNull JsonElement json() throws IOException, JsonParseException {
            String body = this.string();
            return JsonParser.parseString(body, Rson.DEFAULT.getConfig());
        }

        public @Nullable byte[] bytes() throws IOException {
            if (this.cachedBody == null) {
                this.cachedBody = StreamUtil.toBytes(this.stream());
            }

            return this.cachedBody;
        }

        /**
         * @implNote Reading from this stream will consume the request body, preventing
         *           you from using getRequestBodyBytes() and similar methods.
         */
        @SuppressWarnings("deprecation")
        public @Nullable InputStream stream() throws IOException {
            if (!this.hasBody()) {
                throw new IllegalStateException("Request body is not present. Call hasBody() first.");
            }

            HttpSession.this.connection.satisfyExpectations();
            return HttpSession.this.bodyIn;
        }

        public Query urlEncoded() throws IOException {
            String mime = this.mimeType();
            if (!mime.equals("application/x-www-form-urlencoded")) {
                throw new IllegalStateException("Unsupported form body type: " + mime);
            }

            return Query.from(this.string());
        }

    }

    // Server info
    public int serverPort() {
        return this.connection.serverPort;
    }

    // Misc
    public final HttpMethod method() {
        return HttpMethod.from(this.rawMethod());
    }

    public String rawMethod() {
        return this.connection.method;
    }

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
            .append("HttpSession(")
            .append("\n    method=").append(this.rawMethod())
            .append("\n    version=").append(this.httpVersion())
            .append("\n    uri=").append(this.uri())
            .append("\n    headers=").append(this.headers())
            .append("\n    serverPort=").append(this.serverPort())
            .append("\n    remoteIpAddress=").append(this.remoteNetworkAddress())
            .append("\n    hops=").append(this.hops())
            .append("\n)")
            .toString();
    }

}
