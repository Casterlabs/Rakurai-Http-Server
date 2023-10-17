package co.casterlabs.rakurai.http.server.impl.rakurai.io;

import java.io.IOException;
import java.io.OutputStream;

import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@RequiredArgsConstructor
public class NonCloseableOutputStream extends OutputStream {
    private final OutputStream out;
    private final FastLogger sessionLogger;

    private long bytesWritten = 0;

    @Override
    public void close() throws IOException {
        this.sessionLogger.trace("Wrote exactly: %d bytes", this.bytesWritten);
        // Don't actually close the outputstream.
    }

    @Override
    public void write(int b) throws IOException {
        this.out.write(b);
        this.bytesWritten += 1;
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b.length == 0) return;
        this.out.write(b);
        this.bytesWritten += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0) return;
        this.out.write(b, off, len);
        this.bytesWritten += len;
    }

}
