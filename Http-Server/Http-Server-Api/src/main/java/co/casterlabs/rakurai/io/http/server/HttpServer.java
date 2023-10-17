package co.casterlabs.rakurai.io.http.server;

import java.io.IOException;

import co.casterlabs.rakurai.io.http.server.config.HttpServerImplementation;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public interface HttpServer {

    public void start() throws IOException;

    public void stop() throws IOException;

    public int getPort();

    public boolean isAlive();

    public FastLogger getLogger();

    public HttpServerImplementation getImplementation();

}
