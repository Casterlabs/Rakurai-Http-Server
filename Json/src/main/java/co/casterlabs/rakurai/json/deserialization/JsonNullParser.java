package co.casterlabs.rakurai.json.deserialization;

import static co.casterlabs.rakurai.CharStrings.*;

import co.casterlabs.rakurai.json.Rson.RsonConfig;
import co.casterlabs.rakurai.json.element.JsonNull;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.NonNull;

public class JsonNullParser extends JsonParser {
    private static final char[] NULL = "null".toCharArray();

    @Override
    public ParsedTokenPair readToken(char[] in, int skip, @NonNull RsonConfig settings) throws JsonParseException, JsonLexException {
        int length = 0;

        while (true) {
            char c = in[length + skip];

            if (strfindex(JsonParser.JSON_END_TOKENS, c) != -1) {
                break;
            } else {
                length++;

                if (length + skip == in.length) {
                    break;
                }
            }
        }

        char[] read = new char[length];
        strcpy(in, read, skip);

        read = strdropchars(read, JsonParser.JSON_WHITESPACE);

        if (strequals(read, NULL)) {
            return new ParsedTokenPair(JsonNull.INSTANCE, length);
        } else {
            throw new JsonLexException();
        }
    }

}
