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
public interface Preprocessor<E, S, A> {

    public void preprocess(S session, PreprocessorContext<E, A> context);

    @RequiredArgsConstructor
    @Accessors(fluent = true)
    public static class PreprocessorContext<E, A> {

        @Getter
        private final Map<String, String> uriParameters;

        @Getter
        @Setter
        @Nullable
        private A attachment;

        @Getter
        @Setter
        @Nullable
        private E respondEarly;

    }

    /* ---------------- */
    /* Helpers          */
    /* ---------------- */

    public static interface Http<A> extends Preprocessor<HttpResponse, HttpSession, A> {
    }

    public static interface Websocket<A> extends Preprocessor<WebsocketResponse, WebsocketSession, A> {
    }

}
