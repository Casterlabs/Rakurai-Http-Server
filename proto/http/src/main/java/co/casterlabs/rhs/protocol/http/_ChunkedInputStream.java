package co.casterlabs.rhs.protocol.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import co.casterlabs.commons.async.Lock;
import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.exceptions.HttpException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class _ChunkedInputStream extends InputStream {
    private final Lock lock = new Lock();
    private final RHSConnection connection;

    private boolean isEndOfStream = false;
    private long currentChunkSize = 0;

    // Note that we only support a max chunk size of 0x80000000bb785000 (2^63).
    // Also note that chunk extensions will NOT be put in this buffer.
    private byte[] buffer = new byte[16];
    private int bufferWritePos = 0;
    private int bufferChunkSizePos = -1;

    public void skipRemaining() throws IOException {
        while (!this.isEndOfStream) {
            this.skip(Long.MAX_VALUE);
        }
    }

    private void startChunkReadIfNeeded() throws IOException {
        this.lock.execute(() -> {
            if ((this.currentChunkSize > 0) || this.isEndOfStream) return;

            // TODO Speed up using WorkBuffer

            // Reset.
            this.bufferWritePos = 0;
            this.bufferChunkSizePos = -1;

            while (true) {
                int readCharacter = this.connection.input.read();

                if (readCharacter == -1) {
                    throw new IOException("Reached end of stream before chunked body was fully read.");
                }

                // Don't handle the \r character. There'll be a \n next.
                if (readCharacter == '\r') {
                    continue;
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
                try { // Read the footer/trailers.
                    this.connection.readHeaders();
                } catch (HttpException e) {
                    throw new IOException(e);
                }
            }
        });
    }

    @Override
    public int read() throws IOException {
        return this.lock.execute(() -> {
            if (this.isEndOfStream) return -1;
            this.startChunkReadIfNeeded();

            try {
                this.currentChunkSize--;
                return this.connection.input.read();
            } catch (IOException e) {
                this.isEndOfStream = true;
                throw e;
            }
        });
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        return this.lock.execute(() -> {
            if (this.isEndOfStream) return -1;
            this.startChunkReadIfNeeded();

            // Clamp the read length to the amount actually available.
            int amountToRead = Math.min(len, (int) this.currentChunkSize);

            int read = this.connection.input.read(b, off, amountToRead);
            this.currentChunkSize -= read;
            return read;
        });
    }

    @Override
    public long skip(long n) throws IOException {
        return this.lock.execute(() -> {
            if (this.isEndOfStream) return 0L;
            this.startChunkReadIfNeeded();

            long skipped = this.connection.input.skip(n);
            this.currentChunkSize -= skipped;
            return skipped;
        });
    }

    @Override
    public int available() throws IOException {
        return this.lock.execute(() -> {
            if (this.isEndOfStream) return 0;
            this.startChunkReadIfNeeded();

            int chunkSize = this.currentChunkSize > Integer.MAX_VALUE ? // Clamp.
                Integer.MAX_VALUE : (int) this.currentChunkSize;
            int actuallyAvailable = this.connection.input.available();

            // Give them a truthful number :^)
            if (actuallyAvailable > chunkSize) {
                return chunkSize;
            } else {
                return actuallyAvailable;
            }
        });
    }

}
