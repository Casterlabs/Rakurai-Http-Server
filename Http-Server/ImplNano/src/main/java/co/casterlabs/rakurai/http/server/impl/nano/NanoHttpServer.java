package co.casterlabs.rakurai.http.server.impl.nano;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import co.casterlabs.rakurai.StringUtil;
import co.casterlabs.rakurai.io.IOUtil;
import co.casterlabs.rakurai.io.http.HttpStatus;
import co.casterlabs.rakurai.io.http.StandardHttpStatus;
import co.casterlabs.rakurai.io.http.server.DropConnectionException;
import co.casterlabs.rakurai.io.http.server.HttpListener;
import co.casterlabs.rakurai.io.http.server.HttpResponse;
import co.casterlabs.rakurai.io.http.server.HttpResponse.ByteResponse;
import co.casterlabs.rakurai.io.http.server.HttpResponse.ResponseContent;
import co.casterlabs.rakurai.io.http.server.HttpResponse.StreamResponse;
import co.casterlabs.rakurai.io.http.server.HttpServer;
import co.casterlabs.rakurai.io.http.server.config.HttpServerBuilder;
import co.casterlabs.rakurai.io.http.server.config.HttpServerImplementation;
import co.casterlabs.rakurai.io.http.server.websocket.WebsocketListener;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.NanoWSD;
import lombok.Getter;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@SuppressWarnings("deprecation")
public class NanoHttpServer extends NanoWSD implements HttpServer {
    private @Getter FastLogger logger = new FastLogger("Rakurai NanoHttpServer");
    private HttpListener server;
    private boolean secure;

    private HttpServerBuilder config;

    static {
        try {
            Field field = NanoHTTPD.class.getDeclaredField("LOG");
            field.setAccessible(true);

            Logger log = (Logger) field.get(null);
            log.setLevel(Level.OFF); // Hush
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
    }

    public NanoHttpServer(HttpListener server, String hostname, int port, HttpServerBuilder config) {
        super(hostname, port);

        this.setAsyncRunner(new NanoRunner());

        this.secure = false;
        this.server = server;
        this.config = config;
    }

    public NanoHttpServer(HttpListener server, String hostname, int port, HttpServerBuilder config, WrappedSSLSocketFactory factory, String[] tls) {
        this(server, hostname, port, config);

        this.makeSecure(factory, tls);
        this.secure = true;
    }

    @Override
    public void start() throws IOException {
        this.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    // Serves http sessions or calls super to serve websockets
    @SneakyThrows
    @Override
    public Response serve(IHTTPSession nanoSession) {
        if (this.isWebsocketRequested(nanoSession)) {
            return super.serve(nanoSession);
        }

        long start = System.currentTimeMillis();
        NanoHttpSession session = new NanoHttpSession(nanoSession, this.getListeningPort(), this.config, this.logger);
        HttpResponse response = null;

        try {
            response = this.server.serveSession(session.getHost(), session, this.secure);

            if (response == null) {
                return NanoHTTPD.newFixedLengthResponse(Status.NOT_IMPLEMENTED, "text/plaintext", "");
            }

            if (response.getStatus() == StandardHttpStatus.NO_RESPONSE) {
                throw new DropConnectionException();
            }

            String mime = response.getAllHeaders().getOrDefault("content-type", "text/plaintext");
            IStatus status = convertStatus(response.getStatus());
            ResponseContent content = response.getContent();

            Response nanoResponse;
            if (content instanceof ByteResponse) {
                ByteResponse resp = (ByteResponse) response.getContent();
                nanoResponse = NanoHTTPD.newFixedLengthResponse(
                    status,
                    mime,
                    new ByteArrayInputStream(resp.getResponse()),
                    resp.getLength()
                );
            } else if (content instanceof StreamResponse) {
                StreamResponse resp = (StreamResponse) response.getContent();

                if (resp.getLength() < 0) {
                    nanoResponse = NanoHTTPD.newChunkedResponse(status, mime, resp.getResponse());
                } else {
                    nanoResponse = NanoHTTPD.newFixedLengthResponse(
                        status,
                        mime,
                        resp.getResponse(),
                        resp.getLength()
                    );
                }
            } else {
                session.getLogger().fatal("NanoHTTPD only supports the stock ByteResponse and StreamResponse. Unable to further serve request.");
                throw new DropConnectionException();
            }

            for (Map.Entry<String, String> header : response.getAllHeaders().entrySet()) {
                // Check prevents duplicate headers
                if (header.getKey().equalsIgnoreCase("content-type") ||
                    header.getKey().equalsIgnoreCase("content-length")) {
                    continue;
                }

                nanoResponse.addHeader(
                    StringUtil.prettifyHeader(header.getKey()),
                    header.getValue()
                );
            }

            double time = (System.currentTimeMillis() - start) / 1000d;
            logger.debug("Served HTTP %s %s %s (%.2fs)", session.getMethod().name(), session.getRemoteIpAddress(), session.getHost() + session.getUri(), time);

            return nanoResponse;
        } catch (DropConnectionException e) {
            logger.debug("Dropped HTTP %s %s %s", session.getMethod().name(), session.getRemoteIpAddress(), session.getHost() + session.getUri());
            throw e;
        } catch (Exception e) {
            session.getLogger().severe("An exception occurred whilst handling request:\n%s", e);
            return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plaintext", "");
        } finally {
            if (response != null) {
                IOUtil.safeClose(response.getContent());
            }
        }
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession nanoSession) {
        long start = System.currentTimeMillis();
        NanoWebsocketSessionWrapper session = new NanoWebsocketSessionWrapper(nanoSession, this.getListeningPort(), this.config, this.logger);

        WebsocketListener listener = this.server.serveWebsocketSession(session.getHost(), session, this.secure);

        if (listener != null) {
            double time = (System.currentTimeMillis() - start) / 1000d;
            logger.debug("Served websocket %s %s (%.2fs)", session.getRemoteIpAddress(), session.getHost() + session.getUri(), time);

            return new NanoWebsocketWrapper(nanoSession, listener, session);
        } else {
            logger.debug("Dropped websocket %s %s", session.getRemoteIpAddress(), session.getHost() + session.getUri());
            throw new DropConnectionException();
        }
    }

    @Override
    public int getPort() {
        return this.getListeningPort();
    }

    private static IStatus convertStatus(HttpStatus status) {
        return new IStatus() {
            @Override
            public String getDescription() {
                return status.getStatusString(); // What the hell Nano
            }

            @Override
            public int getRequestStatus() {
                return status.getStatusCode();
            }
        };
    }

    @Override
    public HttpServerImplementation getImplementation() {
        return HttpServerImplementation.NANO;
    }

}
