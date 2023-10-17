package co.casterlabs.rakurai;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import lombok.NonNull;

public class CharStrings {

    public static byte[] strbytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);

        byte[] bytes = byteBuffer.array().clone();

        charBuffer = null;
        byteBuffer = null;

        return bytes;
    }

    public static void strcpy(@NonNull char[] source, @NonNull char[] dest) {
        System.arraycopy(source, 0, dest, 0, dest.length);
    }

    public static void strcpy(@NonNull char[] source, @NonNull char[] dest, int skip) {
        System.arraycopy(source, skip, dest, 0, dest.length);
    }

    public static boolean strcontainsany(@NonNull char[] str, @NonNull char[] seek) {
        for (int idx = 0; idx < str.length; idx++) {
            for (int skidx = 0; skidx < seek.length; skidx++) {
                if (str[idx] == seek[skidx]) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean strequals(@NonNull char[] s1, @NonNull char[] s2) {
        if (s1.length == s2.length) {
            for (int idx = 0; idx < s1.length; idx++) {
                if (s1[idx] != s2[idx]) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public static boolean strcontainsonly(@NonNull char[] str, @NonNull char[] valid) {
        for (int idx = 0; idx < str.length; idx++) {
            if (strfindex(valid, str[idx]) == -1) {
                return false;
            }
        }

        return true;
    }

    public static char[] strdropchars(@NonNull char[] str, @NonNull char[] drop) {
        char[] buf = new char[str.length];
        int bufLen = 0;

        for (int idx = 0; idx < str.length; idx++) {
            char c = str[idx];

            if (strfindex(drop, c) == -1) {
                buf[bufLen] = c;
                bufLen++;
            }
        }

        char[] result = new char[bufLen];

        strcpy(buf, result);

        return result;
    }

    public static int strfindex(@NonNull char[] str, char seek) {
        return strfindex(str, seek, 0);
    }

    public static int strfindex(@NonNull char[] str, char seek, int skip) {
        for (int idx = skip; idx < str.length; idx++) {
            if (str[idx] == seek) {
                return idx;
            }
        }

        return -1;
    }

    public static int strlindex(@NonNull char[] str, char seek) {
        for (int idx = str.length - 1; idx >= 0; idx--) {
            if (str[idx] == seek) {
                return idx;
            }
        }

        return -1;
    }

}
