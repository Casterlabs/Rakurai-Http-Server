import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import co.casterlabs.rhs.HttpServer;
import co.casterlabs.rhs.HttpServerBuilder;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.http.HttpProtocol;
import co.casterlabs.rhs.protocol.http.HttpResponse;

public class TestInfinite {

    private static final InputStream INFINITE_STREAM = new InputStream() {
        @Override
        public int read() throws IOException {
            return 0;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return b.length;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return len;
        }

        @Override
        public int available() throws IOException {
            return Integer.MAX_VALUE;
        }
    };

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
//        FastLoggingFramework.setDefaultLevel(LogLevel.ALL);

        // cls && curl -o NUL http://localhost:8080/fixed
        // cls && curl -o NUL http://localhost:8080/chunked

        HttpServer server = new HttpServerBuilder()
            .withPort(8080)
            .with(
                new HttpProtocol(), (session) -> {
                    if (session.uri().path.startsWith("/chunked")) {
                        return HttpResponse.newChunkedResponse(StandardHttpStatus.OK, INFINITE_STREAM);
                    } else {
                        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, INFINITE_STREAM, Long.MAX_VALUE);
                    }
                }
            )
            .build();

        server.start(); // Open up http://127.0.0.1:8080
    }

}
