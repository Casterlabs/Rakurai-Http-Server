package co.casterlabs.rhs.server;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.impl._RHSPackageBridge;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import nl.altindag.ssl.SSLFactory;

@Value
@Getter
@AllArgsConstructor
@SuppressWarnings("deprecation")
public class HttpServerBuilder {
    @NonNull
    private @With String hostname;

    private @With int port;

    @Nullable
    private @With SSLFactory ssl;

    private @With boolean behindProxy;

    public HttpServerBuilder() {
        this("localhost", 80, null, false);
    }

    public HttpServer build(@NonNull HttpListener listener) {
        return _RHSPackageBridge.build(listener, this);
    }

    public HttpServer buildSecure(@NonNull HttpListener listener) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        return _RHSPackageBridge.build(listener, this);
    }

}
