package co.casterlabs.rhs.protocol.http;

import java.io.IOException;
import java.io.OutputStream;

import co.casterlabs.commons.async.Lock;
import co.casterlabs.rhs.protocol.RHSConnection;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class _ChunkedOutputStream extends OutputStream {
    private final Lock lock = new Lock();

    private static final byte[] END = "0\r\n\r\n".getBytes(RHSConnection.CHARSET);
    private static final byte[] NEWLINE = "\r\n".getBytes(RHSConnection.CHARSET);

    private final OutputStream output;

    private boolean alreadyClosed = false;

    @Override
    public void close() throws IOException {
        this.lock.execute(() -> {
            if (this.alreadyClosed) return;

            this.output.write(END);
            this.alreadyClosed = true;
            // Don't actually close the OutputStream.
        });
    }

    @Override
    public void write(int b) throws IOException {
        this.lock.execute(() -> {
            this.output.write('1');
            this.output.write(NEWLINE);
            this.output.write(b);
            this.output.write(NEWLINE);
        });
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.lock.execute(() -> {
            if (len == 0) return;
            String lenHex = Integer.toHexString(len);

            this.output.write(lenHex.getBytes(RHSConnection.CHARSET));
            this.output.write(NEWLINE);
            this.output.write(b, off, len);
            this.output.write(NEWLINE);
        });
    }

}
