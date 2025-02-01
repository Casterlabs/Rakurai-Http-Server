import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import co.casterlabs.rhs.HttpServer;
import co.casterlabs.rhs.HttpServerBuilder;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.http.HttpProtocol;
import co.casterlabs.rhs.protocol.http.HttpResponse;

public class TestRPS {
    private static final byte[] helloWorld = "Hello World!".getBytes();

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
//        FastLoggingFramework.setDefaultLevel(LogLevel.ALL);

        // npx autocannon --warmup [ -d 10 ] --latency -d 10 -c 6 http://localhost:8080

        HttpServer server = new HttpServerBuilder()
            .withPort(8080)
            .with(
                new HttpProtocol(), (session) -> HttpResponse.newFixedLengthResponse(
                    StandardHttpStatus.OK,
                    helloWorld
                )
                    .header("Content-Type", "text/plain")
            )
            .build();

        server.start(); // Open up http://127.0.0.1:8080
    }

}
