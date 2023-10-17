package co.casterlabs.rakurai;

import lombok.NonNull;

public class StringUtil {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private static final String EMPTY_STRING = "";

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;

            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars);
    }

    public static int countCharRepeats(String text, char query) {
        int result = 0;

        for (char c : text.toCharArray()) {
            if (c == query) {
                result++;
            }
        }

        return result;
    }

    public static String indentAllLines(String text, String indent) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\n");

        for (String line : lines) {
            result
                .append('\n')
                .append(indent)
                .append(line);
        }

        return result.substring(1); // Remove the first '\n'
    }

    public static String repeat(@NonNull String repeat, int amount) {
        // Avoid unnecessary allocations.
        if (amount == 0) {
            return EMPTY_STRING;
        } else if (amount == 1) {
            return repeat;
        } else {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < amount; i++) {
                sb.append(repeat);
            }

            return sb.toString();
        }
    }

    public static String prettifyHeader(String header) {
        char[] chars = header.toLowerCase().toCharArray();

        chars[0] = Character.toUpperCase(chars[0]);

        // Go ahead and start at 1, since we already replaced char[0].
        for (int i = 1; i < chars.length; i++) {
            if ((chars[i] == '-') && ((i + 1) < chars.length)) {
                chars[i + 1] = Character.toUpperCase(chars[i + 1]);
            }
        }

        return new String(chars);
    }

}
