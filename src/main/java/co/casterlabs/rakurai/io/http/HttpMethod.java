package co.casterlabs.rakurai.io.http;

// Source: https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods
public enum HttpMethod {
    /* Data */
    GET,
    HEAD,

    /* Modifications */
    POST,
    PUT,
    DELETE,
    PATCH,

    /* Other */
    CONNECT,
    TRACE,
    OPTIONS,

    __OTHER;

    public static HttpMethod from(String string) {
        for (HttpMethod e : values()) {
            if (e.name().equals(string)) {
                return e;
            }
        }
        return __OTHER;
    }

}
