package co.casterlabs.rakurai.json.deserialization;

import static co.casterlabs.rakurai.CharStrings.*;

import co.casterlabs.rakurai.json.Rson.RsonConfig;
import co.casterlabs.rakurai.json.element.JsonNumber;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.NonNull;

public class JsonNumberParser extends JsonParser {
    private static final char[] VALID = "+-0123456789.eE".toCharArray();
    private static final char[] NAN = "NaN".toCharArray();

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

        if (read.length == 0) {
            throw new JsonLexException();
        } else {
            Number value;

            if (settings.areJson5FeaturesEnabled() && strequals(read, NAN)) {
                value = Double.NaN;
            } else if (read[0] == '+' && !settings.areJson5FeaturesEnabled()) {
                // Leading + is a JSON5 feature.
                throw new JsonLexException();
            } else if (strcontainsonly(read, VALID)) {
                String num = new String(read);

                try {
                    if ((strfindex(read, '.') == -1) && (strfindex(read, 'E') == -1) && (strfindex(read, 'e') == -1)) {
                        value = Long.parseLong(num);
                    } else {
                        value = Double.parseDouble(num);
                    }
                } catch (NumberFormatException e) {
                    throw new JsonLexException();
                }
            } else {
                throw new JsonLexException();
            }

            return new ParsedTokenPair(new JsonNumber(value), length);
        }
    }

}
