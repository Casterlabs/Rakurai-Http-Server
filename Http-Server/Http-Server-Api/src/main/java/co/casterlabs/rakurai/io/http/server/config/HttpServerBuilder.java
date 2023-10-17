package co.casterlabs.rakurai.io.http.server.config;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import co.casterlabs.rakurai.io.http.server.HttpListener;
import co.casterlabs.rakurai.io.http.server.HttpServer;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public abstract class HttpServerBuilder {
    /* General */
    protected @NonNull String hostname = "0.0.0.0";

    protected int port = 80;

    protected SSLConfiguration ssl;

    protected boolean behindProxy = false;

    /* Protocols */
    @Deprecated
    protected boolean SPDYEnabled = false;

    protected boolean http2Enabled = true;

    /* Abstract things */
    public abstract HttpServer build(@NonNull HttpListener listener) throws IOException;

    public abstract HttpServer buildSecure(@NonNull HttpListener listener) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException;

    public abstract HttpServerImplementation getImplementation();

    /* Util */

    @Override
    public HttpServerBuilder clone() {
        HttpServerBuilder nu = get(this.getImplementation());
        nu.hostname = this.hostname;
        nu.port = this.port;
        if (this.ssl != null) nu.ssl = this.ssl.clone();
        nu.behindProxy = this.behindProxy;
        nu.SPDYEnabled = this.SPDYEnabled;
        nu.http2Enabled = this.http2Enabled;
        return nu;
    }

    /* Builders */
    public static HttpServerBuilder get(@NonNull HttpServerImplementation implementation) {
        switch (implementation) {
            case NANO:
                return getNanoBuilder();

            case UNDERTOW:
                return getUndertowBuilder();

            case RAKURAI:
                return getRakuraiBuilder();
        }

        throw new IllegalArgumentException(); // Hush mr compiley.
    }

    public static HttpServerBuilder getNanoBuilder() {
        try {
            Class<?> clazz = Class.forName("co.casterlabs.rakurai.http.server.impl.nano.NanoHttpServerBuilder");
            return (HttpServerBuilder) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to instantiate nanohttpd impl.", e);
        }
    }

    public static HttpServerBuilder getUndertowBuilder() {
        try {
            Class<?> clazz = Class.forName("co.casterlabs.rakurai.http.server.impl.undertow.UndertowHttpServerBuilder");
            return (HttpServerBuilder) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to instantiate undertow impl.", e);
        }
    }

    public static HttpServerBuilder getRakuraiBuilder() {
        try {
            Class<?> clazz = Class.forName("co.casterlabs.rakurai.http.server.impl.rakurai.RakuraiHttpServerBuilder");
            return (HttpServerBuilder) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new IllegalStateException("Unable to instantiate rakurai impl.", e);
        }
    }

}
