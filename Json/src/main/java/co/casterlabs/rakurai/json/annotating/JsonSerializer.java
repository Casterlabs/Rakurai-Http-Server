package co.casterlabs.rakurai.json.annotating;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.DefaultJsonSerializer;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.NonNull;

public interface JsonSerializer<T> {
    public static final JsonSerializer<Object> DEFAULT = new DefaultJsonSerializer();

    default JsonElement serialize(@NonNull Object value, @NonNull Rson rson) {
        return DEFAULT.serialize(value, rson);
    }

    @SuppressWarnings("unchecked")
    default @Nullable T deserialize(@NonNull JsonElement value, @NonNull Class<?> type, @NonNull Rson rson) throws JsonParseException {
        return (T) DEFAULT.deserialize(value, type, rson);
    }

}
