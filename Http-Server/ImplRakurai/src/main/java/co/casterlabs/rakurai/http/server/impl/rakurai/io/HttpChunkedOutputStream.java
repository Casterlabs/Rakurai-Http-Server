package co.casterlabs.rakurai.http.server.impl.rakurai.io;

import java.io.IOException;
import java.io.OutputStream;

import co.casterlabs.rakurai.http.server.impl.rakurai.protocol.RHSHttpProtocol;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

@RequiredArgsConstructor
public class HttpChunkedOutputStream extends OutputStream {
    private final OutputStream out;
    private final FastLogger sessionLogger;

    private long bytesWritten = 0;
    private boolean alreadyClosed = false;

    @Override
    public void close() throws IOException {
        if (this.alreadyClosed) return;

        RHSHttpProtocol.writeString("0\r\n\r\n", this.out);
        this.alreadyClosed = true;

        this.sessionLogger.trace("Wrote approximately: %d bytes", this.bytesWritten);
        // Don't actually close the outputstream.
    }

    @Override
    public void write(int b) throws IOException {
        this.out.write('1');
        RHSHttpProtocol.writeString("\r\n", this.out);
        this.out.write(b);
        RHSHttpProtocol.writeString("\r\n", this.out);

        this.bytesWritten += 1;
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (b.length == 0) return;

        RHSHttpProtocol.writeString(Integer.toHexString(b.length), this.out);
        RHSHttpProtocol.writeString("\r\n", this.out);
        this.out.write(b);
        RHSHttpProtocol.writeString("\r\n", this.out);

        this.bytesWritten += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0) return;

        RHSHttpProtocol.writeString(Integer.toHexString(len), this.out);
        RHSHttpProtocol.writeString("\r\n", this.out);
        this.out.write(b, off, len);
        RHSHttpProtocol.writeString("\r\n", this.out);

        this.bytesWritten += len;
    }

}
