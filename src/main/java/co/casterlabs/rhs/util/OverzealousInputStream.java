package co.casterlabs.rhs.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Basically we buffered too many bytes, so we need to give them back during a
 * read.
 */
public class OverzealousInputStream extends InputStream {
    private final InputStream underlying;

    private byte[] overage = {};
    private int overageEnd = 0;
    private int overageIndex = 0;

    public OverzealousInputStream(InputStream underlying) {
        this.underlying = underlying;
    }

    public void append(byte[] overage, int startAt, int endAt) {
        int previousRemaining = this.overageEnd - this.overageIndex;
        int newRemaining = endAt - startAt;

        byte[] newOverage = new byte[previousRemaining + newRemaining];
        System.arraycopy(this.overage, this.overageIndex, newOverage, 0, previousRemaining);
        System.arraycopy(overage, startAt, newOverage, previousRemaining, newRemaining);

        this.overage = newOverage;
        this.overageEnd = this.overage.length;
    }

    @Override
    public int read() throws IOException {
        if (this.overageIndex < this.overageEnd) {
            return this.overage[this.overageIndex++];
        }

        return this.underlying.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) return 0;

        if (this.overageIndex < this.overageEnd) {
            int amount = Math.min(this.overageEnd - this.overageIndex, len);
            System.arraycopy(this.overage, this.overageIndex, b, off, amount);
            this.overageIndex += amount;
            return amount;
        }

        return this.underlying.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
        if (this.overageIndex < this.overageEnd) {
            return this.overageEnd - this.overageIndex;
        }
        return this.underlying.available();
    }

}
