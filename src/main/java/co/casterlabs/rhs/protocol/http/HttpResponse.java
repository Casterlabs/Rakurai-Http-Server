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
import co.casterlabs.rhs.util.DropConnectionException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

public class HttpResponse {
    /**
     * This response is used to signal to the server that we need to drop the
     * connection ASAP. (Assuming throwing {@link DropConnectionException} isn't
     * viable)
     */
    public static final HttpResponse NO_RESPONSE = HttpResponse.newFixedLengthResponse(StandardHttpStatus.NO_RESPONSE, new byte[0]);
    public static final HttpResponse INTERNAL_ERROR = HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, new byte[0]);
    public static final byte[] EMPTY_BODY = new byte[0];

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
        return newFixedLengthResponse(status, CharStrings.strbytes(body));
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
        HttpResponse response = new HttpResponse(
            new ByteResponse(body),
            status
        );

        return response;
    }

    /* ---------------- */
    /* Response (Stream) */
    /* ---------------- */

    public static HttpResponse newFixedLengthResponse(@NonNull HttpStatus status, @NonNull InputStream responseStream, long length) {
        HttpResponse response = new HttpResponse(
            new StreamResponse(responseStream, length),
            status
        );

        return response;
    }

    public static HttpResponse newFixedLengthFileResponse(@NonNull HttpStatus status, @NonNull File file) throws FileNotFoundException {
        FileInputStream fin = new FileInputStream(file);

        return newFixedLengthResponse(status, fin, file.length());
    }

    public static HttpResponse newFixedLengthFileResponse(@NonNull HttpStatus status, @NonNull File file, long skip, long length) throws FileNotFoundException, IOException {
        FileInputStream fin = new FileInputStream(file);

        fin.skip(skip);

        return newFixedLengthResponse(status, fin, length);
    }

    public static HttpResponse newChunkedResponse(@NonNull HttpStatus status, @NonNull InputStream responseStream) {
        return new HttpResponse(
            new StreamResponse(responseStream, -1),
            status
        );
    }

    /* ---------------- */
    /* Responses        */
    /* ---------------- */

    public static interface ResponseContent extends Closeable {

        public void write(OutputStream out) throws IOException;

        /**
         * @return any negative number for a chunked response.
         */
        public long getLength();

    }

    @Getter
    @AllArgsConstructor
    public static class StreamResponse implements ResponseContent {
        private InputStream response;
        private long length;

        @Override
        public void write(OutputStream out) throws IOException {
            StreamUtil.streamTransfer(
                this.response,
                out,
                1400, // Average MTU is 1500,
                      // 1400 gives us some slack with chunked encoding and packet overhead.
                this.length
            );
        }

        @Override
        public long getLength() {
            return this.length;
        }

        @Override
        public void close() throws IOException {
            this.response.close();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ByteResponse implements ResponseContent {
        private byte[] response;

        @Override
        public void write(OutputStream out) throws IOException {
            out.write(this.response);
        }

        @Override
        public long getLength() {
            return this.response.length;
        }

        @Override
        public void close() throws IOException {
            this.response = null; // Free, incase of leaks.
        }
    }

}
