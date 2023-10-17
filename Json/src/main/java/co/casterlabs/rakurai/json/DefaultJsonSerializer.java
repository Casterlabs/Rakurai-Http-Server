package co.casterlabs.rakurai.json;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;

import co.casterlabs.rakurai.json.annotating.JsonSerializer;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rakurai.json.serialization.JsonSerializeException;
import lombok.NonNull;

public class DefaultJsonSerializer implements JsonSerializer<Object> {

    @Override
    public JsonElement serialize(@NonNull Object value, @NonNull Rson rson) {
        try {
            JsonObject json = new JsonObject();
            Class<?> type = value.getClass();

            Map<String, Field> fields = JsonReflectionUtil.getFields(type);

            for (Entry<String, Field> entry : fields.entrySet()) {
                String fieldName = entry.getKey();
                Field field = entry.getValue();

                field.setAccessible(true);

                JsonElement serialized = rson.toJson(field.get(value));

                json.put(fieldName, serialized);
            }

            return json;
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new JsonSerializeException(e);
        }
    }

    @Override
    public Object deserialize(@NonNull JsonElement value, @NonNull Class<?> type, @NonNull Rson rson) throws JsonParseException {
        if (!value.isJsonObject()) {
            throw new JsonParseException(String.format("Expected a JsonObject but got %s for %s", value.getClass().getSimpleName(), type));
        }

        try {
            JsonObject json = value.getAsObject();
            Object o = JsonReflectionUtil.newInstance(type);

            Map<String, Field> fields = JsonReflectionUtil.getFields(type);

            for (Entry<String, Field> entry : fields.entrySet()) {
                String fieldName = entry.getKey();
                Field field = entry.getValue();

                field.setAccessible(true);

                Class<?> fieldType = field.getType();

                JsonElement e = json.get(fieldName);
                if (e == null) continue; // Not present.

                if (e.isJsonNull()) {
                    // We treat a null json value the same way as javascript does, as a value.
                    if (fieldType == boolean.class) {
                        field.set(o, false);
                    } else if (fieldType.isPrimitive()) {
                        field.set(o, 0); // It's a number type.
                    } else {
                        field.set(o, null);
                    }
                } else {
                    Class<?>[] fieldComponents = JsonReflectionUtil.getCollectionComponentForField(field);
                    Object converted = rson.fromJson(e, TypeToken.of(fieldType, fieldComponents));

                    field.set(o, converted);
                }
            }

            return o;
        } catch (IllegalAccessException | InstantiationException | IllegalArgumentException | SecurityException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
            throw new JsonParseException(e);
        }
    }

}
