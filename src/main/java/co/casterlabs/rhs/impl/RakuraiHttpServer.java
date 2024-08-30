package co.casterlabs.rhs.impl;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.X509ExtendedKeyManager;

import co.casterlabs.rhs.protocol.HttpMethod;
import co.casterlabs.rhs.protocol.HttpVersion;
import co.casterlabs.rhs.server.HttpListener;
import co.casterlabs.rhs.server.HttpResponse;
import co.casterlabs.rhs.server.HttpServer;
import co.casterlabs.rhs.server.HttpServerBuilder;
import co.casterlabs.rhs.session.WebsocketListener;
import co.casterlabs.rhs.util.DropConnectionException;
import lombok.Getter;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@Getter
class RakuraiHttpServer implements HttpServer {
    private static final byte[] HTTP_1_1_UPGRADE_REJECT = "HTTP/1.1 400 Bad Request\r\n\r\n".getBytes(HttpProtocol.HEADER_CHARSET);
    private static final byte[] HTTP_1_1_CONTINUE_LINE = "HTTP/1.1 100 Continue\r\n\r\n".getBytes(HttpProtocol.HEADER_CHARSET);

    private static final ExecutorService blockingExecutor = Executors.newCachedThreadPool();

    private final FastLogger logger = new FastLogger("Rakurai RakuraiHttpServer");

    private final HttpListener listener;
    private final HttpServerBuilder config;

    private List<Socket> connectedClients = Collections.synchronizedList(new LinkedList<>());
    private ServerSocket serverSocket;
    private boolean isSecure;

    @Deprecated
    RakuraiHttpServer(HttpListener listener, HttpServerBuilder config) {
        this.listener = listener;
        this.config = config;
    }

    private void doRead() {
        try {
            Socket clientSocket = this.serverSocket.accept();
            this.connectedClients.add(clientSocket);

            String remoteAddress = formatAddress(clientSocket);
            this.logger.debug("New connection from %s", remoteAddress);

            Thread.ofVirtual()
                .name("RakuraiHttpServer - Read Thread " + remoteAddress, 0)
                .start(() -> {
                    FastLogger sessionLogger = this.logger.createChild("Connection: " + remoteAddress);
                    sessionLogger.debug("Handling request...");

                    try {
                        clientSocket.setTcpNoDelay(true);
                        sessionLogger.trace("Set TCP_NODELAY.");

                        BufferedInputStream in = new BufferedInputStream(clientSocket.getInputStream());

                        while (true) {
                            clientSocket.setSoTimeout(HttpProtocol.HTTP_PERSISTENT_TIMEOUT * 1000); // 1m timeout for regular requests.
                            sessionLogger.trace("Set SO_TIMEOUT to %dms.", HttpProtocol.HTTP_PERSISTENT_TIMEOUT * 1000);

                            boolean acceptAnotherRequest = this.handle(in, clientSocket, sessionLogger);
                            if (acceptAnotherRequest) {
                                // We're keeping the connection, let the while{} block do it's thing.
                                sessionLogger.debug("Keeping connection alive for subsequent requests.");
                            } else {
                                // Break out of this torment.
                                break;
                            }
                        }
                    } catch (DropConnectionException ignored) {
                        sessionLogger.debug("Dropping connection.");
                    } catch (Throwable e) {
                        if (!shouldIgnoreThrowable(e)) {
                            sessionLogger.fatal("An error occurred whilst handling request:\n%s", e);
                        }
                    } finally {
                        Thread.interrupted(); // Clear interrupt status.

                        try {
                            clientSocket.close();
                        } catch (IOException e) {
                            sessionLogger.severe("An error occurred whilst closing the socket:\n%s", e);
                        }

                        this.connectedClients.remove(clientSocket);
                        this.logger.debug("Closed connection from %s", remoteAddress);
                    }
                });
        } catch (Throwable t) {
            this.logger.severe("An error occurred whilst accepting a new connection:\n%s", t);
        }
    }

    private boolean handle(BufferedInputStream in, Socket clientSocket, FastLogger sessionLogger) throws IOException, NoSuchAlgorithmException {
        HttpResponse httpResponse = null;
        WebsocketListener websocketListener = null;
        WebsocketImpl websocket = null;

        try {
            sessionLogger.trace("Handling request...");

            HttpSessionImpl session = null;
            HttpVersion version = HttpVersion.HTTP_1_0; // Our default.
            // Note that we don't support 2.0 or 3.0, confirm this in RHSProtocol.

            // Catch any RHSHttpExceptions and convert them into responses.
            try {
                session = HttpProtocol.accept(sessionLogger, this, clientSocket, in);
                version = session.getVersion();
                sessionLogger = session.getLogger();
            } catch (HttpException e) {
                sessionLogger.severe("An error occurred whilst handling a request:\n%s", e);
                httpResponse = HttpResponse.newFixedLengthResponse(e.status);
            }

            sessionLogger.debug("Version: %s, Request headers: %s", version, session.getHeaders());

            boolean keepConnectionAlive = false;
            String protocol = "http";

            switch (version) {
                case HTTP_1_1: {
                    String connection = session.getHeader("Connection");
                    if (connection != null) {
                        if (connection.toLowerCase().contains("upgrade")) {
                            String upgradeTo = session.getHeader("Upgrade");
                            if (upgradeTo == null) upgradeTo = "";

                            switch (upgradeTo.toLowerCase()) {
                                case "websocket": {
                                    if (session.getMethod() != HttpMethod.GET) {
                                        sessionLogger.trace("Rejecting websocket upgrade, method was %s.", session.getRawMethod());
                                        clientSocket.getOutputStream().write(HTTP_1_1_UPGRADE_REJECT);
                                        return false;
                                    }
                                    protocol = "websocket";
                                    sessionLogger.trace("Upgrading to: websocket.");
                                    break;
                                }

                                default: {
                                    sessionLogger.trace("Rejecting upgrade: %s.", upgradeTo.toLowerCase());
                                    clientSocket.getOutputStream().write(HTTP_1_1_UPGRADE_REJECT);
                                    return false;
                                }
                            }
                        } else if (connection.toLowerCase().contains("keep-alive")) {
                            sessionLogger.trace("KA requested, obliging.");
                            keepConnectionAlive = true;
                        }
                    }

                    String expect = session.getHeader("Expect");
                    if (expect != null) {
                        if (expect.contains("100-continue")) {
                            // Immediately write a CONTINUE so that the client will send the body.
                            clientSocket.getOutputStream().write(HTTP_1_1_CONTINUE_LINE);
                            session.getLogger().trace("Response status line: HTTP/1.1 100 Continue");
                        }
                    }

                    break;
                }

                case HTTP_0_9:
                case HTTP_1_0:
                default:
                    break;
            }

            switch (protocol) {
                case "http": {
                    // We have a valid session, try to serve it.
                    // Note that response will always be null at this location IF session isn't.
                    if (session != null) {
                        sessionLogger.trace("Serving session...");
                        httpResponse = executeBlocking(this.listener::serveHttpSession, session);
                    }

                    sessionLogger.trace("Served.");

                    if (httpResponse == null) throw new DropConnectionException();
                    HttpProtocol.writeOutResponse(clientSocket, session, keepConnectionAlive, httpResponse);

                    if (keepConnectionAlive && session.hasBody()) {
                        InputStream bodyStream = session.getRequestBodyStream();

                        while (bodyStream.available() != -1) {
                            bodyStream.skip(Long.MAX_VALUE); // Skip as much as possible.
                        }
                    }

                    return keepConnectionAlive;
                }

                case "websocket": {
                    sessionLogger.trace("Handling websocket request...");

                    if (session != null) {
                        websocketListener = executeBlocking(this.listener::serveWebsocketSession, session);
                    }

                    if (websocketListener == null) throw new DropConnectionException();

                    OutputStream out = clientSocket.getOutputStream();

                    {
                        String wsVersion = session.getHeader("Sec-WebSocket-Version");
                        if (wsVersion == null) wsVersion = "";

                        switch (wsVersion) {
                            // Supported.
                            case "13":
                                break;

                            // Not supported.
                            default: {
                                sessionLogger.debug("Rejected websocket version: %s", wsVersion);
                                HttpProtocol.writeString("HTTP/1.1 426 Upgrade Required\r\n", out);
                                HttpProtocol.writeString("Sec-WebSocket-Version: 13\r\n", out);
                                HttpProtocol.writeString("\r\n", out);
                                return false;
                            }
                        }

                        sessionLogger.trace("Accepted websocket version: %s", wsVersion);
                    }

                    // Upgrade the connection.
                    HttpProtocol.writeString("HTTP/1.1 101 Switching Protocols\r\n", out);
                    sessionLogger.trace("Response status line: HTTP/1.1 101 Switching Protocols");

                    HttpProtocol.writeString("Connection: Upgrade\r\n", out);
                    HttpProtocol.writeString("Upgrade: websocket\r\n", out);

                    // Generate the key and send it out.
                    {
                        String clientKey = session.getHeader("Sec-WebSocket-Key");

                        if (clientKey != null) {
                            MessageDigest hash = MessageDigest.getInstance("SHA-1");
                            hash.reset();
                            hash.update(
                                clientKey
                                    .concat("258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                    .getBytes(StandardCharsets.UTF_8)
                            );

                            String acceptKey = Base64.getEncoder().encodeToString(hash.digest());
                            HttpProtocol.writeString("Sec-WebSocket-Accept: ", out);
                            HttpProtocol.writeString(acceptKey, out);
                            HttpProtocol.writeString("\r\n", out);
                        }
                    }

                    {
                        // Select the first WS protocol, if any are requested.
                        String wsProtocol = session.getHeader("Sec-WebSocket-Protocol");
                        if (wsProtocol != null) {
                            String first = wsProtocol.split(",")[0].trim();

                            HttpProtocol.writeString("Sec-WebSocket-Protocol: ", out);
                            HttpProtocol.writeString(first, out);
                            HttpProtocol.writeString("\r\n", out);
                        }
                    }

                    // Write the separation line.
                    HttpProtocol.writeString("\r\n", out);
                    sessionLogger.trace("WebSocket upgrade complete, handling frames.");

                    websocket = new WebsocketImpl(session, out, clientSocket);
                    executeBlocking(websocketListener::onOpen, websocket);

                    // Ping/pong mechanism.
                    clientSocket.setSoTimeout((int) (WebsocketProtocol.READ_TIMEOUT * 4)); // Timeouts should work differently for WS.
                    sessionLogger.trace("Set SO_TIMEOUT to %dms.", WebsocketProtocol.READ_TIMEOUT * 4);

                    final WebsocketImpl $websocket_pointer = websocket;

                    Thread readThread = Thread.currentThread();

                    Thread.ofVirtual()
                        .name("RakuraiHttpServer - Websocket Ping Thread", 0)
                        .start(() -> {
                            try {
                                while (!clientSocket.isClosed()) {
                                    WebsocketProtocol.doPing($websocket_pointer);
                                    Thread.sleep(WebsocketProtocol.READ_TIMEOUT / 2);
                                }
                            } catch (Exception ignored) {
                                safeClose(clientSocket); // Try to tell the read thread that the connection is ded.
                                readThread.interrupt();
                            }
                        });

                    sessionLogger.trace("Handling WS request...");
                    WebsocketProtocol.handleWebsocketRequest(clientSocket, session, websocket, websocketListener);

                    return false; // Close.
                }

                default:
                    return false;
            }
        } finally {
            if (httpResponse != null) {
                try {
                    httpResponse.getContent().close();
                } catch (Exception e) {
                    sessionLogger.severe("An error occurred whilst response content:\n%s", e);
                }
            }

            if (websocketListener != null) {
                try {
                    RakuraiHttpServer.executeBlocking(websocketListener::onClose, websocket);
                } catch (Exception e) {
                    sessionLogger.severe("An error occurred whilst response listener:\n%s", e);
                }
            }

            Thread.interrupted(); // Clear.
        }
    }

    private static String formatAddress(Socket clientSocket) {
        String address = //
            ((InetSocketAddress) clientSocket.getRemoteSocketAddress())
                .getAddress()
                .toString()
                .replace("/", "");

        if (address.indexOf(':') != -1) {
            // Better Format for ipv6 addresses :^)
            address = '[' + address + ']';
        }

        address += ':';
        address += clientSocket.getPort();

        return address;
    }

    private static boolean shouldIgnoreThrowable(Throwable t) {
        if (t instanceof InterruptedException) return true;
        if (t instanceof SSLHandshakeException) return true;

        String message = t.getMessage();
        if (message == null) return false;
        message = message.toLowerCase();

        if (message.contains("socket closed") ||
            message.contains("socket is closed") ||
            message.contains("read timed out") ||
            message.contains("connection abort") ||
            message.contains("connection or inbound has closed") ||
            message.contains("connection reset") ||
            message.contains("received fatal alert: internal_error") ||
            message.contains("socket write error")) return true;

        return false;
    }

    @Override
    public void start() throws IOException {
        if (this.isAlive()) return;

        try {
            if (this.config.getSsl() == null) {
                this.serverSocket = new ServerSocket();
            } else {
                SSLServerSocketFactory factory = this.config.getSsl().getSslServerSocketFactory();

                // If the certificate doesn't support EC algs, then we need to disable them.
                List<String> cipherSuitesToUse = new ArrayList<>(this.config.getSsl().getCiphers());
                {
                    X509ExtendedKeyManager keyManager = this.config.getSsl().getKeyManager().get();

                    boolean ECsupported = false;
                    for (String alias : keyManager.getClientAliases("RSA", null)) {
                        String publicKeyAlgorithm = keyManager.getPrivateKey(alias).getAlgorithm();
                        if (publicKeyAlgorithm.equals("EC")) {
                            ECsupported = true;
                            break;
                        }
                    }

                    if (!ECsupported) {
                        Iterator<String> it = cipherSuitesToUse.iterator();
                        boolean warnedECunsupported = false;

                        while (it.hasNext()) {
                            String cipherSuite = it.next();
                            if (cipherSuite.contains("_ECDH")) {
                                it.remove();

                                if (!warnedECunsupported) {
                                    warnedECunsupported = true;
                                    this.logger.warn("Elliptic-Curve Cipher Suites are not supported as your certificate does not use the EC public key algorithm.");
                                }
                            }
                        }
                    }
                }

                this.logger.info("Using the following Cipher Suites: %s.", cipherSuitesToUse);

                SSLServerSocket socket = (SSLServerSocket) factory.createServerSocket();
                socket.setEnabledCipherSuites(cipherSuitesToUse.toArray(new String[0]));
                socket.setUseClientMode(false);
                socket.setWantClientAuth(false);
                socket.setNeedClientAuth(false);

                this.serverSocket = factory.createServerSocket();
            }

            this.serverSocket.setReuseAddress(true);
            this.serverSocket.bind(new InetSocketAddress(this.config.getHostname(), this.config.getPort()));

            Thread acceptThread = new Thread(() -> {
                while (!this.serverSocket.isClosed()) {
                    this.doRead();
                }

                try {
                    this.stop();
                } catch (IOException e) {
                    this.logger.severe("An error occurred whilst tearing down server:\n%s", e);
                }
            });
            acceptThread.setName("RakuraiHttpServer - " + this.getPort());
            acceptThread.setDaemon(false);
            acceptThread.start();
        } catch (Exception e) {
            this.serverSocket = null;
            throw new IOException("Unable to start server", e);
        }
    }

    @Override
    public void stop() throws IOException {
        if (!this.isAlive()) return;

        try {
            this.serverSocket.close();

            new ArrayList<>(this.connectedClients)
                .forEach(RakuraiHttpServer::safeClose);
            this.connectedClients.clear();
        } finally {
            this.serverSocket = null;
        }
    }

    @Override
    public boolean isAlive() {
        return this.serverSocket != null;
    }

    @Override
    public int getPort() {
        return this.isAlive() ? //
            this.serverSocket.getLocalPort() : this.config.getPort();
    }

    @SneakyThrows
    static <A, R> R executeBlocking(Function<A, R> toExecute, A arg) {
        try {
            return blockingExecutor.submit(() -> {
                return toExecute.apply(arg);
            }).get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    static <A> void executeBlocking(Consumer<A> toExecute, A arg) {
        executeBlocking((_unused) -> {
            toExecute.accept(arg);
            return null;
        }, arg);
    }

    @SneakyThrows
    static void executeBlocking(Runnable toExecute) {
        executeBlocking((_unused) -> {
            toExecute.run();
            return null;
        }, null);
    }

    static void safeClose(Closeable c) {
        try {
            c.close();
        } catch (Exception ignored) {}
    }

}
