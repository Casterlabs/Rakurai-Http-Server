package co.casterlabs.rakurai.json;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.JsonReflectionUtil.JsonDeserializerMethodImpl;
import co.casterlabs.rakurai.json.JsonReflectionUtil.JsonSerializerMethodImpl;
import co.casterlabs.rakurai.json.JsonReflectionUtil.JsonValidatorImpl;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.annotating.JsonSerializer;
import co.casterlabs.rakurai.json.deserialization.JsonParser;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonNull;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.element.JsonString;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rakurai.json.serialization.JsonSerializeException;
import co.casterlabs.rakurai.json.validation.JsonValidationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

public class Rson {
    public static final Rson DEFAULT = new Rson.Builder()
        .build();

    @Getter
    private final RsonConfig config;

    private Map<Class<?>, TypeResolver<?>> resolvers = new HashMap<>();

    private Rson(Builder builder) {
        this.config = builder.toConfig();
        this.resolvers.putAll(builder.resolvers);
    }

    /* -------------------- */
    /* Obj->Json            */
    /* -------------------- */

    public <T> JsonElement toJson(@Nullable T o) {
        if (o == null) {
            return JsonNull.INSTANCE;
        }

        if (o instanceof JsonElement) {
            return (JsonElement) o;
        }

        @SuppressWarnings("unchecked")
        TypeResolver<T> resolver = (TypeResolver<T>) this.resolvers.get(o.getClass());
        if (resolver != null) {
            return resolver.writeOut(o, o.getClass());
        }

        try {
            Class<?> clazz = o.getClass();

            if (Enum.class.isAssignableFrom(clazz)) {
                Enum<?> en = (Enum<?>) o;

                return new JsonString(en.name());
            }

            if (clazz.isArray()) {
                JsonArray result = new JsonArray();

                int len = Array.getLength(o);
                for (int i = 0; i < len; i++) {
                    Object entry = Array.get(o, i);
                    result.add(this.toJson(entry));
                }

                return result;
            }

            if (Collection.class.isAssignableFrom(clazz)) {
                JsonArray result = new JsonArray();

                for (Object entry : new ArrayList<>((Collection<?>) o)/*copy*/) {
                    result.add(this.toJson(entry));
                }

                return result;
            }

            if (Map.class.isAssignableFrom(clazz)) {
                JsonObject result = new JsonObject();

                for (Map.Entry<?, ?> entry : new ArrayList<>(((Map<?, ?>) o).entrySet())/*copy*/) {
                    JsonElement key = this.toJson(entry.getKey());
                    if (!key.isJsonString()) {
                        throw new JsonSerializeException("Map key must be a valid String-like. (e.g String or Enum), got: " + key);
                    }

                    result.put(
                        key.getAsString(),
                        this.toJson(entry.getValue())
                    );
                }

                return result;
            }

            JsonSerializer<?> serializer;

            // Create the serializer, or supply a default.
            JsonClass classData = clazz.getAnnotation(JsonClass.class);
            if (classData != null) {
                serializer = JsonReflectionUtil.newInstance(classData.serializer());
            } else {
                serializer = JsonSerializer.DEFAULT;
            }

            JsonElement result = serializer.serialize(o, this);

            if (result.isJsonObject()) {
                // We need to call all methods marked with @JsonSerializationMethod.
                JsonObject dest = result.getAsObject();
                Collection<JsonSerializerMethodImpl> serializerMethods = JsonReflectionUtil.getJsonSerializerMethodsForClass(clazz);

                for (JsonSerializerMethodImpl m : serializerMethods) {
                    m.generate(o, dest);
                }
            }

            return result;
        } catch (IllegalArgumentException | SecurityException | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new JsonSerializeException(e);
        }
    }

    /* -------------------- */
    /* String->Json->Obj    */
    /* -------------------- */

    public <T> T fromJson(@NonNull String json, @NonNull Class<T> expected) throws JsonParseException, JsonValidationException {
        return this.fromJson(json, TypeToken.of(expected));
    }

    public <T> T fromJson(@NonNull String json, @NonNull TypeToken<T> token) throws JsonParseException, JsonValidationException {
        JsonElement e = JsonParser.parseString(json, this.config);

        return this.fromJson(e, token);
    }

    public <T> T fromJson(@NonNull JsonElement e, @NonNull Class<T> expected) throws JsonParseException, JsonValidationException {
        Class<?> componentType;

        if (expected.isArray()) {
            componentType = expected.getComponentType();
        } else {
            componentType = Object.class;
        }

        return this.fromJson(e, TypeToken.of(expected, componentType));
    }

    public <T> T fromJson(@NonNull JsonElement e, TypeToken<T> token) throws JsonParseException, JsonValidationException {
        // Note that this is the "last stop" and only performs validation/annotation.
        // fromJson0 is the real gravy.
        try {
            T result = this.fromJson0(e, token);
            Class<?> expected = token.getTypeClass();

            if (e.isJsonObject()) {
                // We need to call all methods marked with @JsonDeserializationMethod.
                JsonObject source = e.getAsObject();
                Collection<JsonDeserializerMethodImpl> serializerMethods = JsonReflectionUtil.getJsonDeserializerMethodsForClass(expected);

                for (JsonDeserializerMethodImpl m : serializerMethods) {
                    m.accept(result, source);
                }
            }

            // Validate (These throw on error).
            Collection<JsonValidatorImpl> validators = JsonReflectionUtil.getJsonValidatorsForClass(expected);
            for (JsonValidatorImpl v : validators) {
                v.validate(result);
            }

            return result;
        } catch (IllegalArgumentException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException ex) {
            throw new JsonSerializeException(ex);
        }
    }

    /* -------------------- */
    /* Json->Obj            */
    /* -------------------- */

    @SuppressWarnings({
            "unchecked"
    })
    private <T> T fromJson0(JsonElement e, TypeToken<T> token) throws JsonParseException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        if (e.isJsonNull()) {
            return null; // Always null :D
        }

        Class<?> expected = token.getTypeClass();

        // Raw json elements.
        if (JsonElement.class.isAssignableFrom(expected)) {
            if (expected == JsonElement.class) {
                return (T) e;
            } else if (e.getClass() == expected) {
                return (T) e;
            } else {
                throw new JsonParseException(String.format("Expected a %s but got a %s\n%s", expected.getSimpleName(), e.getClass().getSimpleName(), e));
            }
        }

        // Try a type resolver.
        TypeResolver<T> resolver = (TypeResolver<T>) this.resolvers.get(expected);
        if (resolver != null) {
            return resolver.resolve(e, expected);
        }

        // Parse enums.
        if (Enum.class.isAssignableFrom(expected)) {
            String name = e.getAsString();

            for (Object enC : expected.getEnumConstants()) {
                Enum<?> en = (Enum<?>) enC;

                if (en.name().equalsIgnoreCase(name)) {
                    return (T) en;
                }
            }

            throw new JsonParseException(String.format("Cannot deserialize enum (%s) from %s.", expected, name));
        }

        // Arrays & Collections
        boolean isCollection = Collection.class.isAssignableFrom(expected);
        boolean isArray = token.isArrayType();
        if (isCollection || isArray) {
            if (!e.isJsonArray()) {
                throw new JsonParseException(String.format("Expected a %s but got a %s\n%s", expected.getSimpleName(), e.getClass().getSimpleName(), e));
            }

            Class<?> componentType;

            if (isArray) {
                componentType = expected.getComponentType();
            } else if (isCollection) {
                componentType = JsonReflectionUtil.typeToClass(token.getTypeArguments()[0], null);
            } else {
                componentType = Object.class;
            }

            JsonArray array = e.getAsArray();

            Object result = Array.newInstance(componentType, array.size());
            TypeToken<?> itemComponentType = TypeToken.of(componentType, JsonReflectionUtil.getCollectionComponent(componentType));

            for (int i = 0; i < array.size(); i++) {
                Object item = this.fromJson(array.get(i), itemComponentType);

                Array.set(result, i, item);
            }

            if (isArray) {
                return (T) result;
            } else {
                // stacks, queues, deques, lists and trees
                Collection<Object> coll;

                if (Stack.class.isAssignableFrom(expected)) {
                    coll = new Stack<>();
                } else if (Set.class.isAssignableFrom(expected)) {
                    coll = new HashSet<>();
                } else if (Queue.class.isAssignableFrom(expected)) {
                    coll = new PriorityQueue<>();
                } else if (Deque.class.isAssignableFrom(expected)) {
                    coll = new ArrayDeque<>();
                } else if (List.class.isAssignableFrom(expected)) {
                    coll = new ArrayList<>();
                } else {
                    throw new JsonParseException("Cannot create a matching collection.");
                }

                for (int i = 0; i < Array.getLength(result); i++) {
                    Object item = Array.get(result, i);
                    coll.add(item);
                }

                return (T) coll;
            }
        }

        // Maps.
        if (Map.class.isAssignableFrom(expected)) {
            if (!e.isJsonObject()) {
                throw new JsonParseException(String.format("Expected a %s but got a %s\n%s", expected.getSimpleName(), e.getClass().getSimpleName(), e));
            }

            Class<?> keyType = JsonReflectionUtil.typeToClass(token.getTypeArguments()[0], null);
            Class<?> valueType = JsonReflectionUtil.typeToClass(token.getTypeArguments()[1], null);

            if ((keyType != String.class) && !keyType.isEnum()) {
                throw new JsonParseException(String.format("Can only deserialize map if the key type is a String or Enum\n%s", e));
            }

            Map<Object, Object> map = new LinkedHashMap<>();

            for (Map.Entry<String, JsonElement> entry : e.getAsObject().entrySet()) {
                Object key = null;

                if (keyType == String.class) {
                    key = entry.getKey();
                } else {
                    for (Object constant : keyType.getEnumConstants()) {
                        Enum<?> enumConstant = (Enum<?>) constant;
                        if (enumConstant.name().equalsIgnoreCase(entry.getKey())) {
                            key = enumConstant;
                            break;
                        }
                    }

                    if (key == null) {
                        throw new JsonParseException(String.format("Could not find a valid enum constant for string \"%s\" out of %s", entry.getKey(), Arrays.toString(keyType.getEnumConstants())));
                    }
                }

                Object value = this.fromJson(entry.getValue(), valueType);
                map.put(key, value);
            }

            return (T) map;
        }

        // Plain ole' object deserialization.
        JsonClass classData = expected.getAnnotation(JsonClass.class);
        JsonSerializer<?> deserializer;

        // Create the deserializer, or supply a default.
        if (classData != null) {
            deserializer = JsonReflectionUtil.newInstance(classData.serializer());
        } else {
            deserializer = JsonSerializer.DEFAULT;
        }

        return (T) deserializer.deserialize(e, expected, this);
    }

    /* -------------------- */
    /* Config               */
    /* -------------------- */

    @Data
    @Accessors(chain = true)
    public static class Builder {
        @Getter(AccessLevel.NONE)
        private boolean json5FeaturesEnabled = false;

        private String tabCharacter = "    ";

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private Map<Class<?>, TypeResolver<?>> resolvers = DefaultTypeResolvers.get();

        // Make it more fluent. :^)
        public boolean areJson5FeaturesEnabled() {
            return this.json5FeaturesEnabled;
        }

        public Builder registerTypeResolver(TypeResolver<?> resolver, Class<?>... types) {
            for (Class<?> type : types) {
                this.resolvers.put(type, resolver);
            }

            return this;
        }

        public Rson build() {
            return new Rson(this);
        }

        public RsonConfig toConfig() {
            return new RsonConfig(this.json5FeaturesEnabled, this.tabCharacter);
        }

    }

    @Getter
    @AllArgsConstructor
    public static class RsonConfig {
        @Getter(AccessLevel.NONE)
        private final boolean json5FeaturesEnabled;

        private final String tabCharacter;

        // Make it more fluent. :^)
        public boolean areJson5FeaturesEnabled() {
            return this.json5FeaturesEnabled;
        }

    }

}
