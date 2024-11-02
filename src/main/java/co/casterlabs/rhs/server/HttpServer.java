package co.casterlabs.rhs.server;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedKeyManager;

import co.casterlabs.commons.functional.tuples.Pair;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.RHSProtocol;
import co.casterlabs.rhs.util.CaseInsensitiveMultiMap;
import co.casterlabs.rhs.util.DropConnectionException;
import co.casterlabs.rhs.util.HttpException;
import co.casterlabs.rhs.util.TaskExecutor;
import co.casterlabs.rhs.util.TaskExecutor.TaskUrgency;
import lombok.Getter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class HttpServer {
    private static final byte[] HTTP_1_1_UPGRADE_REJECT = "HTTP/1.1 400 Cannot Upgrade\r\n\r\n".getBytes(RHSConnection.CHARSET);

    private static final CaseInsensitiveMultiMap ZERO_LENGTH_HEADER = new CaseInsensitiveMultiMap.Builder()
        .put("Content-Length", "0")
        .build();

    private final FastLogger logger = new FastLogger("Rakurai RakuraiHttpServer");

    private final HttpServerBuilder config;

    private List<Socket> connectedClients = Collections.synchronizedList(new LinkedList<>());
    private ServerSocket serverSocket;
    private TaskExecutor executor;

    private @Getter boolean isSecure;

    @Deprecated
    HttpServer(HttpServerBuilder config) {
        this.config = config;
        this.executor = this.config.getBlockingExecutor();
    }

    private void doAccept() {
        try {
            Socket clientSocket = this.serverSocket.accept();
            this.connectedClients.add(clientSocket);

            this.executor.execute(() -> this.handle(clientSocket), TaskUrgency.DELAYABLE);
        } catch (Throwable t) {
            this.logger.severe("An error occurred whilst accepting a new connection:\n%s", t);
        }
    }

    @SuppressWarnings("deprecation")
    private void handle(Socket clientSocket) {
        String remoteAddress = formatAddress(clientSocket);
        this.logger.debug("New connection from %s", remoteAddress);

        FastLogger sessionLogger = this.logger.createChild("Connection: " + remoteAddress);

        TLSVersion tlsVersion = null;
        if (clientSocket instanceof SSLSocket) {
            SSLSession ssl = ((SSLSocket) clientSocket).getSession();
            tlsVersion = TLSVersion.parse(ssl.getProtocol());
        }

        try {
            BufferedInputStream bufferedInput = new BufferedInputStream(clientSocket.getInputStream());
            OutputStream output = clientSocket.getOutputStream();

            clientSocket.setTcpNoDelay(true);
            sessionLogger.trace("Set TCP_NODELAY.");

            while (true) {
                // Protocols can override this so we need to reset it every time.
                clientSocket.setSoTimeout(RHSConnection.HTTP_PERSISTENT_TIMEOUT * 1000);
                sessionLogger.trace("Set SO_TIMEOUT to %dms.", RHSConnection.HTTP_PERSISTENT_TIMEOUT * 1000);

                RHSConnection connection = RHSConnection.accept(
                    sessionLogger,
                    bufferedInput, output,
                    remoteAddress, port(),
                    tlsVersion
                );
                sessionLogger.debug("Handling request...");

                connection.logger.debug("Version: %s, Request headers: %s", connection.httpVersion, connection.headers);

                String protocolName = "http";
                switch (connection.httpVersion) {
                    case HTTP_1_1: {
                        String connectionHeader = connection.headers.getSingleOrDefault("Connection", "").toLowerCase();
                        if (connectionHeader.toLowerCase().contains("upgrade")) {
                            String upgradeTo = connection.headers.getSingle("Upgrade");
                            if (upgradeTo == null) upgradeTo = "";
                            protocolName = upgradeTo.toLowerCase();
                        }
                        break;
                    }

                    case HTTP_0_9:
                    case HTTP_1_0:
                        break;
                }

                Pair<RHSProtocol<?, ?, ?>, Object> protocolPair = this.config.getProtocols().get(protocolName);
                if (protocolPair == null) {
                    connection.output.write(HTTP_1_1_UPGRADE_REJECT);
                    break;
                }

                RHSProtocol<?, ?, ?> protocol = protocolPair.a();
                Object handler = protocolPair.b();

                try {
                    Object session = protocol.accept(connection);
                    Object response = protocol.$handle_cast(session, handler);
                    if (response == null) throw new DropConnectionException();

                    boolean acceptAnotherRequest = protocol.$process_cast(session, response, connection, this.executor);
                    if (acceptAnotherRequest) {
                        // We're keeping the connection, let the while{} block do it's thing.
                        sessionLogger.debug("Keeping connection alive for subsequent requests.");
                    } else {
                        // Break out of this torment.
                        break;
                    }
                } catch (HttpException e) {
                    connection.writeOutStatus(e.status);
                    connection.writeOutHeaders(ZERO_LENGTH_HEADER);
                    break;
                }
            }
        } catch (DropConnectionException d) {
            sessionLogger.debug("Dropping connection!\n%s", d);
        } catch (Throwable e) {
            if (!shouldIgnoreThrowable(e)) {
                sessionLogger.fatal("An error occurred whilst handling request:\n%s", e);
            }
        } finally {
            safeClose(clientSocket);
            this.connectedClients.remove(clientSocket);
            this.logger.debug("Closed connection from %s", remoteAddress);
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
                    this.doAccept();
                }
            });
            acceptThread.setName("RakuraiHttpServer - " + this.config.getHostname() + " - " + this.port());
            acceptThread.setDaemon(false);
            acceptThread.start();
        } catch (Exception e) {
            this.serverSocket = null;
            throw new IOException("Unable to start server", e);
        }
    }

    public void stop(boolean disconnectClients) throws IOException {
        try {
            if (this.isAlive()) {
                this.serverSocket.close();
            }

            if (disconnectClients) {
                new ArrayList<>(this.connectedClients) // Copy.
                    .forEach(HttpServer::safeClose);
                this.connectedClients.clear();
            }
        } finally {
            this.serverSocket = null;
        }
    }

    public boolean isAlive() {
        return this.serverSocket != null;
    }

    public int port() {
        return this.isAlive() ? //
            this.serverSocket.getLocalPort() : this.config.getPort();
    }

    private static void safeClose(Closeable c) {
        try {
            c.close();
        } catch (Exception ignored) {}
    }

}
