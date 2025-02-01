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
import co.casterlabs.rhs.protocol.api.postprocessors.Postprocessor;
import co.casterlabs.rhs.protocol.http.HttpProtocol;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class FrameworkCorsExample implements EndpointProvider {

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        FastLoggingFramework.setDefaultLevel(LogLevel.ALL);

        ApiFramework framework = new ApiFramework();

        framework.register(new FrameworkCorsExample());

        HttpServer server = new HttpServerBuilder()
            .withPort(8080)
            .with(new HttpProtocol(), framework.httpHandler)
            .with(new WebsocketProtocol(), framework.websocketHandler)
            .build();

        server.start(); // Open up http://127.0.0.1:8080
    }

    @HttpEndpoint(path = ".*", postprocessor = CorsPostprocessor.class)
    public HttpResponse onHttpTest(HttpSession session, EndpointData<Void> data) {
        String str = String.format("Hello %s!", session.remoteNetworkAddress());
        if (session.uri().path.startsWith("/chunked")) {
            return HttpResponse.newChunkedResponse(StandardHttpStatus.OK, new ByteArrayInputStream(str.getBytes()));
        } else {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, str);
        }
    }

    public static class CorsPostprocessor implements Postprocessor.Http<Void> {
        @Override
        public void postprocess(HttpSession session, HttpResponse response, EndpointData<Void> data) {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, OPTIONS");
        }
    }

}
