package co.casterlabs.rhs.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rhs.server.HttpResponse.ResponseContent;
import co.casterlabs.rhs.session.HttpSession;

public class HttpServerUtil {

    public static boolean shouldCompress(@Nullable String mimeType) {
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

    public static List<String> getAcceptedEncodings(HttpSession session) {
        List<String> accepted = new LinkedList<>();

        for (String value : session.getHeaders().getOrDefault("Accept-Encoding", Collections.emptyList())) {
            String[] split = value.split(", ");
            for (String encoding : split) {
                accepted.add(encoding.toLowerCase());
            }
        }

        return accepted;
    }

    public static String pickEncoding(HttpSession session, HttpResponse response) {
        return null;

//        if (session.getVersion().value <= 1.0) {
//            return null;
//        }
//
//        if (!shouldCompress(response.getAllHeaders().get("Content-Type"))) {
//            session.getLogger().debug("Format does not appear to be compressible, sending without encoding.");
//        }
//
//        List<String> acceptedEncodings = getAcceptedEncodings(session);
//        String chosenEncoding = null;
//
//        // Order of our preference.
//        if (acceptedEncodings.contains("gzip")) {
//            chosenEncoding = "gzip";
//            session.getLogger().debug("Client supports GZip encoding, using that.");
//        } else if (acceptedEncodings.contains("deflate")) {
//            chosenEncoding = "deflate";
//            session.getLogger().debug("Client supports Deflate encoding, using that.");
//        }
//        // Brotli looks to be difficult. Not going to be supported for a while.
//
//        if (chosenEncoding != null) {
//            response.putHeader("Content-Encoding", chosenEncoding);
//            response.putHeader("Vary", "Accept-Encoding");
//        }
//
//        return chosenEncoding;
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

}
