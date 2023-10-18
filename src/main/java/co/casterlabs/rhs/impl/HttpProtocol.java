package co.casterlabs.rhs.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.io.streams.LimitedInputStream;
import co.casterlabs.commons.io.streams.NonCloseableOutputStream;
import co.casterlabs.rhs.protocol.HttpStatus;
import co.casterlabs.rhs.protocol.HttpVersion;
import co.casterlabs.rhs.server.HttpResponse;
import co.casterlabs.rhs.server.HttpResponse.ResponseContent;
import co.casterlabs.rhs.session.HttpSession;
import co.casterlabs.rhs.session.TLSVersion;
import co.casterlabs.rhs.util.HeaderMap;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

abstract class HttpProtocol {
    public static final Charset HEADER_CHARSET = Charset.forName(System.getProperty("rakurai.http.headercharset", "ISO-8859-1"));
    public static final int HTTP_PERSISTENT_TIMEOUT = 30;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O");

    // @formatter:off
    private static final int MAX_METHOD_LENGTH = 512 /*b*/; // Also used for the http version.
    private static final int MAX_URI_LENGTH    = 256 /*kb*/ * 1024;
    private static final int MAX_HEADER_LENGTH =  16 /*kb*/ * 1024;
    // @formatter:on

    /* ---------------- */
    /* Request/Input    */
    /* ---------------- */

    public static HttpSessionImpl accept(FastLogger sessionLogger, RakuraiHttpServer server, Socket client, BufferedInputStream in) throws IOException, HttpException {
        // Request line
        int[] $currentLinePosition = new int[1]; // int pointer :D
        int[] $endOfLinePosition = new int[1]; // int pointer :D
        byte[] requestLine = readRequestLine(in, $endOfLinePosition);

        String method = readMethod(requestLine, $currentLinePosition, $endOfLinePosition[0]);
        String uri = readURI(requestLine, $currentLinePosition, $endOfLinePosition[0]);
        HttpVersion version = readVersion(requestLine, $currentLinePosition, $endOfLinePosition[0]);

        sessionLogger.trace("Request status line: %s", new String(requestLine, 0, $endOfLinePosition[0], HEADER_CHARSET));

        // Headers
        HeaderMap headers = // HTTP/0.9 doesn't have headers.
            version == HttpVersion.HTTP_0_9 ? //
                new HeaderMap.Builder().build() : readHeaders(in);

        // Retrieve the body, if any.
        InputStream bodyInput = null;
        switch (version) {
            case HTTP_1_1:
                // HTTP/1.1 handshaking header check.
                if (!headers.containsKey("Host")) {
                    throw new HttpException(HttpStatus.adapt(400, "Missing Host header"));
                }

                // Look for a chunked body, otherwise fall through to normal fixed-length
                // behavior (1.0).
                if ("chunked".equalsIgnoreCase(headers.getSingle("Transfer-Encoding"))) {
                    bodyInput = new HttpChunkedInputStream(in);
                    sessionLogger.debug("Detected chunked body.");
                    break;
                }

            case HTTP_1_0: {
                // If there's a Content-Length header then there's a body.
                String contentLength = headers.getSingle("Content-Length");
                if (contentLength != null) {
                    bodyInput = new LimitedInputStream(in, Long.parseLong(contentLength));
                    sessionLogger.debug("Detected fixed-length body.");
                }
                break;
            }

            default:
                break;
        }

        int indexOfQuery = uri.indexOf('?');
        String queryString = "";
        Map<String, List<String>> allQueryParameters = new HashMap<>();

        if (indexOfQuery != -1) {
            queryString = uri.substring(indexOfQuery);
            uri = uri.substring(0, indexOfQuery);
            parseAllQueryParameters(queryString, allQueryParameters);
        }

        // Copy the query parameters to a singleton map.
        // Also copy to another map, this time making the list unmodifiable.
        Map<String, List<String>> unmodQueryParameters = new HashMap<>();
        Map<String, String> queryParameters = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : allQueryParameters.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();

            unmodQueryParameters.put(key, Collections.unmodifiableList(values));

            if (!values.isEmpty()) {
                queryParameters.put(key, values.get(0));
            }
        }

        TLSVersion tlsVersion = null;
        if (client instanceof SSLSocket) {
            SSLSession ssl = ((SSLSocket) client).getSession();
            tlsVersion = TLSVersion.parse(ssl.getProtocol());
        }

        return new HttpSessionImpl(
            headers,
            uri,
            queryString,
            Collections.unmodifiableMap(unmodQueryParameters),
            Collections.unmodifiableMap(queryParameters),
            server.getPort(),
            tlsVersion,
            version,
            method,
            client.getInetAddress().getHostAddress(),
            bodyInput
        ).rhsPostConstruct(server.getConfig(), sessionLogger);
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
        return new String(buffer, startPos, length, HEADER_CHARSET);
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

        String uri = new String(buffer, startPos, length, HEADER_CHARSET);

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
        String version = new String(buffer, startPos, endOfLinePosition - startPos, HEADER_CHARSET);

        try {
            return HttpVersion.fromString(version);
        } catch (IllegalArgumentException e) {
            throw new HttpException(HttpStatus.adapt(400, "Unsupported HTTP version"));
        }
    }

    public static HeaderMap readHeaders(BufferedInputStream in) throws IOException {
        HeaderMap.Builder headers = new HeaderMap.Builder();

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
    /* Response/Output  */
    /* ---------------- */

    private static enum ResponseMode {
        CLOSE_ON_COMPLETE,
        FIXED_LENGTH,
        CHUNKED
    }

    public static void writeOutResponse(Socket client, HttpSession session, boolean keepConnectionAlive, HttpResponse response) throws IOException {
        OutputStream out = client.getOutputStream();

        // Write out status and headers.
        String contentEncoding = null;
        ResponseMode responseMode = null;

        // 0.9 doesn't have a status line or anything, so we don't write it out.
        if (session.getVersion().value >= 1.0) {
            session.getLogger().trace("Response status line: %s %s", session.getVersion(), response.getStatus().getStatusString());

            // Write status.
            HttpProtocol.writeString(session.getVersion().toString(), out);
            HttpProtocol.writeString(" ", out);
            HttpProtocol.writeString(response.getStatus().getStatusString(), out);
            HttpProtocol.writeString("\r\n", out);

            if (!response.hasHeader("Content-Type")) {
                response.putHeader("Content-Type", "application/octet-stream");
            }

            long length = response.getContent().getLength();

            if (session.getVersion() == HttpVersion.HTTP_1_0) {
                if (length == -1) {
                    responseMode = ResponseMode.CLOSE_ON_COMPLETE;
                } else {
                    responseMode = ResponseMode.FIXED_LENGTH;
                }
            } else {
                contentEncoding = pickEncoding(session, response);

                if (length == -1 || contentEncoding != null) {
                    // We always chunk compressed responses. This is to avoid some jank in our code
                    // :P
                    response.putHeader("Transfer-Encoding", "chunked");
                    responseMode = ResponseMode.CHUNKED;
                } else {
                    responseMode = ResponseMode.FIXED_LENGTH;
                    response.putHeader("Content-Length", String.valueOf(length));
                }

                if (keepConnectionAlive) {
                    // Add the keepalive headers.
                    response.putHeader("Connection", "keep-alive");
                    response.putHeader("Keep-Alive", "timeout=" + HTTP_PERSISTENT_TIMEOUT);
                } else {
                    // Let the client know that we will be closing the socket.
                    response.putHeader("Connection", "close");
                }

                // Write out a Date header for HTTP/1.1 requests with a non-100 status code.
                if ((session.getVersion().value >= 1.1) && (response.getStatus().getStatusCode() >= 200)) {
                    response.putHeader("Date", HttpProtocol.getHttpTime());
                }

                if (contentEncoding != null) {
                    response.putHeader("Content-Encoding", contentEncoding);
                    response.putHeader("Vary", "Accept-Encoding");
                }
            }

            session.getLogger().debug("Response headers: %s", response.getAllHeaders());

            // Write headers.
            for (Map.Entry<String, String> entry : response.getAllHeaders().entrySet()) {
                HttpProtocol.writeString(entry.getKey(), out);
                HttpProtocol.writeString(": ", out);
                HttpProtocol.writeString(entry.getValue(), out);
                HttpProtocol.writeString("\r\n", out);
            }

            // Write the separation line.
            HttpProtocol.writeString("\r\n", out);
        }

        try {
            if (responseMode == ResponseMode.CHUNKED) {
                out = new HttpChunkedOutputStream(out);
            } else {
                out = new NonCloseableOutputStream(out);
            }

            // Write out the response, defaulting to non-encoded responses.
            writeWithEncoding(contentEncoding, out, response.getContent());
        } finally {
            // Chunked output streams have special close implementations that don't actually
            // close the underlying connection, they just signal that this is the end of the
            // request.
            out.close();
        }
    }

    /* ---------------- */
    /* Response Encoding */
    /* ---------------- */

    private static boolean shouldCompress(@Nullable String mimeType) {
        if (mimeType == null) return false;

        // Source: https://cdn.jsdelivr.net/gh/jshttp/mime-db@master/db.json

        // Literal text.
        if (mimeType.startsWith("text/")) return true;
        if (mimeType.endsWith("+text")) return true;

        // Compressible data types.
        if (mimeType.endsWith("json")) return true;
        if (mimeType.endsWith("xml")) return true;
        if (mimeType.endsWith("csv")) return true;

        // Other.
        if (mimeType.equals("application/javascript") || mimeType.equals("application/x-javascript")) return true;
        if (mimeType.equals("image/bmp")) return true;
        if (mimeType.equals("image/vnd.adobe.photoshop")) return true;
        if (mimeType.equals("image/vnd.microsoft.icon") || mimeType.equals("image/x-icon")) return true;
        if (mimeType.equals("application/tar") || mimeType.equals("application/x-tar")) return true;
        if (mimeType.equals("application/wasm")) return true;

        return false;
    }

    private static List<String> getAcceptedEncodings(HttpSession session) {
        List<String> accepted = new LinkedList<>();

        for (String value : session.getHeaders().getOrDefault("Accept-Encoding", Collections.emptyList())) {
            String[] split = value.split(", ");
            for (String encoding : split) {
                accepted.add(encoding.toLowerCase());
            }
        }

        return accepted;
    }

    private static String pickEncoding(HttpSession session, HttpResponse response) {
        if (session.getVersion().value <= 1.0) {
            return null;
        }

        if (!shouldCompress(response.getAllHeaders().get("Content-Type"))) {
            session.getLogger().debug("Format does not appear to be compressible, sending without encoding.");
        }

        List<String> acceptedEncodings = getAcceptedEncodings(session);

        // Order of our preference.
        if (acceptedEncodings.contains("gzip")) {
            session.getLogger().debug("Client supports GZip encoding, using that.");
            return "gzip";
        } else if (acceptedEncodings.contains("deflate")) {
            session.getLogger().debug("Client supports Deflate encoding, using that.");
            return "deflate";
        }
        // Brotli looks to be difficult. Not going to be supported for a while.

        return null;
    }

    public static void writeWithEncoding(@Nullable String encoding, OutputStream out, ResponseContent content) throws IOException {
        if (encoding == null) {
            encoding = ""; // Switch doesn't support nulls :/
        }

        switch (encoding) {
            case "gzip": {
                GZIPOutputStream enc = new GZIPOutputStream(out);
                content.write(enc);
                enc.finish(); // Do not close.
                break;
            }

            case "deflate": {
                DeflaterOutputStream enc = new DeflaterOutputStream(out);
                content.write(enc);
                enc.finish(); // Do not close.
                break;
            }

            default:
                content.write(out);
                break;
        }
    }

    /* ---------------- */
    /* Helpers          */
    /* ---------------- */

    public static void writeString(String str, OutputStream out) throws IOException {
        out.write(str.getBytes(HttpProtocol.HEADER_CHARSET));
    }

    public static String getHttpTime() {
        return TIME_FORMATTER.format(ZonedDateTime.now(ZoneOffset.UTC));
    }

    private static String convertBufferToTrimmedString(byte[] buffer, int bufferLength) {
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
        return new String(buffer, startPos, length, HEADER_CHARSET);
    }

    private static void parseAllQueryParameters(String queryString, Map<String, List<String>> allQueryParameters) {
        // Magic.
        Arrays
            .stream(queryString.substring(1).split("&"))
            .map((it) -> {
                try {
                    int eqIdx = it.indexOf("=");

                    if (eqIdx == -1) {
                        return new SimpleImmutableEntry<>(
                            URLDecoder.decode(it, "UTF-8"),
                            null
                        );
                    }

                    String key = it.substring(0, eqIdx);
                    String value = it.substring(eqIdx + 1);

                    return new SimpleImmutableEntry<>(
                        URLDecoder.decode(key, "UTF-8"),
                        URLDecoder.decode(value, "UTF-8")
                    );
                } catch (UnsupportedEncodingException ignored) {
                    return null;
                }
            })
            .collect(
                Collectors.groupingBy(
                    SimpleImmutableEntry::getKey,
                    HashMap::new,
                    Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                )
            )
            .forEach((key, values) -> {
                @SuppressWarnings("unchecked")
                List<String> actualValues = (List<String>) values
                    .parallelStream()
                    .filter((v) -> v != null)
                    .collect(Collectors.toList());

                allQueryParameters.put(key, actualValues);
            });
    }

}
