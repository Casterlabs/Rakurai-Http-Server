package co.casterlabs.rhs;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.functional.tuples.Pair;
import co.casterlabs.rhs.protocol.RHSProtocol;
import co.casterlabs.rhs.util.TaskExecutor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import lombok.experimental.Accessors;
import nl.altindag.ssl.SSLFactory;

@Value
@Getter
@AllArgsConstructor
@Accessors(fluent = true)
@SuppressWarnings("deprecation")
public class HttpServerBuilder {
    @NonNull
    private @With String hostname;

    private @With int port;

    @Nullable
    private @With SSLFactory ssl;

    private @With boolean behindProxy;

    @NonNull
    private @With String serverHeader;

    private @With Map<String, Pair<RHSProtocol<?, ?, ?>, Object>> protocols;

    private @With TaskExecutor taskExecutor;

    public HttpServerBuilder() {
        this(
            "::", 80,
            null, false,
            "Rakurai/latest",
            Collections.emptyMap(),
            (r, u) -> {
                Thread t = new Thread(r);
                t.setName(u + " THREAD");
                t.start();
                return t;
            }
        );
    }

    public <S, R, H> HttpServerBuilder with(RHSProtocol<S, R, H> protocol, H handler) {
        Map<String, Pair<RHSProtocol<?, ?, ?>, Object>> protocols = new HashMap<>(this.protocols);
        protocols.put(protocol.name(), new Pair<>(protocol, handler));
        return this.withProtocols(protocols);
    }

    public HttpServer build() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        return new HttpServer(this);
    }

}
