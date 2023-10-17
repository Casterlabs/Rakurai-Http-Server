package co.casterlabs.rakurai.json.element;

import co.casterlabs.rakurai.json.serialization.JsonSerializationContext;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * For use in non-standard JSON applications where you need to include a raw
 * string (BSON, for example).
 * 
 * @implNote JsonRaw CANNOT be deserialized.
 */
@Deprecated
@EqualsAndHashCode
@AllArgsConstructor
public class JsonRaw implements JsonElement {
    private String raw;

    /* Serialization */

    @Override
    public String toString() {
        return this.raw;
    }

    @Override
    public void serializeToArray(@NonNull JsonSerializationContext ctx) {
        ctx.insertRaw(this.raw);
    }

    @Override
    public void serializeToObject(@NonNull String key, @NonNull JsonSerializationContext ctx) {
        ctx.insertObjectRaw(key, this.raw);
    }

}
