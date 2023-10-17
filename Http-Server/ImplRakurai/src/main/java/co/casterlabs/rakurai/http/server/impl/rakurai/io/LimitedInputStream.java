package co.casterlabs.rakurai.http.server.impl.rakurai.io;

import java.io.IOException;
import java.io.InputStream;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LimitedInputStream extends InputStream {
    private final InputStream in;
    private long contentAvailable;

    @Override
    public void close() throws IOException {} // NOOP

    @Override
    public synchronized int read() throws IOException {
        if (this.contentAvailable == 0) {
            return -1;
        }
        this.contentAvailable--;
        return this.in.read();
    }

    @Override
    public synchronized int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        if (this.contentAvailable == 0) {
            return -1;
        }

        if (len > this.contentAvailable) { // Clamp.
            len = (int) this.contentAvailable;
        }

        int read = this.in.read(b, off, len);
        this.contentAvailable -= read;
        return read;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        if (this.contentAvailable == 0) {
            return -1;
        }

        if (n > this.contentAvailable) { // Clamp.
            n = this.contentAvailable;
        }

        long skipped = this.in.skip(n);
        this.contentAvailable -= skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        if (this.contentAvailable == 0) {
            return -1;
        }

        int available = this.in.available();
        if (available > this.contentAvailable) {
            return (int) this.contentAvailable;
        }

        return available;
    }

}
