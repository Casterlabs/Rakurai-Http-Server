package co.casterlabs.rhs.protocol.api.endpoints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import co.casterlabs.rhs.protocol.api.preprocessors.NoOpPreprocessor;
import co.casterlabs.rhs.protocol.api.preprocessors.Preprocessor;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketSession;
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

    Class<? extends Preprocessor<WebsocketResponse, WebsocketSession>> preprocessor() default NoOpPreprocessor.Websocket.class;

}
