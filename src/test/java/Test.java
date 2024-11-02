import java.io.ByteArrayInputStream;
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
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class Test {

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        FastLoggingFramework.setDefaultLevel(LogLevel.ALL);
        HttpServer server = new HttpServerBuilder()
            .withPort(8080)
            .with(
                new HttpProtocol(), (session) -> {
                    String str = String.format("Hello %s!", session.remoteNetworkAddress());
                    if (session.uri().path.startsWith("/chunked")) {
                        return HttpResponse.newChunkedResponse(StandardHttpStatus.OK, new ByteArrayInputStream(str.getBytes()));
                    } else {
                        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, str);
                    }
                }
            )
            .build();

        server.start(); // Open up http://127.0.0.1:8080
    }

}
