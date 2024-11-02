package co.casterlabs.rhs.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import co.casterlabs.rhs.util.CaseInsensitiveMultiMap;

public class RHSConnectionWriter {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O");

    public static final int HTTP_PERSISTENT_TIMEOUT = 30;

    public static void writeOutStatus(RHSConnection connection, HttpStatus status) throws IOException {
        // 0.9 doesn't have a status line, so we don't write it out.
        if (connection.httpVersion == HttpVersion.HTTP_0_9) return;

        writeString(connection.httpVersion.toString(), connection.output);
        writeString(" ", connection.output);
        writeString(status.statusString(), connection.output);
        writeString("\r\n", connection.output);
    }

    public static void writeOutHeaders(RHSConnection connection, CaseInsensitiveMultiMap headers) throws IOException {
        // 0.9 doesn't have headers, so we don't write it out.
        if (connection.httpVersion == HttpVersion.HTTP_0_9) return;

        for (Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                writeString(key, connection.output);
                writeString(": ", connection.output);
                writeString(value, connection.output);
                writeString("\r\n", connection.output);
            }
        }

        // Write the separation line.
        writeString("\r\n", connection.output);
    }

    public static void writeOutHeaders(RHSConnection connection, Map<String, String> headers) throws IOException {
        // 0.9 doesn't have headers, so we don't write it out.
        if (connection.httpVersion == HttpVersion.HTTP_0_9) return;

        for (Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            writeString(key, connection.output);
            writeString(": ", connection.output);
            writeString(value, connection.output);
            writeString("\r\n", connection.output);
        }

        // Write the separation line.
        writeString("\r\n", connection.output);
    }

    public static void writeString(String str, OutputStream out) throws IOException {
        out.write(str.getBytes(RHSConnection.CHARSET));
    }

    public static String getHttpTime() {
        return TIME_FORMATTER.format(ZonedDateTime.now(ZoneOffset.UTC));
    }

}
