package co.casterlabs.rhs.protocol.http;

import java.io.IOException;
import java.io.OutputStream;

import co.casterlabs.rhs.protocol.RHSConnectionWriter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ChunkedOutputStream extends OutputStream {
    private final OutputStream out;

    private boolean alreadyClosed = false;

    @Override
    public void close() throws IOException {
        if (this.alreadyClosed) return;

        RHSConnectionWriter.writeString("0\r\n\r\n", this.out);
        this.alreadyClosed = true;
        // Don't actually close the OutputStream.
    }

    @Override
    public void write(int b) throws IOException {
        this.out.write('1');
        RHSConnectionWriter.writeString("\r\n", this.out);
        this.out.write(b);
        RHSConnectionWriter.writeString("\r\n", this.out);
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b.length == 0) return;

        RHSConnectionWriter.writeString(Integer.toHexString(b.length), this.out);
        RHSConnectionWriter.writeString("\r\n", this.out);
        this.out.write(b);
        RHSConnectionWriter.writeString("\r\n", this.out);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0) return;

        RHSConnectionWriter.writeString(Integer.toHexString(len), this.out);
        RHSConnectionWriter.writeString("\r\n", this.out);
        this.out.write(b, off, len);
        RHSConnectionWriter.writeString("\r\n", this.out);
    }

}
