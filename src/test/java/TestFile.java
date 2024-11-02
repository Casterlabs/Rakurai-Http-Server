import java.io.File;
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
import co.casterlabs.rhs.util.HttpException;

public class TestFile {

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
//        FastLoggingFramework.setDefaultLevel(LogLevel.ALL);
        HttpServer server = new HttpServerBuilder()
            .withPort(8080)
            .with(
                new HttpProtocol(), (session) -> {
                    try {
                        return HttpResponse.newRangedFileResponse(session, StandardHttpStatus.OK, new File("test.mp4"));
                    } catch (IOException e) {
//                        e.printStackTrace();
                        throw new HttpException(StandardHttpStatus.INTERNAL_ERROR);
                    }
                }
            )
            .build();

        server.start(); // Open up http://127.0.0.1:8080
    }

}
