package co.casterlabs.rhs.util.io;

import java.io.IOException;
import java.io.OutputStream;

import co.casterlabs.commons.async.Lock;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MTUOutputStream extends OutputStream {
    private final Lock lock = new Lock();

    private final OutputStream underlying;
    private final int mtu;

    @Override
    public void write(int b) throws IOException {
        this.lock.execute(() -> {
            this.underlying.write(b);
        });
    }

    @Override
    public void write(byte[] b, int _off, int _len) throws IOException {
        this.lock.execute(() -> {
            int len = _len; // Copy these.
            int off = _off;

            // We need to batch the write into chunks of the MTU size.
            while (len > 0) {
                int chunkSize = Math.min(len, this.mtu);
                this.underlying.write(b, off, chunkSize);
                len -= chunkSize;
                off += chunkSize;
            }
        });
    }

    @Override
    public void flush() throws IOException {
        this.underlying.flush();
    }

}
