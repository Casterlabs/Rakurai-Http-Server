package co.casterlabs.rhs.protocol.http;

import java.io.IOException;
import java.io.OutputStream;

import co.casterlabs.rhs.protocol.RHSConnection;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ChunkedOutputStream extends OutputStream {
    private final RHSConnection connection;

    private boolean alreadyClosed = false;

    @Override
    public void close() throws IOException {
        if (this.alreadyClosed) return;

        this.connection.writeString("0\r\n\r\n");
        this.alreadyClosed = true;
        // Don't actually close the OutputStream.
    }

    @Override
    public void write(int b) throws IOException {
        this.connection.output.write('1');
        this.connection.writeString("\r\n");
        this.connection.output.write(b);
        this.connection.writeString("\r\n");
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b.length == 0) return;

        this.connection.writeString(Integer.toHexString(b.length));
        this.connection.writeString("\r\n");
        this.connection.output.write(b);
        this.connection.writeString("\r\n");
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0) return;

        this.connection.writeString(Integer.toHexString(len));
        this.connection.writeString("\r\n");
        this.connection.output.write(b, off, len);
        this.connection.writeString("\r\n");
    }

}
