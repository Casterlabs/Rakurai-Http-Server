package co.casterlabs.rakurai.http.server.impl.rakurai.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import co.casterlabs.rakurai.http.server.impl.rakurai.protocol.RHSHttpProtocol;

public class HttpChunkedInputStream extends InputStream {
    private BufferedInputStream in;
    private boolean isEndOfStream = false;
    private long currentChunkSize = 0;

    // Note that we only support a max chunk size of 0x80000000bb785000 (2^63).
    // Also note that chunk extensions will NOT be put in this buffer.
    private byte[] buffer = new byte[16];
    private int bufferWritePos = 0;
    private int bufferChunkSizePos = -1;

    public HttpChunkedInputStream(BufferedInputStream in) {
        this.in = in;
    }

    public void skipRemaining() throws IOException {
        while (!this.isEndOfStream) {
            this.skip(Long.MAX_VALUE);
        }
    }

    private synchronized void startChunkReadIfNeeded() throws IOException {
        if ((this.currentChunkSize > 0) || this.isEndOfStream) return;

        // Reset.
        this.bufferWritePos = 0;
        this.bufferChunkSizePos = -1;

        while (true) {
            int readCharacter = this.in.read();

            if (readCharacter == -1) {
                throw new IOException("Reached end of stream before chunked body was fully read.");
            }

            // Convert the \r character to \n, dealing with the consequences if necessary.
            if (readCharacter == '\r') {
                readCharacter = '\n';

                // Peek at the next byte, if it's a \n then we need to consume it.
                this.in.mark(1);
                if (this.in.read() == '\n') {
                    this.in.reset();
                    this.in.skip(1);
                } else {
                    this.in.reset();
                }
            }

            // You can include "extensions" at the end of chunk sizes. We gotta ignore them
            // somehow. We also need to ensure that we don't overwrite the chunkSizePos by
            // accident if there's a ':' in the extension.
            if ((readCharacter == ';') && (this.bufferChunkSizePos == -1)) {
                this.bufferChunkSizePos = this.bufferWritePos;
            }

            if (readCharacter == '\n') {
                if (this.bufferWritePos == 0) continue; // We're not done, this is just a newline.

                if (this.bufferChunkSizePos == -1) {
                    // See the above comment.
                    this.bufferChunkSizePos = this.bufferWritePos;
                }

                break; // End length declaration, break!
            }

            // Avoid writing extensions into the buffer (discard them instead).
            if (this.bufferChunkSizePos == -1) {
                this.buffer[this.bufferWritePos++] = (byte) (readCharacter & 0xff);
            }
        }

        String chunkSizeInHex = new String(this.buffer, 0, this.bufferChunkSizePos, StandardCharsets.ISO_8859_1);
        this.currentChunkSize = Long.parseLong(chunkSizeInHex, 16);

        // End of stream.
        if (this.currentChunkSize == 0) {
            this.isEndOfStream = true;
            RHSHttpProtocol.readHeaders(this.in); // Read the footer/trailers.
        }
    }

    @Override
    public synchronized int read() throws IOException {
        this.startChunkReadIfNeeded();
        if (this.isEndOfStream) return -1;

        try {
            this.currentChunkSize--;
            return this.in.read();
        } catch (IOException e) {
            this.isEndOfStream = true;
            throw e;
        }
    }

    @Override
    public synchronized int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public synchronized int read(byte b[], int off, int len) throws IOException {
        this.startChunkReadIfNeeded();
        if (this.isEndOfStream) return -1;

        // Clamp the read length to the amount actually available.
        if (len > this.currentChunkSize) {
            len = (int) this.currentChunkSize;
        }

        int read = this.in.read(b, off, len);
        this.currentChunkSize -= read;
        return read;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        this.startChunkReadIfNeeded();
        if (this.isEndOfStream) return -1;

        long skipped = this.in.skip(n);
        this.currentChunkSize -= skipped;
        return skipped;
    }

    @Override
    public synchronized int available() throws IOException {
        this.startChunkReadIfNeeded();
        if (this.isEndOfStream) return -1;

        int chunkSize = this.currentChunkSize > Integer.MAX_VALUE ? // Clamp.
            Integer.MAX_VALUE : (int) this.currentChunkSize;
        int actuallyAvailable = this.in.available();

        // Give them a truthful number :^)
        if (actuallyAvailable > chunkSize) {
            return chunkSize;
        } else {
            return actuallyAvailable;
        }
    }

}
