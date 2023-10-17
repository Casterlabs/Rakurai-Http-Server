package co.casterlabs.rakurai.json.element;

import co.casterlabs.rakurai.json.JsonStringUtil;
import co.casterlabs.rakurai.json.serialization.JsonSerializationContext;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode
@AllArgsConstructor
public class JsonString implements JsonElement {
    private String value;

    public JsonString(Enum<?> e) {
        this.value = e.name();
    }

    @Override
    public boolean isJsonString() {
        return true;
    }

    @Override
    public String getAsString() {
        return this.value;
    }

    /* Serialization */

    @Override
    public String toString() {
        return String.format("\"%s\"", JsonStringUtil.jsonEscape(this.value));
    }

    @Override
    public void serializeToArray(@NonNull JsonSerializationContext ctx) {
        ctx.insertArrayString(this.value);
    }

    @Override
    public void serializeToObject(@NonNull String key, @NonNull JsonSerializationContext ctx) {
        ctx.insertObjectString(key, this.value);
    }

}
