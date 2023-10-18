package co.casterlabs.rhs.impl;

import co.casterlabs.rhs.server.HttpListener;
import co.casterlabs.rhs.server.HttpServer;
import co.casterlabs.rhs.server.HttpServerBuilder;
import lombok.NonNull;

/*
 *  The implementation has package-level visibility. This is needed to expose limited information to the public classes.
 */
@Deprecated
public class _RHSPackageBridge {

    public static HttpServer build(@NonNull HttpListener listener, HttpServerBuilder clone) {
        return new RakuraiHttpServer(listener, clone);
    }

}
