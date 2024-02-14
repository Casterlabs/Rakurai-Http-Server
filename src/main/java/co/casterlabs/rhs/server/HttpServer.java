package co.casterlabs.rhs.server;

import java.io.IOException;

import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public interface HttpServer {

    public void start() throws IOException;

    public void stop() throws IOException;

    public int getPort();

    public boolean isAlive();

    public FastLogger getLogger();

}
