package co.casterlabs.rakurai.json.deserialization;

import static co.casterlabs.rakurai.CharStrings.*;

import co.casterlabs.rakurai.json.Rson.RsonConfig;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.NonNull;

public class JsonObjectParser extends JsonParser {
    private final JsonStringParser STRING_PARSER = new JsonStringParser();

    @Override
    public ParsedTokenPair readToken(char[] in, int skip, @NonNull RsonConfig settings) throws JsonParseException, JsonLexException {
        int sectionLength = 0;
        boolean startFound = false;

        int level = 0;
        boolean isStringEscaped = false;
        boolean inString = false;
        while (true) {
            int pos = sectionLength + skip;

            if (pos >= in.length) {
                if (startFound) {
                    throw new JsonParseException("Cannot find end of object: " + new String(in));
                } else {
                    throw new JsonLexException();
                }
            } else {
                char c = in[pos];

                sectionLength++;

                boolean isQuote = (c == '"') || (settings.areJson5FeaturesEnabled() && (c == '\''));

                if (isQuote && !isStringEscaped) {
                    inString = !inString;
                } else if (!inString) {
                    if ((c == '[') && !startFound) {
                        // We've stumbled on an array.
                        throw new JsonLexException();
                    } else if (c == '{') {
                        level++;
                        startFound = true;
                    } else if (!startFound && (strfindex(JsonParser.JSON_WHITESPACE, c) == -1)) {
                        throw new JsonLexException();
                    } else if (c == '}') {
                        level--;

                        if (level == 0) {
                            break;
                        }
                    }
                }

                if (isStringEscaped) {
                    isStringEscaped = false;
                } else if (c == '\\') {
                    isStringEscaped = true;
                }
            }
        }

        char[] section = new char[sectionLength];
        strcpy(in, section, skip);

        int dSkip = strfindex(section, '{') + 1;
        int dLen = strlindex(section, '}') + 1;

        int objectContentsLen = dLen - dSkip;
        char[] objectContents = new char[objectContentsLen];
        strcpy(section, objectContents, dSkip);

        JsonObject object = new JsonObject();

        int read = 0;
        while (read < objectContentsLen - 1) {
            String key;

            try {
                ParsedTokenPair keyPair = STRING_PARSER.readToken(objectContents, read, settings);
                read += keyPair.getRead();

                // Eat remaining whitespace up to the colon.
                while (strfindex(JsonParser.JSON_WHITESPACE, objectContents[read]) != -1) {
                    read++;
                }
                read++; // Consume the colon.

                key = keyPair.getElement().getAsString();
            } catch (JsonLexException e) {
                // Try to parse json5 keys.
                if (!settings.areJson5FeaturesEnabled()) {
                    throw e;
                }

                // Eat remaining whitespace.
                while (strfindex(JsonParser.JSON_WHITESPACE, objectContents[read]) != -1) {
                    read++;
                }

                int colonLocation = strfindex(objectContents, ':', read);
                if (colonLocation == -1) throw e;

                int keyLen = colonLocation - read;
                if (keyLen < -1) throw e;

                char[] keyContents = new char[keyLen];
                strcpy(objectContents, keyContents, read);

                if (strcontainsany(keyContents, JsonStringParser.NEEDS_ESCAPE)) {
                    throw new JsonParseException("Cannot make heads or tails of object key: " + new String(keyContents));
                }

                key = new String(keyContents);
                read = colonLocation + 1; // Skip.
            }

            ParsedTokenPair elementPair = JsonParser.parseElement(objectContents, read, settings);

            if (elementPair == null) {
                read++; // Was a dud.
            } else {
                read += elementPair.getRead();

                object.put(key, elementPair.getElement());
            }

            // Eat remaining whitespace.
            while (strfindex(JsonParser.JSON_WHITESPACE, objectContents[read]) != -1) {
                read++;
            }

            if (objectContents[read] == ',') {
                read++; // Eat a remaining comma.
            }
        }

        return new ParsedTokenPair(object, sectionLength);
    }

}
