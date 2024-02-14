import java.io.IOException;
import java.nio.file.Paths;

import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.protocol.StandardHttpStatus;
import co.casterlabs.rhs.server.HttpListener;
import co.casterlabs.rhs.server.HttpResponse;
import co.casterlabs.rhs.server.HttpServer;
import co.casterlabs.rhs.server.HttpServerBuilder;
import co.casterlabs.rhs.session.HttpSession;
import co.casterlabs.rhs.session.WebsocketListener;
import co.casterlabs.rhs.session.WebsocketSession;
import lombok.NonNull;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.pem.util.PemUtils;

public class SSLTest {

    public static void main(String[] args) throws IOException {
        X509ExtendedKeyManager keyManager = PemUtils.loadIdentityMaterial(Paths.get("ssl/chain.pem"), Paths.get("ssl/key.pem"));
        X509ExtendedTrustManager trustManager = PemUtils.loadTrustMaterial(Paths.get("ssl/crt.pem"));

        SSLFactory factory = SSLFactory.builder()
            .withIdentityMaterial(keyManager)
            .withTrustMaterial(trustManager)
//            .withCiphers("TLS_AES_256_GCM_SHA384", "abc123") // Unsupported ciphers are automatically excluded.
            .build();

        HttpServer server = new HttpServerBuilder()
            .withPort(443)
            .withSsl(factory)
            .build(new HttpListener() {
                @Override
                public @Nullable HttpResponse serveHttpSession(@NonNull HttpSession session) {
                    String body = String.format("Hello %s!", session.getRemoteIpAddress());

                    return HttpResponse
                        .newFixedLengthResponse(StandardHttpStatus.OK, body)
                        .setMimeType("text/plain");
                }

                @Override
                public @Nullable WebsocketListener serveWebsocketSession(@NonNull WebsocketSession session) {
                    // Returning null will drop the connection.
                    return null;
                }
            });

        server.start(); // Open up http://127.0.0.1:8080
    }

}
