package co.casterlabs.rhs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedKeyManager;

import co.casterlabs.commons.functional.tuples.Pair;
import co.casterlabs.commons.io.streams.MTUOutputStream;
import co.casterlabs.commons.io.streams.OverzealousInputStream;
import co.casterlabs.rhs.protocol.HeaderValue;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.RHSProtocol;
import co.casterlabs.rhs.protocol.exceptions.DropConnectionException;
import co.casterlabs.rhs.protocol.exceptions.HttpException;
import co.casterlabs.rhs.util.TaskExecutor;
import lombok.Getter;
import lombok.experimental.Accessors;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@Accessors(fluent = true)
public class HttpServer {
    private final @Getter FastLogger logger = new FastLogger("Rakurai RakuraiHttpServer");

    private final HttpServerBuilder config;

    private List<Socket> connectedClients = Collections.synchronizedList(new LinkedList<>());
    private ServerSocket serverSocket;
    private TaskExecutor executor;

    private @Getter boolean isSecure;

    @Deprecated
    HttpServer(HttpServerBuilder config) {
        this.config = config;
        this.executor = this.config.taskExecutor();
    }

    /* ---------------- */
    /* API              */
    /* ---------------- */

    public synchronized void start() throws IOException {
        if (this.isAlive()) return;

        try {
            if (this.config.ssl() == null) {
                this.serverSocket = new ServerSocket();
            } else {
                SSLServerSocketFactory factory = this.config.ssl().getSslServerSocketFactory();

                // If the certificate doesn't support EC algs, then we need to disable them.
                List<String> cipherSuitesToUse = new ArrayList<>(this.config.ssl().getCiphers());
                {
                    X509ExtendedKeyManager keyManager = this.config.ssl().getKeyManager().get();

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
            this.serverSocket.bind(new InetSocketAddress(this.config.hostname(), this.config.port()));

            Thread acceptThread = new Thread(() -> {
                while (!this.serverSocket.isClosed()) {
                    this.doAccept();
                }
            });
            acceptThread.setName("RakuraiHttpServer - " + this.config.hostname() + " - " + this.port());
            acceptThread.setDaemon(false);
            acceptThread.start();
        } catch (Exception e) {
            this.serverSocket = null;
            throw new IOException("Unable to start server", e);
        }
    }

    public synchronized void stop(boolean disconnectClients) throws IOException {
        try {
            if (this.isAlive()) {
                this.serverSocket.close();
            }

            if (disconnectClients) {
                new ArrayList<>(this.connectedClients) // Copy.
                    .forEach((c) -> {
                        try {
                            c.close();
                        } catch (IOException ignored) {}
                    });
                this.connectedClients.clear();
            }
        } finally {
            this.serverSocket = null;
        }
    }

    public synchronized boolean isAlive() {
        return this.serverSocket != null;
    }

    public int port() {
        return this.config.port();
    }

    /* ---------------- */
    /* Internals        */
    /* ---------------- */

    private void doAccept() {
        try {
            Socket clientSocket = this.serverSocket.accept();
            this.connectedClients.add(clientSocket);

            this.executor.execute(() -> this.handle(clientSocket));
        } catch (Throwable t) {
            this.logger.severe("An error occurred whilst accepting a new connection:\n%s", t);
        }
    }

    @SuppressWarnings("deprecation")
    private void handle(Socket clientSocket) {
        int guessedMtu = guessMtu(clientSocket);
        String remoteAddress = formatAddress(clientSocket);

        this.logger.debug("New connection from %s", remoteAddress);
        FastLogger sessionLogger = this.logger.createChild("Connection: " + remoteAddress);

        try (clientSocket) {
            TLSVersion tlsVersion = null;
            if (clientSocket instanceof SSLSocket) {
                SSLSession ssl = ((SSLSocket) clientSocket).getSession();
                tlsVersion = TLSVersion.parse(ssl.getProtocol());
            }

            clientSocket.setTcpNoDelay(true);
            sessionLogger.trace("Set TCP_NODELAY.");

            if (clientSocket.isInputShutdown() || clientSocket.isOutputShutdown()) {
                this.logger.debug("%s was closed before we could handle it. Oh well.", remoteAddress);
                return;
            }

            OverzealousInputStream input = new OverzealousInputStream(clientSocket.getInputStream());
            OutputStream output = new MTUOutputStream(clientSocket.getOutputStream(), guessedMtu);

            while (true) {
                int soTimeout = Math.max(this.config.keepAliveSeconds(), this.config.minSoTimeoutSeconds()) * 1000;
                clientSocket.setSoTimeout(soTimeout);
                sessionLogger.trace("Set SO_TIMEOUT to %dms.", soTimeout);

                RHSConnection connection = RHSConnection.accept(
                    guessedMtu,
                    this.config.keepAliveSeconds(),
                    soTimeout,
                    sessionLogger,
                    input, output,
                    remoteAddress, port(),
                    tlsVersion,
                    this.config
                );

                try {
                    sessionLogger.debug("Handling request...");

                    sessionLogger.debug("Version: %s, Request headers: %s", connection.httpVersion, connection.headers);

                    List<String> toUpgradeTo = Arrays.asList("http");
                    switch (connection.httpVersion) {
                        case HTTP_1_1: {
                            String connectionHeader = connection.headers.getSingleOrDefault("Connection", HeaderValue.EMPTY).raw().toLowerCase();
                            if (connectionHeader.contains("upgrade")) {
                                toUpgradeTo = connection.headers
                                    .getSingleOrDefault("Upgrade", HeaderValue.EMPTY)
                                    .delimited(",")
                                    .stream()
                                    .map(HeaderValue::raw)
                                    .collect(Collectors.toList());
                            }
                            break;
                        }

                        case HTTP_0_9:
                        case HTTP_1_0:
                            break;
                    }

                    Pair<RHSProtocol<?, ?, ?>, Object> protocolPair = null;
                    for (String protocolName : toUpgradeTo) {
                        protocolPair = this.config.protocols().get(protocolName);
                        if (protocolPair != null) {
                            break;
                        }
                    }

                    if (protocolPair == null) {
                        connection.respond(HttpStatus.adapt(400, "Unable to upgrade to any of the following protocols: " + toUpgradeTo));
                        break;
                    }

                    RHSProtocol<?, ?, ?> protocol = protocolPair.a();
                    Object handler = protocolPair.b();

                    Object session = protocol.accept(connection);
                    if (session == null) throw new DropConnectionException();

                    Object response = protocol.$handle_cast(session, handler);
                    if (response == null) throw new DropConnectionException();

                    boolean acceptAnotherRequest = protocol.$process_cast(session, response, connection);
                    if (acceptAnotherRequest) {
                        // We're keeping the connection, let the while{} block do it's thing.
                        sessionLogger.debug("Keeping connection alive for subsequent requests.");
                    } else {
                        // Break out of this torment.
                        return;
                    }
                } catch (HttpException e) {
                    connection.respond(e.status);
                }
            }
        } catch (DropConnectionException | HttpException d) {
            sessionLogger.debug("Dropping connection!\n%s", d);
        } catch (Throwable e) {
            if (shouldIgnoreThrowable(e)) {
                sessionLogger.debug("An error occurred whilst handling request, swallowing it:\n%s", e);
            } else {
                sessionLogger.fatal("An error occurred whilst handling request:\n%s", e);
            }
        } finally {
            this.connectedClients.remove(clientSocket);
            this.logger.debug("Closed connection from %s", remoteAddress);
            Thread.interrupted(); // Clear.
        }
    }

    /* ---------------- */
    /* Helpers          */
    /* ---------------- */

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

    private static int guessMtu(Socket clientSocket) {
        InetAddress address = clientSocket.getInetAddress();

        if (address.isLoopbackAddress()) {
            // In practice, loopback MTU is usually ~2^64. Because we use this MTU value to
            // determine our buffer size, we want to keep it small to avoid memory issues.
            return 8192; // Arbitrary.
        }

        if (address instanceof Inet6Address) {
            /*
             * ipv6 min. MTU = 1280
             * ipv6 header size = 40
             */
            return 1280 - 40;
        } else {
            /*
             * ipv4 min. MTU = 576 (though usually around 1500)
             * ipv4 header size = 20-60
             */
            return 1500 - 60;
        }
    }

    private static final Class<?>[] SILENCED_THROWABLES = {
            InterruptedException.class,
            SSLHandshakeException.class
    };

    private static final String[] SILENCED_THROWABLE_MESSAGES = {
            "socket closed",
            "socket is closed",
            "read timed out",
            "connection abort",
            "connection was abort",
            "connection or inbound has closed",
            "connection or outbound has closed",
            "connection reset",
            "received fatal alert: internal_error",
            "socket write error",
            "broken pipe",
            "reached end of stream before line was fully read"
    };

    private static boolean shouldIgnoreThrowable(Throwable t) {
        for (Class<?> c : SILENCED_THROWABLES) {
            if (t.getClass().isInstance(c)) return true;
        }

        String message = t.getMessage();
        if (message == null) return false;
        message = message.toLowerCase();

        for (String e : SILENCED_THROWABLE_MESSAGES) {
            if (message.contains(e)) return true;
        }

        return false;
    }

}
