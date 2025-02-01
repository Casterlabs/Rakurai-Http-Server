package co.casterlabs.rhs.protocol.api.preprocessors;

import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketSession;

public class NoOpPreprocessor {

    public static class Http implements Preprocessor.Http {
        @Override
        public void preprocess(HttpSession session, PreprocessorContext<HttpResponse> context) {
            // NOOP
        }
    }

    public static class Websocket implements Preprocessor.Websocket {
        @Override
        public void preprocess(WebsocketSession session, PreprocessorContext<WebsocketResponse> context) {
            // NOOP
        }
    }

}
