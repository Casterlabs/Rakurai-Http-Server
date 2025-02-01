package co.casterlabs.rhs.protocol.api.endpoints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lombok.NonNull;

/**
 * Tag methods with this annotation to listen for websocket connections.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WebsocketEndpoint {

    /**
     * A regex expression to match the path. Use :paramName to decode path
     * parameters.
     * 
     * @implNote :paramName is the regex-equivalent of [^/]*
     */
    @NonNull
    String path();

}
