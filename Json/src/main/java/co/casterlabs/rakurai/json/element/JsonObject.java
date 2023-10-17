package co.casterlabs.rakurai.json.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.serialization.JsonSerializationContext;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode
public class JsonObject implements JsonElement, Iterable<Map.Entry<String, JsonElement>> {
    public static final JsonObject EMPTY_OBJECT = new JsonObject();

    static {
        EMPTY_OBJECT.contents = Collections.emptyMap();
    }

    private Map<String, JsonElement> contents = new LinkedHashMap<>();

    /* Static Construction */

    public static JsonObject singleton(@NonNull String key, @Nullable Object value) {
        return new JsonObject()
            .put(key, Rson.DEFAULT.toJson(value));
    }

    /* Access */

    @Override
    public boolean isJsonObject() {
        return true;
    }

    @Override
    public JsonObject getAsObject() {
        return this;
    }

    /* Serialization */

    @Override
    public String toString() {
        return this.toString(false);
    }

    @Override
    public void serializeToArray(@NonNull JsonSerializationContext ctx) {
        ctx.startObject();

        for (Map.Entry<String, JsonElement> entry : this.contents.entrySet()) {
            JsonElement element = entry.getValue();

            element.serializeToObject(entry.getKey(), ctx);
        }

        ctx.endObject();
    }

    @Override
    public void serializeToObject(@NonNull String key, @NonNull JsonSerializationContext ctx) {
        ctx.startObjectEntry(key);

        for (Map.Entry<String, JsonElement> entry : this.contents.entrySet()) {
            JsonElement element = entry.getValue();

            element.serializeToObject(entry.getKey(), ctx);
        }

        ctx.endObject();
    }

    /* Helpers */

    public String getString(String key) {
        return this.contents.get(key).getAsString();
    }

    public JsonNumber getNumber(String key) {
        return this.contents.get(key).getAsNumber();
    }

    public boolean getBoolean(String key) {
        return this.contents.get(key).getAsBoolean();
    }

    public JsonArray getArray(String key) {
        return this.contents.get(key).getAsArray();
    }

    public JsonObject getObject(String key) {
        return this.contents.get(key).getAsObject();
    }

    public JsonObject put(@NonNull String key, @Nullable String value) {
        if (value == null) {
            this.putNull(key);
        } else {
            this.contents.put(key, new JsonString(value));
        }

        return this;
    }

    public JsonObject put(@NonNull String key, @Nullable Number value) {
        if (value == null) {
            this.putNull(key);
        } else {
            this.contents.put(key, new JsonNumber(value));
        }

        return this;
    }

    public JsonObject put(@NonNull String key, boolean value) {
        this.contents.put(key, new JsonBoolean(value));

        return this;
    }

    public JsonObject putNull(@NonNull String key) {
        this.contents.put(key, JsonNull.INSTANCE);

        return this;
    }

    public JsonObject put(@NonNull String key, @Nullable JsonElement value) {
        if (value == null) {
            value = JsonNull.INSTANCE;
        }

        this.contents.put(key, value);

        return this;
    }

    /* Proxy */

    public int size() {
        return this.contents.size();
    }

    public boolean isEmpty() {
        return this.contents.isEmpty();
    }

    public boolean containsKey(@NonNull String key) {
        return this.contents.containsKey(key);
    }

    public boolean containsValue(@Nullable JsonElement value) {
        if (value == null) {
            value = JsonNull.INSTANCE;
        }

        return this.containsValue(value);
    }

    public JsonElement get(@NonNull String key) {
        return this.contents.get(key);
    }

    public JsonElement remove(@NonNull String key) {
        return this.contents.remove(key);
    }

    public void clear() {
        this.contents.clear();
    }

    public Set<String> keySet() {
        return new HashSet<>(this.contents.keySet());
    }

    public Collection<JsonElement> values() {
        return new ArrayList<>(this.contents.values());
    }

    public Set<Map.Entry<String, JsonElement>> entrySet() {
        return new HashSet<>(this.contents.entrySet());
    }

    @Override
    public Iterator<Entry<String, JsonElement>> iterator() {
        return this.entrySet().iterator();
    }

    public Map<String, JsonElement> toMap() {
        return new HashMap<>(this.contents);
    }

}
