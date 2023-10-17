package co.casterlabs.rakurai.json.element;

import co.casterlabs.rakurai.json.serialization.JsonSerializationContext;
import lombok.NonNull;

public class JsonNull implements JsonElement {
    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {}

    @Override
    public boolean isJsonNull() {
        return true;
    }

    /* Serialization */

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public void serializeToArray(@NonNull JsonSerializationContext ctx) {
        ctx.insertArrayNull();
    }

    @Override
    public void serializeToObject(@NonNull String key, @NonNull JsonSerializationContext ctx) {
        ctx.insertObjectNull(key);
    }

}
