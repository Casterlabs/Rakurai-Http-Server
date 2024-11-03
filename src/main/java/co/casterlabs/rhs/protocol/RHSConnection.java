package co.casterlabs.rhs.protocol;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.HttpStatus;
import co.casterlabs.rhs.HttpVersion;
import co.casterlabs.rhs.TLSVersion;
import co.casterlabs.rhs.protocol.ConnectionUtil.RequestLineInfo;
import co.casterlabs.rhs.util.CaseInsensitiveMultiMap;
import co.casterlabs.rhs.util.HttpException;
import co.casterlabs.rhs.util.OverzealousInputStream;
import co.casterlabs.rhs.util.SimpleUri;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@RequiredArgsConstructor
public class RHSConnection implements Closeable {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O");

    public static final int HTTP_PERSISTENT_TIMEOUT = 30;
    public static final Charset CHARSET = Charset.forName(System.getProperty("rakurai.http.charset", "ISO-8859-1"));

    public final FastLogger logger;

    public final int guessedMtu;

    public final OverzealousInputStream input; // Cannot be final, has to be swappable with an OverzealousInputStream.
    public final OutputStream output;
    public final String remoteAddress;
    public final int serverPort;

    public final String method;
    public final SimpleUri uri;
    public final CaseInsensitiveMultiMap headers;

    public final HttpVersion httpVersion;
    public final @Nullable TLSVersion tlsVersion; // null, if TLS was not used

    public boolean expectFulfilled = false;

    @Override
    public void close() {
        try {
            this.input.close();
        } catch (IOException ignored) {}
        try {
            this.output.close();
        } catch (IOException ignored) {}
    }

    public final List<String> hops() {
        List<String> hops = new LinkedList<>();

        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-For
        List<String> forwardedForHeader = this.headers.get("X-Forwarded-For");
        if (forwardedForHeader != null) {
            for (String list : forwardedForHeader) {
                for (String hop : list.split(",")) {
                    hops.add(hop.trim());
                }
            }
        }

        // Add the final hop. If we aren't proxied then this has the effect of adding
        // the actual IP address to the list.
        String finalHop = this.remoteAddress;
        hops.add(finalHop);

        return hops;
    }

    /* ---------------- */
    /* Write Utilities  */
    /* ---------------- */

    public void writeOutStatus(HttpStatus status) throws IOException {
        // 0.9 doesn't have a status line, so we don't write it out.
        if (this.httpVersion == HttpVersion.HTTP_0_9) return;

        this.writeString(this.httpVersion.toString());
        this.writeString(" ");
        this.writeString(status.statusString());
        this.writeString("\r\n");
    }

    public void writeOutHeaders(CaseInsensitiveMultiMap headers) throws IOException {
        // 0.9 doesn't have headers, so we don't write it out.
        if (this.httpVersion == HttpVersion.HTTP_0_9) return;

        for (Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                this.writeString(key);
                this.writeString(": ");
                this.writeString(value);
                this.writeString("\r\n");
            }
        }

        // Write the separation line.
        this.writeString("\r\n");
    }

    public void writeOutHeaders(Map<String, String> headers) throws IOException {
        // 0.9 doesn't have headers, so we don't write it out.
        if (this.httpVersion == HttpVersion.HTTP_0_9) return;

        for (Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            this.writeString(key);
            this.writeString(": ");
            this.writeString(value);
            this.writeString("\r\n");
        }

        // Write the separation line.
        this.writeString("\r\n");
    }

    public void writeString(String str) throws IOException {
        this.output.write(str.getBytes(CHARSET));
    }

    /* ---------------- */
    /* Read Utilities   */
    /* ---------------- */

    public CaseInsensitiveMultiMap readHeaders() throws IOException, HttpException {
        return ConnectionUtil.readHeaders(this.input, this.guessedMtu);
    }

    /* ---------------- */
    /* Static Utilities */
    /* ---------------- */

    public static RHSConnection accept(
        int guessedMtu,
        FastLogger logger,
        OverzealousInputStream input,
        OutputStream output,
        String remoteAddress,
        int serverPort,
        @Nullable TLSVersion tlsVersion
    ) throws IOException, HttpException {
        RequestLineInfo requestLine = ConnectionUtil.readRequestLine(input, guessedMtu);

        // Headers
        CaseInsensitiveMultiMap headers;
        if (requestLine.httpVersion == HttpVersion.HTTP_0_9) {
            // HTTP/0.9 doesn't have headers.
            headers = CaseInsensitiveMultiMap.EMPTY;
        } else {
            headers = ConnectionUtil.readHeaders(input, guessedMtu);
        }

        SimpleUri uri = SimpleUri.from(headers.getSingleOrDefault("Host", ""), requestLine.uriPath);

        return new RHSConnection(logger, guessedMtu, input, output, remoteAddress, serverPort, requestLine.method, uri, headers, requestLine.httpVersion, tlsVersion);
    }

    public static String getHttpTime() {
        return TIME_FORMATTER.format(ZonedDateTime.now(ZoneOffset.UTC));
    }

}
