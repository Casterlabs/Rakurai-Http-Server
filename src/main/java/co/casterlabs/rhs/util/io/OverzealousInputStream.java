package co.casterlabs.rhs.util.io;

import java.io.IOException;
import java.io.InputStream;

import co.casterlabs.commons.async.Lock;

/**
 * Basically we buffered too many bytes, so we need to give them back during a
 * read.
 */
public class OverzealousInputStream extends InputStream {
    private final Lock lock = new Lock();

    private final InputStream underlying;

    private byte[] overage = new byte[1024];
    private int overageEnd = 0;
    private int overageIndex = 0;

    public OverzealousInputStream(InputStream underlying) {
        this.underlying = underlying;
    }

    private void ensureCapacity(int additionalSize) {
        int requiredCapacity = this.overageEnd - this.overageIndex + additionalSize;
        if (requiredCapacity > this.overage.length) {
            int newCapacity = Math.max(this.overage.length * 2, requiredCapacity);
            byte[] newBuffer = new byte[newCapacity];
            System.arraycopy(this.overage, this.overageIndex, newBuffer, 0, this.overageEnd - this.overageIndex);
            this.overageEnd -= this.overageIndex;
            this.overageIndex = 0;
            this.overage = newBuffer;
        } else if (this.overageIndex > 0) {
            // Shift data if there’s unused space at the beginning
            System.arraycopy(this.overage, this.overageIndex, this.overage, 0, this.overageEnd - this.overageIndex);
            this.overageEnd -= this.overageIndex;
            this.overageIndex = 0;
        }
    }

    public void append(byte[] data, int startAt, int endAt) {
        this.lock.execute(() -> {
            int newRemaining = endAt - startAt;
            ensureCapacity(newRemaining);

            // Copy new data to the buffer
            System.arraycopy(data, startAt, this.overage, this.overageEnd, newRemaining);
            this.overageEnd += newRemaining;
        });
    }

    @Override
    public int read() throws IOException {
        return this.lock.execute(() -> {
            if (this.overageIndex < this.overageEnd) {
                return (int) this.overage[this.overageIndex++];
            }

            return this.underlying.read();
        });
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) return 0;

        return this.lock.execute(() -> {
            if (this.overageIndex < this.overageEnd) {
                int amount = Math.min(this.overageEnd - this.overageIndex, len);
                System.arraycopy(this.overage, this.overageIndex, b, off, amount);
                this.overageIndex += amount;
                return amount;
            }

            return this.underlying.read(b, off, len);
        });
    }

    @Override
    public int available() throws IOException {
        return this.lock.execute(() -> {
            if (this.overageIndex < this.overageEnd) {
                return this.overageEnd - this.overageIndex;
            }
            return this.underlying.available();
        });
    }

}
