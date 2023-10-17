package co.casterlabs.rakurai.http.server.impl.undertow;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import co.casterlabs.rakurai.io.http.server.HttpListener;
import co.casterlabs.rakurai.io.http.server.HttpServer;
import co.casterlabs.rakurai.io.http.server.config.HttpServerBuilder;
import co.casterlabs.rakurai.io.http.server.config.HttpServerImplementation;
import lombok.NonNull;

public class UndertowHttpServerBuilder extends HttpServerBuilder {

    @Override
    public HttpServer build(@NonNull HttpListener listener) {
        return new UndertowHttpServer(listener, this.hostname, this.port, this);
    }

    @Override
    public HttpServer buildSecure(@NonNull HttpListener listener) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        this.ssl.applyDHSize();

        KeyStore keystore = KeyStore.getInstance("jks");
        keystore.load(new FileInputStream(this.ssl.getKeystoreLocation()), this.ssl.getKeystorePassword());

        KeyManagerFactory managerFactory = KeyManagerFactory.getInstance("SunX509");
        managerFactory.init(keystore, this.ssl.getKeystorePassword());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keystore);

        return new UndertowHttpServer(
            listener,
            this.hostname,
            this.port,
            managerFactory.getKeyManagers(),
            trustManagerFactory.getTrustManagers(),
            this.ssl.convertTLS(),
            this.ssl.getEnabledCipherSuites(),
            this
        );
    }

    @Override
    public HttpServerImplementation getImplementation() {
        return HttpServerImplementation.UNDERTOW;
    }

}
