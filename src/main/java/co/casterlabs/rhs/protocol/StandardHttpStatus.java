package co.casterlabs.rhs.protocol;

import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Nullable;

import lombok.Getter;
import lombok.NonNull;

/**
 * Sources: <br>
 * <a href=
 * "https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">Wikipedia</a> <br>
 * <a href=
 * "https://developer.mozilla.org/en-US/docs/Web/HTTP/Status">Mozilla</a>
 */
@Getter
@NonNull
public enum StandardHttpStatus implements HttpStatus {
    // @formatter:off
    
                                   /* -------- Informational -------- */
//                                       CONTINUE(100, "Continue"),
//                            SWITCHING_PROTOCOLS(101, "Switching Protocols"),
                                     PROCESSING(102, "Processing"), // (WebDAV)

                                   /* -------- Success -------- */
                                             OK(200, "OK"),
                                        CREATED(201, "Created"),
                                       ACCEPTED(202, "Accepted"),
                  NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
                                     NO_CONTENT(204, "No Content"),
                                  RESET_CONTENT(205, "Reset Content"),
                                PARTIAL_CONTENT(206, "Partial Content"),
                                   THIS_IS_FINE(218, "This Is Fine"), // (Rakurai/Apache Web Server)
                     INSTANCE_MANIPULATION_USED(226, "Instance Manipulation Used"),

                                   /* -------- Redirection -------- */
                               MULTIPLE_CHOICES(300, "Multiple Choices"),
                              MOVED_PERMANENTLY(301, "Moved Permanently"),
    /**
     * @deprecated Even if the specification requires the method (and the body) not
     *             to be altered when the redirection is performed, not all
     *             user-agents conform here - you can still find this type of bugged
     *             software out there. It is therefore recommended to set the
     *             {@link FOUND} code only as a response for GET or HEAD methods and
     *             to use {@link TEMPORARY_REDIRECT} instead, as the method change
     *             is explicitly prohibited in that case.
     */
                              @Deprecated FOUND(302, "Found"),
                                      SEE_OTHER(303, "See Other"),
                                   NOT_MODIFIED(304, "Not Modified"),
    /**
     * Defined in a previous version of the HTTP specification to indicate that a
     * requested response must be accessed by a proxy. It has been deprecated due to
     * security concerns regarding in-band configuration of a proxy.
     */
                          @Deprecated USE_PROXY(305, "Use Proxy"),
//                                         UNUSED(306, "Unused");
                             TEMPORARY_REDIRECT(307, "Temporary Redirect"),
                             PERMANENT_REDIRECT(308, "Permanent Redirect"),

                                   /* -------- Client Error -------- */
                                    BAD_REQUEST(400, "Bad Request"),
                                   UNAUTHORIZED(401, "Unauthorized"),
                 @Experimental PAYMENT_REQUIRED(402, "Payment Required"),
                                      FORBIDDEN(403, "Forbidden"),
                                      NOT_FOUND(404, "Not Found"),
                             METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
                                 NOT_ACCEPTABLE(406, "Not Acceptable"),
                   PROXY_AUTHENTICAION_REQUIRED(407, "Proxy Authentication Required"),
                                REQUEST_TIMEOUT(408, "Request Timeout"),
                                       CONFLICT(409, "Conflict"),
                                           GONE(410, "Gone"),
                                LENGTH_REQUIRED(411, "Length Required"),
                            PRECONDITION_FAILED(412, "Precondition Failed"),
                              PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
                           REQUEST_URI_TOO_LONG(414, "Request URI Too Long"),
                         UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
                          RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
                             EXPECTATION_FAILED(417, "Expectation Failed"),
                                    IM_A_TEAPOT(418, "I'm A Teapot"), // :D https://en.wikipedia.org/wiki/HTTP_418
                              ENHANCE_YOUR_CALM(420, "Enhance Your Calm"), // (Twitter)
                           UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"), // (WebDAV)
                                         LOCKED(423, "Locked"), // (WebDAV)
                              FAILED_DEPENDENCY(424, "Failed Dependency"), // (WebDAV)
                               UPGRADE_REQUIRED(426, "Upgrade Required"),
                          PRECONDITION_REQUIRED(428, "Precondition Required"),
                              TOO_MANY_REQUESTS(429, "Too Many Requests"),
                                  LOGIN_TIMEOUT(440, "Login Timeout"), // (Microsoft IIS)
                                    NO_RESPONSE(444, "No Response"), // (Rakurai/Nginx)
                                     RETRY_WITH(449, "Retry With"), // (Microsoft IIS)
                 UNAVAILABLE_FOR_LEAGAL_REASONS(451, "Unavailable For Legal Reasons"),
                       REQUEST_HEADER_TOO_LARGE(494, "Request Header Too Large"), // (Nginx)
                HTTP_REQUEST_SENT_TO_HTTPS_PORT(497, "HTTP Request Sent To HTTPS Port"), // (Nginx)
                                  INVALID_TOKEN(498, "Invalid Token"), // (Esri)

                                   /* -------- Server Error -------- */
                                 INTERNAL_ERROR(500, "Internal Server Error"),
                                NOT_IMPLEMENTED(501, "Not Implemented"),
                                    BAD_GATEWAY(502, "Bad Gateway"),
                            SERVICE_UNAVAILABLE(503, "Service Unavailable"),
                                GATEWAY_TIMEOUT(504, "Gateway Timeout"),
                       UNSUPPORTED_HTTP_VERSION(505, "HTTP Version Not Supported"),
                        VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),
                           INSUFFICIENT_STORAGE(507, "Insufficient Storage"), // (WebDAV)
                                  LOOP_DETECTED(508, "Loop Detected"), // (WebDAV)
                       BANDWIDTH_LIMIT_EXCEEDED(509, "Badnwidth Limit Exceeded"), // (Apache Web Server / cPanel)
                                   NOT_EXTENDED(510, "Not Extended"),
                NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required"),
                     NETWORK_READ_TIMEOUT_ERROR(598, "Network Read Timeout Error"), // Some proxies use it.
                  NETWORK_CONNECT_TIMEOUT_ERROR(599, "Network Connect Timeout Error"), // Some proxies use it.

    // @formatter:on
    ;

    // TODO make sure this is always tied to the max status code value.
    private static final StandardHttpStatus[] STATUS_BY_CODE = new StandardHttpStatus[600];

    static {
        for (StandardHttpStatus status : StandardHttpStatus.values()) {
            STATUS_BY_CODE[status.statusCode] = status;
        }
    }

    private String description;
    private int statusCode;

    private StandardHttpStatus(int statusCode, String description) {
        this.description = description;
        this.statusCode = statusCode;
    }

    /**
     * Does a status lookup by code.
     *
     * @param  code the code
     * 
     * @return      the http status
     */
    public static @Nullable StandardHttpStatus lookup(int code) {
        try {
            return STATUS_BY_CODE[code];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return this.getStatusString();
    }

}
