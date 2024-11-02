package co.casterlabs.rhs.protocol;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.server.TLSVersion;
import co.casterlabs.rhs.util.CaseInsensitiveMultiMap;
import co.casterlabs.rhs.util.SimpleUri;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@RequiredArgsConstructor
public class RHSConnection implements Closeable {
    public static final Charset CHARSET = Charset.forName(System.getProperty("rakurai.http.charset", "ISO-8859-1"));

    public final FastLogger logger;

    public final BufferedInputStream input;
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

}
