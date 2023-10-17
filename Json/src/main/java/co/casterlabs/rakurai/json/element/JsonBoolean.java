package co.casterlabs.rakurai.json.element;

import co.casterlabs.rakurai.json.serialization.JsonSerializationContext;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode
@AllArgsConstructor
public class JsonBoolean implements JsonElement {
    private boolean value;

    @Override
    public boolean isJsonBoolean() {
        return true;
    }

    @Override
    public boolean getAsBoolean() {
        return this.value;
    }

    /* Serialization */

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }

    @Override
    public void serializeToArray(@NonNull JsonSerializationContext ctx) {
        ctx.insertArrayBoolean(this.value);
    }

    @Override
    public void serializeToObject(@NonNull String key, @NonNull JsonSerializationContext ctx) {
        ctx.insertObjectBoolean(key, this.value);
    }

}
