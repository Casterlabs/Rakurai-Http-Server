package co.casterlabs.rhs.protocol;

import java.io.IOException;
import java.io.InputStream;

import co.casterlabs.commons.io.streams.OverzealousInputStream;
import co.casterlabs.rhs.HttpStatus;
import co.casterlabs.rhs.HttpVersion;
import co.casterlabs.rhs.protocol.http.HeaderValue;
import co.casterlabs.rhs.util.CaseInsensitiveMultiMap;
import co.casterlabs.rhs.util.WorkBuffer;
import lombok.AllArgsConstructor;

class _ConnectionUtil {
    private static final int MAX_REQUEST_LINE_LENGTH = 16 /*kb*/ * 1024;
    private static final int MAX_HEADER_LENGTH = 16 /*kb*/ * 1024;

    /* ---------------- */
    /* Types            */
    /* ---------------- */

    @AllArgsConstructor
    public static class RequestLineInfo {
        public final String method;
        public final String uriPath;
        public final HttpVersion httpVersion;
    }

    /* ---------------- */
    /* Data             */
    /* ---------------- */

    static RequestLineInfo readRequestLine(OverzealousInputStream input, int guessedMtu) throws IOException, HttpException {
        WorkBuffer buffer = new WorkBuffer(MAX_REQUEST_LINE_LENGTH);

        // Request line
        int requestLineEnd = _ConnectionUtil.readLine(input, buffer, guessedMtu);

        String method = _ConnectionUtil.readStringUntil(buffer, requestLineEnd, ' ');
        buffer.marker++; // Consume the ' '
        if (method.length() == 0) {
            // We will not send an ALLOW header.
            throw new HttpException(HttpStatus.adapt(405, "Method was blank"));
        }

        String uriPath = _ConnectionUtil.readStringUntil(buffer, requestLineEnd, ' ');
        buffer.marker++; // Consume the ' '
        if (uriPath.length() <= 0) {
            throw new HttpException(HttpStatus.adapt(404, "No URI specified"));
        }

        // Absolute URLs must be accepted but ignored.
        if (uriPath.startsWith("http://")) {
            uriPath = uriPath.substring(uriPath.indexOf('/', "http://".length()));
        } else if (uriPath.startsWith("https://")) {
            uriPath = uriPath.substring(uriPath.indexOf('/', "https://".length()));
        }

        HttpVersion version;
        try {
            version = HttpVersion.fromString(_ConnectionUtil.readStringUntil(buffer, requestLineEnd, ' '));
            buffer.marker++; // Consume the ' '
        } catch (IllegalArgumentException e) {
            throw new HttpException(HttpStatus.adapt(400, "Unsupported HTTP version"));
        }

        input.append(buffer.raw, buffer.marker, buffer.limit);

        // Discard 1 bytes to consume the \n at the \n at the end of the request line
        // (note that readStringUtil has already consumed the \r).
        input.read();

        return new RequestLineInfo(method, uriPath, version);
    }

    static CaseInsensitiveMultiMap<HeaderValue> readHeaders(OverzealousInputStream input, int guessedMtu) throws IOException, HttpException {
        CaseInsensitiveMultiMap.Builder<HeaderValue> headers = new CaseInsensitiveMultiMap.Builder<>();
        WorkBuffer buffer = new WorkBuffer(MAX_HEADER_LENGTH);

        String currentKey = null;
        String currentValue = null;

        while (true) {
            int lineEnd = readLine(input, buffer, guessedMtu);

            if (lineEnd - buffer.marker == 0) {
                // End of headers
                if (currentKey != null) {
                    headers.put(currentKey.trim(), new HeaderValue(currentValue.trim()));
                }

                break;
            }

            // A header line that starts with a whitespace or tab is a continuation of the
            // previous header line. Example of what we're looking for:
            /* X-My-Header: some-value-1,\r\n  */
            /*              some-value-2\r\n   */
            if (currentKey != null) {
                if (buffer.raw[buffer.marker] == ' ' || buffer.raw[buffer.marker] == '\t') {
                    currentValue += readStringUntil(buffer, lineEnd, '\r');
                }
                headers.put(currentKey.trim(), new HeaderValue(currentValue.trim()));
            }

            currentKey = readStringUntil(buffer, lineEnd, ':');
            buffer.marker++; // Consume the ':'

            if (currentKey.length() == 0) {
                throw new HttpException(HttpStatus.adapt(400, "Header key was blank"));
            }

            currentValue = readStringUntil(buffer, lineEnd, '\r');
            buffer.marker += 2; // +2 to consume \r\n.

            if (currentValue.length() == 0) {
                throw new HttpException(HttpStatus.adapt(400, "Header value was blank"));
            }
        }

        // Discard 2 bytes to consume the \r\n at the end of the header block
        buffer.marker += 2;

        input.append(buffer.raw, buffer.marker, buffer.limit);

        return headers.build();
    }

    /* ---------------- */
    /* Helpers          */
    /* ---------------- */

    private static int readLine(InputStream in, WorkBuffer buffer, int guessedMtu) throws IOException, HttpException {
        while (true) {
            for (int bufferIndex = buffer.marker; bufferIndex < buffer.limit; bufferIndex++) {
                if (buffer.raw[bufferIndex] == '\r' && buffer.raw[bufferIndex + 1] == '\n') {
                    return bufferIndex; // End of line, break!
                }
            }

            buffer.marker = buffer.limit;

            if (buffer.available() == 0) {
                throw new HttpException(HttpStatus.adapt(400, "Request line or header line too long"));
            }

            int amountToRead = Math.min(
                Math.max(guessedMtu, in.available()), // We might already have > mtu waiting on the wire
                buffer.available() // Limit by available space.
            );

            int read = in.read(buffer.raw, buffer.limit, amountToRead);
            if (read == -1) {
                throw new IOException("Reached end of stream before line was fully read.");
            } else {
                buffer.limit += read;
            }
        }
    }

    private static String readStringUntil(WorkBuffer buffer, int limit, char target) throws IOException, HttpException {
        int start = buffer.marker;
        int end = start;
        for (; end < limit; end++) {
            int readCharacter = buffer.raw[end];

            if (readCharacter == target) {
                break; // End of string, break!
            }
        }

        buffer.marker = end; // +1 to consume the target.
        return new String(buffer.raw, start, end - start, RHSConnection.CHARSET);
    }

}
