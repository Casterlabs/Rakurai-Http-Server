package co.casterlabs.rhs.protocol.api.preprocessors;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * This runs before your request handler method is invoked. You can use this to
 * check parameters, authenticate, etc.
 */
public interface Preprocessor<E, S> {

    public void preprocess(S session, PreprocessorContext<E> context);

    @RequiredArgsConstructor
    @Accessors(fluent = true)
    public static class PreprocessorContext<E> {

        @Getter
        private final Map<String, String> uriParameters;

        @Getter
        @Setter
        @Nullable
        private Object attachment;

        @Getter
        @Setter
        @Nullable
        private E respondEarly;

    }

    /* ---------------- */
    /* Helpers          */
    /* ---------------- */

    public static interface Http extends Preprocessor<HttpResponse, HttpSession> {
    }

    public static interface Websocket extends Preprocessor<WebsocketResponse, WebsocketSession> {
    }

}
