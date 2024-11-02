package co.casterlabs.rhs.protocol.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.io.streams.StreamUtil;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rhs.protocol.HttpMethod;
import co.casterlabs.rhs.protocol.HttpVersion;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.server.TLSVersion;
import co.casterlabs.rhs.util.CaseInsensitiveMultiMap;
import co.casterlabs.rhs.util.SimpleUri;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class HttpSession {
    protected final RHSConnection connection;
    private final @Nullable InputStream bodyIn;

    private byte[] cachedBody;

    // Request headers
    public CaseInsensitiveMultiMap headers() {
        return this.connection.headers;
    }

    // URI
    public SimpleUri uri() {
        return connection.uri;
    }

    // Request body
    public final @Nullable String getBodyMimeType() {
        return stripDirectives(this.headers().getSingle("Content-Type"));
    }

    public final Charset getBodyCharset() {
        Map<String, String> directives = parseDirectives(this.headers().getSingle("Content-Type"));

        String charset = directives.get("charset");
        if (charset == null) return StandardCharsets.UTF_8;

        return Charset.forName(charset.replace('_', '-'));
    }

    public boolean hasBody() {
        return this.bodyIn != null;
    }

    public final @Nullable String getRequestBodyString() throws IOException {
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

    /**
     * @implNote Reading from this stream will consume the request body, preventing
     *           you from using getRequestBodyBytes() and similar methods.
     */
    public @Nullable InputStream getRequestBodyStream() throws IOException {
        if (!this.connection.expectFulfilled) {
            switch (this.connection.httpVersion) {
                case HTTP_1_1: {
                    String expect = this.connection.headers.getSingle("Expect");
                    if ("100-continue".equalsIgnoreCase(expect)) {
                        // Immediately write a CONTINUE so that the client will send the body.
                        this.connection.output.write(HttpProtoAdapter.HTTP_1_1_CONTINUE_LINE);
                    }
                    break;
                }

                case HTTP_1_0:
                case HTTP_0_9:
                    break;
            }
            this.connection.expectFulfilled = true;
        }

        return this.bodyIn;
    }

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
