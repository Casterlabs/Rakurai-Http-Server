package co.casterlabs.rakurai.json.element;

import co.casterlabs.rakurai.json.serialization.JsonSerializationContext;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class JsonNumber extends Number implements JsonElement {
    private static final long serialVersionUID = -7482974399597626813L;

    private Number value;

    @Override
    public boolean isJsonNumber() {
        return true;
    }

    @Override
    public JsonNumber getAsNumber() {
        return this;
    }

    /* Serialization */

    @Override
    public String toString() {
        return this.value.toString();
    }

    @Override
    public void serializeToArray(@NonNull JsonSerializationContext ctx) {
        ctx.insertArrayNumber(this.value);
    }

    @Override
    public void serializeToObject(@NonNull String key, @NonNull JsonSerializationContext ctx) {
        ctx.insertObjectNumber(key, this.value);
    }

    /* Proxy */

    @Override
    public int intValue() {
        return this.value.intValue();
    }

    @Override
    public long longValue() {
        return this.value.longValue();
    }

    @Override
    public float floatValue() {
        return this.value.floatValue();
    }

    @Override
    public double doubleValue() {
        return this.value.doubleValue();
    }

}
