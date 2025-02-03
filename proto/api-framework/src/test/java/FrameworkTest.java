import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import co.casterlabs.rhs.HttpServer;
import co.casterlabs.rhs.HttpServerBuilder;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.api.ApiFramework;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointProvider;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.api.endpoints.WebsocketEndpoint;
import co.casterlabs.rhs.protocol.api.postprocessors.Postprocessor;
import co.casterlabs.rhs.protocol.api.preprocessors.Preprocessor;
import co.casterlabs.rhs.protocol.http.HttpProtocol;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import co.casterlabs.rhs.protocol.websocket.Websocket;
import co.casterlabs.rhs.protocol.websocket.WebsocketListener;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol;
import co.casterlabs.rhs.protocol.websocket.WebsocketResponse;
import co.casterlabs.rhs.protocol.websocket.WebsocketSession;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class FrameworkTest implements EndpointProvider {

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        FastLoggingFramework.setDefaultLevel(LogLevel.ALL);

        ApiFramework framework = new ApiFramework();

        framework.register(new FrameworkTest());

        HttpServer server = new HttpServerBuilder()
            .withPort(8080)
            .with(new HttpProtocol(), framework.httpHandler)
            .with(new WebsocketProtocol(), framework.websocketHandler)
            .build();

        server.start(); // Open up http://127.0.0.1:8080
    }

    @HttpEndpoint(path = ".*", priority = -1000)
    public HttpResponse onNotFound(HttpSession session, EndpointData<Void> data) {
        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NOT_FOUND, "Not found.");
    }

    @HttpEndpoint(path = "/:param", preprocessor = TestPreprocessor.class, postprocessor = TestPostprocessor.class)
    public HttpResponse onHttpTest(HttpSession session, EndpointData<Void> data) {
        String str = String.format("Hello %s! Your route param: %s", session.remoteNetworkAddress(), data.uriParameters().get("param"));
        if (session.uri().path.startsWith("/chunked")) {
            return HttpResponse.newChunkedResponse(StandardHttpStatus.OK, new ByteArrayInputStream(str.getBytes()));
        } else {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, str);
        }
    }

    @WebsocketEndpoint(path = "/:param")
    public WebsocketResponse onWebsocketTest(WebsocketSession session, EndpointData<Void> data) {
        return WebsocketResponse.accept(
            new WebsocketListener() {
                @Override
                public void onOpen(Websocket websocket) throws IOException {
                    String str = String.format("Hello %s! Your route param: %s", session.remoteNetworkAddress(), data.uriParameters().get("param"));
                    websocket.send(str);
                }

                @Override
                public void onText(Websocket websocket, String message) throws IOException {
                    websocket.send(message);
                }

                @Override
                public void onBinary(Websocket websocket, byte[] bytes) throws IOException {
                    websocket.send(bytes);
                }
            },
            session.firstProtocol()
        );
    }

    public static class TestPreprocessor implements Preprocessor.Http {
        @Override
        public void preprocess(HttpSession session, PreprocessorContext<HttpResponse> context) {
            session.logger().info("Hello! I'm a preprocessor!");
        }
    }

    public static class TestPostprocessor implements Postprocessor.Http<Void> {
        @Override
        public void postprocess(HttpSession session, HttpResponse response, EndpointData<Void> data) {
            session.logger().info("Hello! I'm a postprocessor!");
        }
    }

}
