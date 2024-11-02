package co.casterlabs.rhs.protocol;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.server.TLSVersion;
import co.casterlabs.rhs.util.HttpException;
import co.casterlabs.rhs.util.CaseInsensitiveMultiMap;
import co.casterlabs.rhs.util.SimpleUri;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class RHSConnectionReader {
    // @formatter:off
    private static final int MAX_METHOD_LENGTH = 512 /*b*/; // Also used for the http version.
    private static final int MAX_URI_LENGTH    = 256 /*kb*/ * 1024;
    private static final int MAX_HEADER_LENGTH =  16 /*kb*/ * 1024;
    // @formatter:on

    public static RHSConnection accept(
        FastLogger logger,
        BufferedInputStream input,
        OutputStream output,
        String remoteAddress,
        int serverPort,
        @Nullable TLSVersion tlsVersion
    ) throws IOException, HttpException {
        // Request line
        int[] $currentLinePosition = new int[1]; // int pointer :D
        int[] $endOfLinePosition = new int[1]; // int pointer :D
        byte[] requestLine = readRequestLine(input, $endOfLinePosition);

        String method = readMethod(requestLine, $currentLinePosition, $endOfLinePosition[0]);
        String uriPath = readURI(requestLine, $currentLinePosition, $endOfLinePosition[0]);
        HttpVersion version = readVersion(requestLine, $currentLinePosition, $endOfLinePosition[0]);

        // Headers
        CaseInsensitiveMultiMap headers = // HTTP/0.9 doesn't have headers.
            version == HttpVersion.HTTP_0_9 ? CaseInsensitiveMultiMap.EMPTY : readHeaders(input);

        SimpleUri uri = SimpleUri.from(headers.getSingleOrDefault("Host", ""), uriPath);

        return new RHSConnection(logger, input, output, remoteAddress, serverPort, method, uri, headers, version, tlsVersion);
    }

    public static byte[] readRequestLine(BufferedInputStream in, int[] $endOfLinePosition) throws IOException, HttpException {
        byte[] buffer = new byte[MAX_METHOD_LENGTH + MAX_URI_LENGTH + MAX_METHOD_LENGTH];
        int bufferWritePos = 0;
        while (true) {
            int readCharacter = in.read();

            if (readCharacter == -1) {
                if (bufferWritePos == 0) {
                    throw new IOException("Socket closed.");
                } else {
                    throw new IOException("Reached end of stream before request line was fully read.");
                }
            }

            // Convert the \r character to \n, dealing with the consequences if necessary.
            if (readCharacter == '\r') {
                readCharacter = '\n';

                // Peek at the next byte, if it's a \n then we need to consume it.
                in.mark(1);
                if (in.read() == '\n') {
                    in.reset();
                    in.skip(1);
                } else {
                    in.reset();
                }
            }

            if (readCharacter == '\n') {
                break; // End of method name, break!
            }

            buffer[bufferWritePos++] = (byte) (readCharacter & 0xff);
        }

        if (bufferWritePos == 0) {
            throw new HttpException(HttpStatus.adapt(400, "Request line was blank"));
        }

        $endOfLinePosition[0] = bufferWritePos; // Update the pointer.
        return buffer;
    }

    public static String readMethod(byte[] buffer, int[] $currentLinePosition, int endOfLinePosition) throws IOException, HttpException {
        final int startPos = $currentLinePosition[0];
        int bufferReadPos = startPos;
        int length = -1;
        while (true) {
            if (bufferReadPos == endOfLinePosition) {
                length = bufferReadPos - startPos - 1;
                break;
            }

            int readCharacter = buffer[bufferReadPos++];

            if (readCharacter == ' ') {
                length = bufferReadPos - startPos - 1;

                // Consume any trailing spaces.
                while (true) {
                    if (buffer[bufferReadPos] == ' ') {
                        bufferReadPos++;
                    } else {
                        break;
                    }
                }

                break; // End of method name, break!
            }
        }

        if (length <= 0) {
            // We will not send an ALLOW header.
            throw new HttpException(HttpStatus.adapt(405, "Method was blank"));
        }

        $currentLinePosition[0] = bufferReadPos; // Update the pointer.
        return new String(buffer, startPos, length, RHSConnection.CHARSET);
    }

    public static String readURI(byte[] buffer, int[] $currentLinePosition, int endOfLinePosition) throws IOException, HttpException {
        final int startPos = $currentLinePosition[0];
        int bufferReadPos = startPos;
        int length = -1;
        while (true) {
            if (bufferReadPos == endOfLinePosition) {
                length = bufferReadPos - startPos - 1;
                break;
            }

            int readCharacter = buffer[bufferReadPos++];

            if (readCharacter == ' ') {
                length = bufferReadPos - startPos - 1;

                // Consume any trailing spaces.
                while (true) {
                    if (buffer[bufferReadPos] == ' ') {
                        bufferReadPos++;
                    } else {
                        break;
                    }
                }

                break; // End of URI, break!
            }
        }

        if (length <= 0) {
            throw new HttpException(HttpStatus.adapt(404, "No URI specified"));
        }

        String uri = new String(buffer, startPos, length, RHSConnection.CHARSET);

        // Absolute URLs must be accepted but ignored.
        if (uri.startsWith("http://")) {
            uri = uri.substring(uri.indexOf('/', "http://".length()));
        } else if (uri.startsWith("https://")) {
            uri = uri.substring(uri.indexOf('/', "https://".length()));
        }

        $currentLinePosition[0] = bufferReadPos; // Update the pointer.
        return uri;
    }

    public static HttpVersion readVersion(byte[] buffer, int[] $currentLinePosition, int endOfLinePosition) throws IOException, HttpException {
        final int startPos = $currentLinePosition[0];
        String version = new String(buffer, startPos, endOfLinePosition - startPos, RHSConnection.CHARSET);

        try {
            return HttpVersion.fromString(version);
        } catch (IllegalArgumentException e) {
            throw new HttpException(HttpStatus.adapt(400, "Unsupported HTTP version"));
        }
    }

    public static CaseInsensitiveMultiMap readHeaders(BufferedInputStream in) throws IOException {
        CaseInsensitiveMultiMap.Builder headers = new CaseInsensitiveMultiMap.Builder();

        byte[] keyBuffer = new byte[MAX_HEADER_LENGTH];
        int keyBufferWritePos = 0;

        byte[] valueBuffer = new byte[MAX_HEADER_LENGTH];
        int valueBufferWritePos = 0;

        boolean isCurrentLineBlank = true;
        boolean isBuildingHeaderKey = true;
        while (true) {
            int readCharacter = in.read();

            if (readCharacter == -1) {
                throw new IOException("Reached end of stream before headers were fully read.");
            }

            // Convert the \r character to \n, dealing with the consequences if necessary.
            if (readCharacter == '\r') {
                readCharacter = '\n';

                // Peek at the next byte, if it's a \n then we need to consume it.
                in.mark(1);
                if (in.read() == '\n') {
                    in.reset();
                    in.skip(1);
                } else {
                    in.reset();
                }
            }

            if (readCharacter == '\n') {
                if (isCurrentLineBlank) {
                    break; // A blank line after headers marks the end, so we break out.
                }

                // A header line that is a whitespace is a continuation of the previous header
                // line. Example of what we're looking for:
                /* X-My-Header: some-value-1,\r\n  */
                /*              some-value-2\r\n   */
                try {
                    in.mark(1);
                    if (in.read() == ' ') {
                        continue; // Keep on readin'
                    }
                } finally {
                    in.reset();
                }

                // Alright, we're done with this header.
                String headerKey = convertBufferToTrimmedString(keyBuffer, keyBufferWritePos);
                String headerValue = convertBufferToTrimmedString(valueBuffer, valueBufferWritePos);
                headers.put(headerKey, headerValue);

                // Cleanup / Reset for the next header.
                isCurrentLineBlank = true;
                isBuildingHeaderKey = true;
                keyBufferWritePos = 0;
                valueBufferWritePos = 0;
                continue;
            }

            // Okay, line isn't blank. Let's buffer some data!
            isCurrentLineBlank = false;

            if (readCharacter == ':' && isBuildingHeaderKey) { // Note that colons are allowed in header values.
                // Time to switch over to building the value.
                isBuildingHeaderKey = false;
                continue;
            }

            byte b = (byte) (readCharacter & 0xff);

            if (isBuildingHeaderKey) {
                keyBuffer[keyBufferWritePos++] = b;
            } else {
                valueBuffer[valueBufferWritePos++] = b;
            }
        }

        return headers.build();
    }

    /* ---------------- */
    /* Helpers          */
    /* ---------------- */

    public static String convertBufferToTrimmedString(byte[] buffer, int bufferLength) {
        int startPos = 0;

        // Trim the leading.
        for (; startPos < bufferLength; startPos++) {
            byte ch = buffer[startPos];

            // Skip spaces.
            if (ch == ' ') {
                continue;
            }

            break;
        }

        int endPos = bufferLength;
        for (; endPos > 0; endPos--) {
            byte ch = buffer[endPos];

            // Skip spaces.
            if (ch == ' ') {
                continue;
            }

            break;
        }

        int length = endPos - startPos;
        return new String(buffer, startPos, length, RHSConnection.CHARSET);
    }

}
