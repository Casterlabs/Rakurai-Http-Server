package co.casterlabs.rhs.util;

public class WorkBuffer {
    public final byte[] raw;
    public int marker;
    public int limit = 0;

    public WorkBuffer(int bufferSize) {
        this.raw = new byte[bufferSize];
    }

    public void reset() {
        this.marker = 0;
        this.limit = 0;
    }

    public int remaining() {
        return this.limit - this.marker;
    }

    public int available() {
        return this.raw.length - this.limit;
    }

}
