package co.casterlabs.rakurai.http.server.impl.nano;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;

import co.casterlabs.rakurai.io.http.server.HttpListener;
import co.casterlabs.rakurai.io.http.server.HttpServer;
import co.casterlabs.rakurai.io.http.server.config.HttpServerBuilder;
import co.casterlabs.rakurai.io.http.server.config.HttpServerImplementation;
import fi.iki.elonen.NanoHTTPD;
import lombok.NonNull;

public class NanoHttpServerBuilder extends HttpServerBuilder {

    @Override
    public HttpServer build(@NonNull HttpListener listener) throws IOException {
        return new NanoHttpServer(listener, this.hostname, this.port, this);
    }

    @Override
    public HttpServer buildSecure(@NonNull HttpListener listener) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        this.ssl.applyDHSize();

        KeyStore keystore = KeyStore.getInstance("jks");
        keystore.load(new FileInputStream(this.ssl.getKeystoreLocation()), this.ssl.getKeystorePassword());

        KeyManagerFactory managerFactory = KeyManagerFactory.getInstance("SunX509");
        managerFactory.init(keystore, this.ssl.getKeystorePassword());

        SSLServerSocketFactory factory = NanoHTTPD.makeSSLSocketFactory(keystore, managerFactory);

        return new NanoHttpServer(listener, this.hostname, this.port, this, new WrappedSSLSocketFactory(factory, this.ssl), this.ssl.convertTLS());
    }

    @Override
    public HttpServerImplementation getImplementation() {
        return HttpServerImplementation.NANO;
    }

}
