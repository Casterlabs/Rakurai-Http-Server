package co.casterlabs.rakurai.json.element;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.Rson.RsonConfig;
import co.casterlabs.rakurai.json.serialization.JsonSerializationContext;
import lombok.NonNull;

public interface JsonElement {

    /* String */

    default boolean isJsonString() {
        return false;
    }

    default String getAsString() {
        throw new UnsupportedOperationException();
    }

    /* Number */

    default boolean isJsonNumber() {
        return false;
    }

    default JsonNumber getAsNumber() {
        throw new UnsupportedOperationException();
    }

    /* Null */

    default boolean isJsonNull() {
        return false;
    }

    /* Boolean */

    default boolean isJsonBoolean() {
        return false;
    }

    default boolean getAsBoolean() {
        throw new UnsupportedOperationException();
    }

    /* Array */

    default boolean isJsonArray() {
        return false;
    }

    default JsonArray getAsArray() {
        throw new UnsupportedOperationException();
    }

    /* Object */

    default boolean isJsonObject() {
        return false;
    }

    default JsonObject getAsObject() {
        throw new UnsupportedOperationException();
    }

    /* Serialization */

    default String toString(boolean prettyPrinting) {
        return toString(Rson.DEFAULT.getConfig(), prettyPrinting);
    }

    default String toString(RsonConfig config, boolean prettyPrinting) {
        JsonSerializationContext ctx = new JsonSerializationContext()
            .setConfig(config)
            .setPrettyPrinting(prettyPrinting);

        this.serialize(ctx);

        return ctx.toString();
    }

    default void serialize(@NonNull JsonSerializationContext ctx) {
        // Functionally the same
        this.serializeToArray(ctx);
    }

    public void serializeToArray(@NonNull JsonSerializationContext ctx);

    public void serializeToObject(@NonNull String key, @NonNull JsonSerializationContext ctx);

}
