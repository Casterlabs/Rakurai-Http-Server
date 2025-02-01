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
import co.casterlabs.rhs.protocol.api.preprocessors.Preprocessor;
import co.casterlabs.rhs.protocol.http.HttpProtocol;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol;
import lombok.AllArgsConstructor;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class FrameworkAuthExample implements EndpointProvider {

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        FastLoggingFramework.setDefaultLevel(LogLevel.ALL);

        ApiFramework framework = new ApiFramework();

        framework.register(new FrameworkAuthExample());

        HttpServer server = new HttpServerBuilder()
            .withPort(8080)
            .with(new HttpProtocol(), framework.httpHandler)
            .with(new WebsocketProtocol(), framework.websocketHandler)
            .build();

        server.start(); // Open up http://127.0.0.1:8080
    }

    @HttpEndpoint(path = ".*", preprocessor = AuthPreprocessor.class)
    public HttpResponse onHttpTest(HttpSession session, EndpointData<AuthorizedUser> data) {
        String str = String.format("Hello %s!", data.attachment().username);
        if (session.uri().path.startsWith("/chunked")) {
            return HttpResponse.newChunkedResponse(StandardHttpStatus.OK, new ByteArrayInputStream(str.getBytes()));
        } else {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, str);
        }
    }

    public static class AuthPreprocessor implements Preprocessor.Http {
        @Override
        public void preprocess(HttpSession session, PreprocessorContext<HttpResponse> context) {
            // Check if the user is localhost
            // Normal values: "127.0.0.1:12345", "[0:0:0:0:0:0:0:1]:12345"

            // You could do anything you want here. Check Http Basic Auth, JWT, lookup a
            // client id parameter in the database, etc.

            if (session.remoteNetworkAddress().startsWith("127.0.0.1") ||
                session.remoteNetworkAddress().startsWith("[0:0:0:0:0:0:0:1]")) {
                context.attachment(new AuthorizedUser("Localhost"));
                return;
            }

            context.respondEarly(
                HttpResponse.newFixedLengthResponse(
                    StandardHttpStatus.UNAUTHORIZED,
                    "Unauthorized. You are not localhost."
                )
            );
        }
    }

    @AllArgsConstructor
    public static class AuthorizedUser {
        public String username;
    }

}
