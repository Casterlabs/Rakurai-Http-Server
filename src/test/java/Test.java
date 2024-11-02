import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import co.casterlabs.rhs.protocol.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.http.HttpProtocol;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.server.HttpServer;
import co.casterlabs.rhs.server.HttpServerBuilder;

public class Test {

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
//        FastLoggingFramework.setDefaultLevel(LogLevel.ALL);
        HttpServer server = new HttpServerBuilder()
            .withPort(8080)
            .with(
                new HttpProtocol(), (session) -> HttpResponse.newChunkedResponse(
                    StandardHttpStatus.OK,
                    new ByteArrayInputStream(String.format("Hello %s!", session.remoteNetworkAddress()).getBytes())
                )
                    .header("Content-Type", "text/plain")
            )
            .build();

        server.start(); // Open up http://127.0.0.1:8080
    }

}
