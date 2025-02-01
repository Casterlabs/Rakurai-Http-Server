package co.casterlabs.rhs.protocol.api.endpoints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.HttpMethod;
import lombok.NonNull;

/**
 * Tag methods with this annotation to listen for http requests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HttpEndpoint {

    /**
     * Allowed methods.
     *
     * @return a list of allowed methods
     */
    @Nullable
    HttpMethod[] allowedMethods() default {
            HttpMethod.GET,
            HttpMethod.HEAD,
            HttpMethod.QUERY,
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.DELETE,
            HttpMethod.PATCH
    };

    /**
     * A regex expression to match the path. Use :paramName to decode path
     * parameters.
     * 
     * @implNote :paramName is the regex-equivalent of [^/]*
     */
    @NonNull
    String path();

}
