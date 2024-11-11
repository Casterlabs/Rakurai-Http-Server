package co.casterlabs.rhs.protocol.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.protocol.RHSConnection;
import co.casterlabs.rhs.protocol.http.HttpResponse.ResponseContent;

class _CompressionUtil {
    private static final int COMPRESSION_THRESHOLD = 100 /*kb*/ * 1024; // Arbitrary

    private static boolean shouldCompress(@Nullable String mimeType) {
        if (mimeType == null) return false;

        // Source: https://cdn.jsdelivr.net/gh/jshttp/mime-db@master/db.json

        // Literal text.
        if (mimeType.startsWith("text/")) return true;
        if (mimeType.contains("+text")) return true;

        // Compressible data types.
        if (mimeType.contains("json")) return true;
        if (mimeType.contains("xml")) return true;
        if (mimeType.contains("csv")) return true;

        // Other.
        if (mimeType.contains("application/javascript") || mimeType.contains("application/x-javascript")) return true;
        if (mimeType.contains("image/bmp")) return true;
        if (mimeType.contains("image/vnd.adobe.photoshop")) return true;
        if (mimeType.contains("image/vnd.microsoft.icon") || mimeType.contains("image/x-icon")) return true;
        if (mimeType.contains("application/tar") || mimeType.contains("application/x-tar")) return true;
        if (mimeType.contains("application/wasm")) return true;

        return false;
    }

    private static List<String> getAcceptedEncodings(RHSConnection session) {
        List<String> accepted = new LinkedList<>();

        for (String value : session.headers.getOrDefault("Accept-Encoding", Collections.emptyList())) {
            String[] split = value.split(", ");
            for (String encoding : split) {
                accepted.add(encoding.toLowerCase());
            }
        }

        return accepted;
    }

    static String pickEncoding(RHSConnection session, HttpResponse response) {
        if (session.httpVersion.value <= 1.0) {
            return null;
        }

        if (response.content.length() < COMPRESSION_THRESHOLD) {
            // This handles both chunked and fixed-length responses.
            return null;
        }

        if (!shouldCompress(response.headers.get("Content-Type"))) {
            return null;
        }

        List<String> acceptedEncodings = getAcceptedEncodings(session);

        // Order of our preference.
        if (acceptedEncodings.contains("gzip")) {
            return "gzip";
        } else if (acceptedEncodings.contains("deflate")) {
            return "deflate";
        }
        // Brotli looks to be difficult. Not going to be supported for a while.

        return null;
    }

    static void writeWithEncoding(@Nullable String encoding, int recommendedBufferSize, OutputStream out, ResponseContent content) throws IOException {
        if (encoding == null) {
            encoding = ""; // Switch doesn't support nulls :/
        }

        switch (encoding) {
            case "gzip":
                try (GZIPOutputStream enc = new GZIPOutputStream(out)) {
                    content.write(recommendedBufferSize, enc);
                }
                break;

            case "deflate":
                try (DeflaterOutputStream enc = new DeflaterOutputStream(out)) {
                    content.write(recommendedBufferSize, enc);
                }
                break;

            default:
                content.write(recommendedBufferSize, out);
                break;
        }
    }

}
