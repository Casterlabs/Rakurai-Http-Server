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
import co.casterlabs.rhs.util.TaskExecutor.Task;
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
    private static final int DEFAULT_KEEP_ALIVE_SECONDS = 60;

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

    /**
     * Negative or 0 to disable Keep-Alive
     */
    private @With int keepAliveSeconds;

    /**
     * {@link #keepAliveSeconds} will be used if it is larger than
     * {@link #minSoTimeoutSeconds}.
     * 
     * It is recommended to set this to at least 30 seconds.
     * 
     * @implSpec Value cannot be negative or 0.
     * 
     * @implNote The websocket ping interval is always 5 seconds.
     */
    private @With int minSoTimeoutSeconds;

    public HttpServerBuilder() {
        this(
            "::", 80,
            null, false,
            "Rakurai/latest",
            Collections.emptyMap(),
            (r) -> {
                Thread t = new Thread(r);
                t.setName("RHS THREAD");
                t.start();
                return new Task() {
                    @Override
                    public void interrupt() {
                        t.interrupt();
                    }

                    @Override
                    public void waitFor() throws InterruptedException {
                        if (this.isAlive()) {
                            t.join();
                        }
                    }

                    @Override
                    public boolean isAlive() {
                        return t.isAlive();
                    }
                };
            },
            DEFAULT_KEEP_ALIVE_SECONDS,
            DEFAULT_KEEP_ALIVE_SECONDS / 2
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
