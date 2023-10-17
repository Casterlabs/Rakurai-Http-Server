package co.casterlabs.rakurai.io;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.Nullable;

import lombok.NonNull;

public class IOUtil {
    public static int DEFAULT_BUFFER_SIZE = Integer.parseInt(System.getProperty("rakuraidefaultbuffersize", "16384")); // Default to 16k

    /**
     * @param closeables The closeables to... close...
     * 
     *                   Closes the given closeables, swallowing any errors.
     */
    public static void safeClose(@Nullable Closeable... closeables) {
        if (closeables == null) return;

        for (Closeable closeable : closeables) {
            if (closeable == null) continue;

            try {
                closeable.close();
            } catch (Throwable ignored) {}
        }
    }

    /**
     * @param    source      The data source.
     * @param    dest        The target destination.
     * 
     * @throws   IOException If an IO error occurs.
     * 
     *                       Allows you to quickly write the source to the dest
     *                       using the default buffer size (See:
     *                       {@link #DEFAULT_BUFFER_SIZE}).
     * 
     * @see                  #DEFAULT_BUFFER_SIZE
     * 
     * @implNote             The streams are NOT automatically closed for you, you
     *                       will need to close them yourself (e.g using a
     *                       try-with-resources block).
     */
    public static void writeInputStreamToOutputStream(@NonNull InputStream source, @NonNull OutputStream dest) throws IOException {
        writeInputStreamToOutputStream(source, dest, DEFAULT_BUFFER_SIZE);
    }

    /**
     * @param    source        The data source.
     * @param    dest          The target destination.
     * @param    length        The expected length of source, can be negative. Note
     *                         that this value will constrain the amount of bytes
     *                         read from the source! (Use any negative value for
     *                         unconstrained reading)
     * @param    maxBufferSize The maximum IO buffer size, constrain this to a
     *                         reasonable number.
     * 
     * @throws   IOException   If an IO error occurs.
     * 
     *                         This is designed to be an efficient IO transfer
     *                         allowing for fast block reads for high-bandwidth
     *                         inputs while still constraining memory usage for the
     *                         transfer buffer.
     * 
     * @implNote               The streams are NOT automatically closed for you, you
     *                         will need to close them yourself (e.g using a
     *                         try-with-resources block).
     */
    public static void writeInputStreamToOutputStream(@NonNull InputStream source, @NonNull OutputStream dest, long length, int maxBufferSize) throws IOException {
        if (length < 0) {
            // Don't constrain.
            writeInputStreamToOutputStream(source, dest, maxBufferSize);
            return;
        }

        int bufferSize = length > maxBufferSize ? //
            maxBufferSize : // Constrain to maxBufferSize.
            (int) length;// Source length is of suitable size, use it.

        byte[] buffer = new byte[bufferSize];
        long remaining = length;
        int read = 0;

        while ((read = source.read(buffer)) != -1) {
            if (read >= remaining) {
                dest.write(buffer, 0, (int) remaining);
                dest.flush();
                break; // We're done!
            }

            remaining -= read;
            dest.write(buffer, 0, read);
            dest.flush();
        }
    }

    /**
     * @param    source      The data source.
     * @param    dest        The target destination.
     * @param    bufferSize  The read buffer size.
     * 
     * @throws   IOException If an IO error occurs.
     * 
     *                       Allows you to quickly write the source to the dest
     *                       using the specified buffer size.
     * 
     * @implNote             The streams are NOT automatically closed for you, you
     *                       will need to close them yourself (e.g using a
     *                       try-with-resources block).
     * 
     * @implNote             The destination is automatically flush()'d upon
     *                       success.
     */
    public static void writeInputStreamToOutputStream(@NonNull InputStream source, @NonNull OutputStream dest, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int read = 0;

        while ((read = source.read(buffer)) != -1) {
            dest.write(buffer, 0, read);
        }

        dest.flush();
    }

    /**
     * @param    source The data source.
     * 
     * @return          The data in a byte array.
     * 
     *                  Reads an input stream into an array and then hands you the
     *                  result. The IO buffer uses the default buffer size (See:
     *                  {@link #DEFAULT_BUFFER_SIZE}).
     * 
     * @see             #DEFAULT_BUFFER_SIZE
     * 
     * 
     * @implNote        The stream is NOT automatically closed for you, you will
     *                  need to close them yourself (e.g using a try-with-resources
     *                  block).
     */
    public static byte[] readInputStreamBytes(@NonNull InputStream source) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeInputStreamToOutputStream(source, out);
        return out.toByteArray();
    }

    /**
     * @param    source The data source.
     * 
     * @return          The data as a UTF8 String.
     * 
     *                  Reads an input stream into an String and then hands you the
     *                  result. The IO buffer uses the default buffer size (See:
     *                  {@link #DEFAULT_BUFFER_SIZE}).
     * 
     * @see             #DEFAULT_BUFFER_SIZE
     * @see             #readInputStreamString(InputStream, Charset)
     * 
     * 
     * @implNote        The stream is NOT automatically closed for you, you will
     *                  need to close them yourself (e.g using a try-with-resources
     *                  block).
     * 
     * @implNote        This method uses UTF8 as the charset, see
     *                  {@link #readInputStreamString(InputStream, Charset)} if you
     *                  need to use a different charset.
     */
    public static String readString(@NonNull InputStream source) throws IOException {
        return readInputStreamString(source, StandardCharsets.UTF_8);
    }

    /**
     * @param    source  The data source.
     * @param    charset The charset to use.
     * 
     * @return           The data as a String.
     * 
     *                   Reads an input stream into an String and then hands you the
     *                   result. The IO buffer uses the default buffer size (See:
     *                   {@link #DEFAULT_BUFFER_SIZE}).
     * 
     * @see              #DEFAULT_BUFFER_SIZE
     * 
     * 
     * @implNote         The stream is NOT automatically closed for you, you will
     *                   need to close them yourself (e.g using a try-with-resources
     *                   block).
     */
    public static String readInputStreamString(@NonNull InputStream source, @NonNull Charset sourceCharset) throws IOException {
        byte[] bytes = readInputStreamBytes(source);
        return new String(bytes, sourceCharset);
    }

}
