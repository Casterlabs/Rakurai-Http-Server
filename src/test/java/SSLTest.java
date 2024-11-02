import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;

import co.casterlabs.rhs.protocol.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.http.HttpProtoAdapter;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.server.HttpServer;
import co.casterlabs.rhs.server.HttpServerBuilder;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.pem.util.PemUtils;

public class SSLTest {

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
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
            .with(
                new HttpProtoAdapter(), (session) -> HttpResponse.newChunkedResponse(
                    StandardHttpStatus.OK,
                    new ByteArrayInputStream(String.format("Hello %s!", session.remoteNetworkAddress()).getBytes())
                )
                    .header("Content-Type", "text/plain")
            )
            .build();

        server.start(); // Open up http://127.0.0.1:8080
    }

}
