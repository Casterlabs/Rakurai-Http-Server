package co.casterlabs.rhs.impl;

import java.io.IOException;
import java.io.OutputStream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class HttpChunkedOutputStream extends OutputStream {
    private final OutputStream out;

    private boolean alreadyClosed = false;

    @Override
    public void close() throws IOException {
        if (this.alreadyClosed) return;

        HttpProtocol.writeString("0\r\n\r\n", this.out);
        this.alreadyClosed = true;
        // Don't actually close the outputstream.
    }

    @Override
    public void write(int b) throws IOException {
        this.out.write('1');
        HttpProtocol.writeString("\r\n", this.out);
        this.out.write(b);
        HttpProtocol.writeString("\r\n", this.out);
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b.length == 0) return;

        HttpProtocol.writeString(Integer.toHexString(b.length), this.out);
        HttpProtocol.writeString("\r\n", this.out);
        this.out.write(b);
        HttpProtocol.writeString("\r\n", this.out);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0) return;

        HttpProtocol.writeString(Integer.toHexString(len), this.out);
        HttpProtocol.writeString("\r\n", this.out);
        this.out.write(b, off, len);
        HttpProtocol.writeString("\r\n", this.out);
    }

}
