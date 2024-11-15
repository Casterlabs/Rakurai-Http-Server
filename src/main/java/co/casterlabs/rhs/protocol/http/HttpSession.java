package co.casterlabs.rhs.protocol.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.io.streams.StreamUtil;
import co.casterlabs.rhs.HttpMethod;
import co.casterlabs.rhs.HttpVersion;
import co.casterlabs.rhs.TLSVersion;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.util.CaseInsensitiveMultiMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HttpSession {
    protected final RHSConnection connection;
    private final @Nullable InputStream bodyIn;

    public FastLogger logger() {
        return this.connection.logger;
    }

    public CaseInsensitiveMultiMap<HeaderValue> headers() {
        return this.connection.headers;
    }

    public SimpleUri uri() {
        return this.connection.uri;
    }

    // Request body
    private final HttpSessionBody body = new HttpSessionBody();

    public HttpSessionBody body() {
        return this.body;
    }

    public class HttpSessionBody {
        private byte[] cachedBody;

        public @Nullable String mime() {
            if (!this.present()) return null;
            if (!HttpSession.this.headers().containsKey("Content-Type")) return "application/octet-stream";
            return HttpSession.this.headers().getSingle("Content-Type").raw();
        }

        public @Nullable Charset charset() {
            if (!this.present()) return null;

            CaseInsensitiveMultiMap<String> directives = HttpSession.this.headers().getSingle("Content-Type").directives();
            return Charset.forName(
                directives
                    .getSingleOrDefault("charset", "UTF-8")
                    .replace('_', '-')
            );
        }

        public boolean present() {
            return HttpSession.this.bodyIn != null;
        }

        /**
         * @return -1 if the body is chunked or not present.
         */
        public long length() {
            if (!this.present()) return -1;
            if (!HttpSession.this.headers().containsKey("Content-Length")) return -1;
            return Long.parseLong(HttpSession.this.headers().getSingle("Content-Length").raw());
        }

        public @Nullable String string() throws IOException {
            return new String(this.bytes(), this.charset());
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
            if (!this.present()) {
                throw new IllegalStateException("Request body is not present. Call hasBody() first.");
            }

            HttpSession.this.connection.satisfyExpectations();
            return HttpSession.this.bodyIn;
        }

        public Query urlEncoded() throws IOException {
            String mime = this.mime();
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
