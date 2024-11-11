package co.casterlabs.rhs.protocol.http;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.io.streams.StreamUtil;
import co.casterlabs.rakurai.CharStrings;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rhs.HttpStatus;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.DropConnectionException;
import co.casterlabs.rhs.util.MimeTypes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

public class HttpResponse {
    public static final byte[] EMPTY_BODY = new byte[0];

    /**
     * This response is used to signal to the server that we need to drop the
     * connection ASAP. (Assuming throwing {@link DropConnectionException} isn't
     * viable)
     */
    public static final HttpResponse NO_RESPONSE = HttpResponse.newFixedLengthResponse(StandardHttpStatus.NO_RESPONSE, EMPTY_BODY);
    public static final HttpResponse INTERNAL_ERROR = HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, EMPTY_BODY);

    Map<String, String> headers = new HashMap<>();
    ResponseContent content;
    HttpStatus status;

    public HttpResponse(@NonNull ResponseContent content, @NonNull HttpStatus status) {
        this.content = content;
        this.status = status;
    }

    /* ---------------- */
    /* Headers          */
    /* ---------------- */

    public HttpResponse mime(@Nullable String type) {
        if (type == null) type = "application/octet-stream";
        return this.header("Content-Type", type);
    }

    public HttpResponse header(@NonNull String key, @NonNull String value) {
        this.headers.put(key, value);
        return this;
    }

    public HttpResponse putAllHeaders(@NonNull Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /* ---------------- */
    /* Creating (Byte)  */
    /* ---------------- */

    public static HttpResponse newFixedLengthResponse(@NonNull HttpStatus status) {
        return newFixedLengthResponse(status, EMPTY_BODY);
    }

    public static HttpResponse newFixedLengthResponse(@NonNull HttpStatus status, @NonNull String body) {
        return newFixedLengthResponse(status, body.getBytes(StandardCharsets.UTF_8))
            .mime("text/plain; charset=utf-8");
    }

    public static HttpResponse newFixedLengthResponse(@NonNull HttpStatus status, @NonNull char[] body) {
        return newFixedLengthResponse(status, CharStrings.strbytes(body))
            .mime("text/plain; charset=utf-8");
    }

    public static HttpResponse newFixedLengthResponse(@NonNull HttpStatus status, @NonNull JsonElement json) {
        if ((json instanceof JsonObject) || (json instanceof JsonArray)) {
            byte[] body = json
                .toString(false)
                .getBytes(StandardCharsets.UTF_8);

            return newFixedLengthResponse(status, body)
                .mime("application/json; charset=utf-8");
        } else {
            throw new IllegalArgumentException("Json must be an Object or Array.");
        }
    }

    public static HttpResponse newFixedLengthResponse(@NonNull HttpStatus status, @NonNull byte[] body) {
        return new HttpResponse(new ByteResponse(body), status);
    }

    /* ---------------- */
    /* Creating (Stream) */
    /* ---------------- */

    public static HttpResponse newFixedLengthResponse(@NonNull HttpStatus status, @NonNull InputStream responseStream, long length) {
        return new HttpResponse(
            new StreamResponse(responseStream, length),
            status
        );
    }

    public static HttpResponse newChunkedResponse(@NonNull HttpStatus status, @NonNull InputStream responseStream) {
        return new HttpResponse(
            new StreamResponse(responseStream, -1),
            status
        );
    }

    /* ---------------- */
    /* Creating (File) */
    /* ---------------- */

    public static HttpResponse newFixedLengthFileResponse(@NonNull HttpStatus status, @NonNull File file) throws FileNotFoundException {
        String mime = MimeTypes.getMimeForFile(file);
        FileInputStream fin = new FileInputStream(file);
        return newFixedLengthResponse(status, fin, file.length())
            .mime(mime);
    }

    public static HttpResponse newFixedLengthFileResponse(@NonNull HttpStatus status, @NonNull File file, long skip, long length) throws FileNotFoundException, IOException {
        String mime = MimeTypes.getMimeForFile(file);
        FileInputStream fin = new FileInputStream(file);
        try {
            fin.skip(skip);
        } catch (IOException e) {
            fin.close();
            throw e;
        }
        return newFixedLengthResponse(status, fin, length)
            .mime(mime);
    }

    public static HttpResponse newRangedFileResponse(@NonNull HttpSession session, @NonNull HttpStatus status, @NonNull File file) throws FileNotFoundException, IOException {
        String etag = Integer.toHexString((file.getName() + file.lastModified() + file.length()).hashCode());

        HeaderValue range = session.headers().getSingle("Range");
        long fileLen = file.length();
        long startFrom = 0;
        long endAt = -1;

        if (range != null && range.raw().startsWith("bytes=")) {
            String byteRange = range.raw().substring("bytes=".length());
            int minusLocation = byteRange.indexOf('-');
            if (minusLocation > 0) {
                try {
                    startFrom = Long.parseLong(byteRange.substring(0, minusLocation));
                    if (byteRange.length() - minusLocation - 1 > 0) {
                        endAt = Long.parseLong(byteRange.substring(minusLocation + 1));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        HttpResponse response = null;

        if (range == null) {
            response = HttpResponse.newFixedLengthFileResponse(StandardHttpStatus.OK, file);
        } else {
            if (endAt < 0) endAt = fileLen - 1; // Range requests are 0-indexed, so subtract 1.
            long dataLen = endAt - startFrom + 1; // Add it back for the content length :D

            if (startFrom < 0 || startFrom >= fileLen || dataLen < 0) {
                response = HttpResponse.newFixedLengthResponse(StandardHttpStatus.RANGE_NOT_SATISFIABLE)
                    .header("Content-Range", String.format("bytes 0-0/%d", fileLen));
            } else {
                response = HttpResponse.newFixedLengthFileResponse(StandardHttpStatus.PARTIAL_CONTENT, file, startFrom, dataLen)
                    .header("Content-Range", String.format("bytes %d-%d/%d", startFrom, endAt, fileLen));
            }
        }

        response.header("ETag", etag);
        response.header("Content-Disposition", "filename=\"" + file.getName() + "\"");
        response.header("Accept-Ranges", "bytes");

        return response;
    }

    /* ---------------- */
    /* Responses        */
    /* ---------------- */

    public static interface ResponseContent extends Closeable {

        public void write(int recommendedBufferSize, OutputStream out) throws IOException;

        /**
         * @return any negative number for a chunked response.
         */
        public long length();

    }

    @Getter
    @AllArgsConstructor
    public static class StreamResponse implements ResponseContent {
        private InputStream response;
        private long length;

        @Override
        public void write(int recommendedBufferSize, OutputStream out) throws IOException {
            StreamUtil.streamTransfer(
                this.response,
                out,
                recommendedBufferSize,
                this.length
            );
        }

        @Override
        public long length() {
            return this.length;
        }

        @Override
        public void close() throws IOException {
            this.response.close();
            this.response = null; // Free, incase of leaks.
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ByteResponse implements ResponseContent {
        private byte[] response;

        @Override
        public void write(int recommendedBufferSize, OutputStream out) throws IOException {
            out.write(this.response);
        }

        @Override
        public long length() {
            return this.response.length;
        }

        @Override
        public void close() throws IOException {
            this.response = null; // Free, incase of leaks.
        }
    }

}
