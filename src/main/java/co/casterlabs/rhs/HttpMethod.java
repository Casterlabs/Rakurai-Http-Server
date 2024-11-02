package co.casterlabs.rhs;

// Source: https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods
public enum HttpMethod {
    /* Data */
    GET,
    HEAD,
    QUERY, // Draft.

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
