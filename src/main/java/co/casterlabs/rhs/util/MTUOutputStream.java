package co.casterlabs.rhs.util;

import java.io.IOException;
import java.io.OutputStream;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MTUOutputStream extends OutputStream {
    private final OutputStream underlying;
    private final int mtu;

    @Override
    public void write(int b) throws IOException {
        this.underlying.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        // We need to batch the write into chunks of the MTU size.
        while (len > 0) {
            int chunkSize = Math.min(len, this.mtu);
            this.underlying.write(b, off, chunkSize);
            len -= chunkSize;
            off += chunkSize;
        }
    }

    @Override
    public void flush() throws IOException {
        this.underlying.flush();
    }

    @Override
    public void close() throws IOException {
        this.underlying.close();
    }

}
