package co.casterlabs.rhs.server;

import java.io.IOException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public interface HttpServer {

    public void start() throws IOException;

    public void start(@NonNull SSLContext sslContext, @NonNull KeyStore keyStore) throws IOException;

    public void stop() throws IOException;

    public int getPort();

    public boolean isAlive();

    public FastLogger getLogger();

}
