package co.casterlabs.rhs.protocol.api.preprocessors;

import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketSession;

public class NoOpPreprocessor {

    public static class Http implements Preprocessor.Http<Object> {
        @Override
        public void preprocess(HttpSession session, PreprocessorContext<HttpResponse, Object> context) {
            // NOOP
        }
    }

    public static class Websocket implements Preprocessor.Websocket<Object> {
        @Override
        public void preprocess(WebsocketSession session, PreprocessorContext<WebsocketResponse, Object> context) {
            // NOOP
        }
    }

}
