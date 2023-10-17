package co.casterlabs.rakurai.io;

public class LittleEndianIOUtil {

    /* -------- */
    /* Long     */
    /* -------- */

    public static byte[] longToBytes(long v) {
        return new byte[] {
                (byte) (v >> 0),
                (byte) (v >> 8),
                (byte) (v >> 16),
                (byte) (v >> 24),
                (byte) (v >> 32),
                (byte) (v >> 40),
                (byte) (v >> 48),
                (byte) (v >> 56),
        };
    }

    public static long bytesToLong(byte[] b) {
        return (((long) b[7]) << 56) |
            ((b[6] & 0xFFl) << 48) |
            ((b[5] & 0xFFl) << 40) |
            ((b[4] & 0xFFl) << 32) |
            ((b[3] & 0xFFl) << 24) |
            ((b[2] & 0xFFl) << 16) |
            ((b[1] & 0xFFl) << 8) |
            ((b[0] & 0xFFl) << 0);
    }

    /* -------- */
    /* Int      */
    /* -------- */

    public static byte[] intToBytes(int v) {
        return new byte[] {
                (byte) (v >> 0),
                (byte) (v >> 8),
                (byte) (v >> 16),
                (byte) (v >> 24),
        };
    }

    public static int bytesToInt(byte[] b) {
        return ((b[3]) << 24) |
            ((b[2] & 0xFF) << 16) |
            ((b[1] & 0xFF) << 8) |
            ((b[0] & 0xFF) << 0);
    }

    /* -------- */
    /* Short    */
    /* -------- */

    public static byte[] shortToBytes(short v) {
        return new byte[] {
                (byte) (v >> 0),
                (byte) (v >> 8),
        };
    }

    public static short bytesToShort(byte[] b) {
        return (short) (((b[1]) << 8) |
            ((b[0] & 0xFF) << 0));
    }

}