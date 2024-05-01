package co.casterlabs.rhs.session;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.io.streams.StreamUtil;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rhs.protocol.HttpMethod;
import co.casterlabs.rhs.protocol.HttpVersion;
import co.casterlabs.rhs.server.HttpServerBuilder;
import co.casterlabs.rhs.util.HeaderMap;
import lombok.Getter;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public abstract class HttpSession {
    private final @Getter String requestId = UUID.randomUUID().toString();

    boolean hasSessionErrored = false;

    private @Getter FastLogger logger;

    private @Getter boolean isProxied;
    private String remoteIp;

    private byte[] cachedBody;

    protected void postConstruct(HttpServerBuilder config, FastLogger parentLogger) {
        this.isProxied = config.isBehindProxy();

        FastLogger realLogger = parentLogger.createChild("Session: " + this.requestId);

        this.remoteIp = this.getRequestHops().get(0);

        this.logger = new FastLogger(this.requestId) {
            @Override
            public FastLogger log(@NonNull LogLevel level, @Nullable Object object, @Nullable Object... args) {
                if (level.canLog(LogLevel.WARNING)) {
                    hasSessionErrored = true;
                }

                realLogger.log(level, object, args);

                return this;
            }
        };
    }

    // Request headers
    public abstract HeaderMap getHeaders();

    public final @Nullable String getHeader(@NonNull String header) {
        return this.getHeaders().getSingle(header);
    }

    // URI
    public abstract String getUri();

    public abstract Map<String, List<String>> getAllQueryParameters();

    public abstract Map<String, String> getQueryParameters();

    public abstract String getQueryString();

    // Request body
    public final @Nullable String getBodyMimeType() {
        return stripDirectives(this.getHeader("Content-Type"));
    }

    public final Charset getBodyCharset() {
        Map<String, String> directives = parseDirectives(this.getHeader("Content-Type"));

        String charset = directives.get("charset");
        if (charset == null) return StandardCharsets.UTF_8;

        return Charset.forName(charset.replace('_', '-'));
    }

    public abstract boolean hasBody();

    public final @Nullable String getRequestBody() throws IOException {
        if (this.hasBody()) {
            return new String(this.getRequestBodyBytes(), this.getBodyCharset());
        } else {
            return null;
        }
    }

    public final @NonNull JsonElement getRequestBodyJson(@Nullable Rson rson) throws IOException, JsonParseException {
        if (this.hasBody()) {
            if (rson == null) {
                rson = Rson.DEFAULT;
            }

            if ("application/json".equals(this.getBodyMimeType())) {
                String body = new String(this.getRequestBodyBytes(), this.getBodyCharset());

                switch (body.charAt(0)) {
                    case '{': {
                        return rson.fromJson(body, JsonObject.class);
                    }

                    case '[': {
                        return rson.fromJson(body, JsonArray.class);
                    }

                    default: {
                        throw new JsonParseException("Request body must be either a JsonObject or JsonArray.");
                    }
                }
            } else {
                throw new JsonParseException("Request body must have a Content-Type of application/json.");
            }
        } else {
            return null;
        }
    }

    public @Nullable byte[] getRequestBodyBytes() throws IOException {
        if (!this.hasBody()) return null;

        if (this.cachedBody == null) {
            this.cachedBody = StreamUtil.toBytes(this.getRequestBodyStream());
        }

        return this.cachedBody;
    }

    public abstract @Nullable InputStream getRequestBodyStream() throws IOException;

//    /**
//     * @return either a form body (multipart) or a map (url encoded).
//     */
//    public Either<MultipartForm, URLEncodedForm> parseFormBody() throws IOException {
//        String mime = this.getBodyMimeType();
//
//        if (mime.equals("application/x-www-form-urlencoded")) {
//            return URLEncodedForm.parse(this);
//        } else if (mime.startsWith("multipart/form-data;boundary=")) {
//            throw new IOException("Multipart form data is unsupported at this time.");
//        } else {
//            throw new IOException("Unsupported form body type: " + mime);
//        }
//    }

    // Server info
    /**
     * @return the value from the HOST header.
     */
    public final @Nullable String getHost() {
        double version = this.getVersion().value;

        if (version < 1) {
            return null;
        } else if (version >= 2) {
            return this.getHeader(":authority");
        } else {
            return this.getHeader("Host");
        }
    }

    public abstract int getPort();

    // Misc
    public final HttpMethod getMethod() {
        return HttpMethod.from(this.getRawMethod());
    }

    public abstract String getRawMethod();

    public abstract HttpVersion getVersion();

    /**
     * @return null, if TLS was not used.
     */
    public abstract @Nullable TLSVersion getTLSVersion();

    public final String getRemoteIpAddress() {
        return this.remoteIp;
    }

    protected abstract String getNetworkIpAddress();

    public final List<String> getRequestHops() {
        List<String> hops = new ArrayList<>();

        // If we are expecting the request to be proxied and the X-Forwarded-For header
        // is present then we parse it.
        if (this.isProxied) {
            // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For
            List<String> forwardedForHeader = this.getHeaders().get("X-Forwarded-For");

            if (forwardedForHeader != null) {
                for (String list : forwardedForHeader) {
                    for (String hop : list.split(",")) {
                        hops.add(hop.trim());
                    }
                }
            }
        }

        // Add the final hop. If we aren't proxied then this has the effect of adding
        // the actual IP address to the list.
        String finalHop = this.getNetworkIpAddress();

        if (!hops.contains(finalHop)) {
            hops.add(finalHop);
        }

        return hops;
    }

    @Override
    public final String toString() {
        return new StringBuilder()
            .append("HttpSession(")
            .append("\n    method=").append(this.getMethod())
            .append("\n    version=").append(this.getVersion())
            .append("\n    uri=").append(this.getUri()).append(this.getQueryString())
            .append("\n    queryParameters=").append(this.getAllQueryParameters())
            .append("\n    headers=").append(this.getHeaders())
            .append("\n    host=").append(this.getHost())
            .append("\n    port=").append(this.getPort())
            .append("\n    remoteIpAddress=").append(this.getRemoteIpAddress())
            .append("\n    hops=").append(this.getRequestHops())
            .append("\n)")
            .toString();
    }

    public static @Nullable String stripDirectives(@Nullable String str) {
        if (str == null) return null;
        if (str.indexOf(';') == -1) return str;

        return str.substring(0, str.indexOf(';'));
    }

    public static Map<String, String> parseDirectives(@Nullable String str) {
        if (str == null) return Collections.emptyMap();
        if (str.indexOf(';') == -1) return Collections.emptyMap();

        str = str.substring(str.indexOf(';') + 1).trim();

        Map<String, String> directives = new HashMap<>();
        for (String directive : str.split(" ")) {
            String[] split = directive.split("=");
            if (split.length == 1) {
                directives.put(split[0], "");
            } else {
                directives.put(split[0], split[1]);
            }
        }
        return directives;
    }

}
