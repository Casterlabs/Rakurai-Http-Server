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
public interface Preprocessor<EARLY, SESSION, ATTACHMENT> {

    public void preprocess(SESSION session, PreprocessorContext<EARLY, ATTACHMENT> context);

    @RequiredArgsConstructor
    @Accessors(fluent = true)
    public static class PreprocessorContext<EARLY, ATTACHMENT> {

        @Getter
        private final Map<String, String> uriParameters;

        @Getter
        private final String annotationPath;

        @Getter
        @Setter
        @Nullable
        private ATTACHMENT attachment;

        @Getter
        @Setter
        @Nullable
        private EARLY respondEarly;

    }

    /* ---------------- */
    /* Helpers          */
    /* ---------------- */

    public static interface Http<ATTACHMENT> extends Preprocessor<HttpResponse, HttpSession, ATTACHMENT> {
    }

    public static interface Websocket<ATTACHMENT> extends Preprocessor<WebsocketResponse, WebsocketSession, ATTACHMENT> {
    }

}
