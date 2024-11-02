package co.casterlabs.rhs.server;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.functional.tuples.Pair;
import co.casterlabs.rhs.protocol.RHSProtoAdapter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import nl.altindag.ssl.SSLFactory;

@Value
@Getter(AccessLevel.PACKAGE)
@AllArgsConstructor
@SuppressWarnings("deprecation")
public class HttpServerBuilder {
    @NonNull
    private @With String hostname;

    private @With int port;

    @Nullable
    private @With SSLFactory ssl;

    private @With boolean behindProxy;

    private @With Map<String, Pair<RHSProtoAdapter<?, ?, ?>, Object>> protocols;

    private @With Consumer<Runnable> blockingExecutor;

    public HttpServerBuilder() {
        this(
            "::", 80,
            null, false,
            Collections.emptyMap(),
            (r) -> new Thread(r).start()
        );
    }

    public <S, R, H> HttpServerBuilder with(RHSProtoAdapter<S, R, H> protocol, H handler) {
        Map<String, Pair<RHSProtoAdapter<?, ?, ?>, Object>> protocols = new HashMap<>(this.protocols);
        protocols.put(protocol.name(), new Pair<>(protocol, handler));
        return this.withProtocols(protocols);
    }

    public HttpServer build() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        return new HttpServer(this);
    }

}
