package co.casterlabs.rhs.protocol.api.endpoints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.HttpMethod;
import co.casterlabs.rhs.protocol.api.postprocessors.NoOpPostprocessor;
import co.casterlabs.rhs.protocol.api.postprocessors.Postprocessor;
import co.casterlabs.rhs.protocol.api.preprocessors.NoOpPreprocessor;
import co.casterlabs.rhs.protocol.api.preprocessors.Preprocessor;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
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
     * @apiNote  Valid :paramName labels must only contain alpha-numeric or
     *           underscore or hyphen. e.g :my-param or :my_param2 or :myparam
     * 
     * @implNote :paramName is the regex-equivalent of [^/]*
     */
    @NonNull
    String path();

    Class<? extends Preprocessor<HttpResponse, HttpSession>> preprocessor() default NoOpPreprocessor.Http.class;

    Class<? extends Postprocessor<HttpResponse, HttpSession, ?>> postprocessor() default NoOpPostprocessor.Http.class;

}
