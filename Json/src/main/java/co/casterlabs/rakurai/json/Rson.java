package co.casterlabs.rakurai.json;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
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
import co.casterlabs.rakurai.json.serialization.JsonSerializationContext;
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
        .setPrettyPrintingEnabled(true)
        .build();

    private final RsonConfig settings;

    private Map<Class<?>, TypeResolver<?>> resolvers = new HashMap<>();

    private Rson(Builder builder) {
        this.settings = builder.toConfig();
        this.resolvers.putAll(builder.resolvers);
    }

    public String toJsonString(@Nullable Object o) {
        JsonElement e = this.toJson(o);

        JsonSerializationContext ctx = new JsonSerializationContext()
            .setConfig(this.settings);

        e.serialize(ctx);

        return ctx.toString();
    }

    public <T> JsonElement toJson(@Nullable T o) {
        if (o == null) {
            return JsonNull.INSTANCE;
        } else if (o instanceof JsonElement) {
            return (JsonElement) o;
        } else {
            @SuppressWarnings("unchecked")
            TypeResolver<T> resolver = (TypeResolver<T>) this.resolvers.get(o.getClass());

            if (resolver != null) {
                return resolver.writeOut(o, o.getClass());
            } else {
                try {
                    Class<?> clazz = o.getClass();

                    boolean isCollection = Collection.class.isAssignableFrom(clazz);
                    boolean isMap = Map.class.isAssignableFrom(clazz);
                    boolean isEnum = Enum.class.isAssignableFrom(clazz);

                    if (isCollection) {
                        JsonArray result = new JsonArray();

                        Collection<?> collection = (Collection<?>) o;

                        for (Object entry : collection) {
                            if (entry == null) {
                                result.addNull();
                            } else {
                                JsonElement serialized = this.toJson(entry);

                                result.add(serialized);
                            }
                        }

                        return result;
                    } else if (clazz.isArray()) {
                        JsonArray result = new JsonArray();

                        int len = Array.getLength(o);

                        for (int i = 0; i < len; i++) {
                            Object entry = Array.get(o, i);

                            if (entry == null) {
                                result.addNull();
                            } else {
                                JsonElement serialized = this.toJson(entry);

                                result.add(serialized);
                            }
                        }

                        return result;
                    } else if (isMap) {
                        JsonObject result = new JsonObject();

                        Map<?, ?> map = (Map<?, ?>) o;

                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            JsonElement key = this.toJson(entry.getKey());

                            if (key.isJsonString()) {
                                JsonElement value = this.toJson(entry.getValue());

                                result.put(key.getAsString(), value);
                            } else {
                                throw new JsonSerializeException("Map key must be either a String or Enum.");
                            }
                        }

                        return result;
                    } else if (isEnum) {
                        Enum<?> en = (Enum<?>) o;

                        return new JsonString(en.name());
                    } else {
                        JsonSerializer<?> serializer;

                        // Create the serializer, or supply a default.
                        {
                            JsonClass classData = clazz.getAnnotation(JsonClass.class);

                            if (classData != null) {
                                Class<? extends JsonSerializer<?>> serializerClass = classData.serializer();
                                Constructor<? extends JsonSerializer<?>> serializerConstructor = serializerClass.getConstructor();

                                serializerConstructor.setAccessible(true);

                                serializer = serializerConstructor.newInstance();
                            } else {
                                serializer = JsonSerializer.DEFAULT;
                            }
                        }

                        JsonElement result = serializer.serialize(o, this);

                        if (result.isJsonObject()) {
                            JsonObject dest = result.getAsObject();
                            Collection<JsonSerializerMethodImpl> serializerMethods = JsonReflectionUtil.getJsonSerializerMethodsForClass(clazz);

                            for (JsonSerializerMethodImpl m : serializerMethods) {
                                m.generate(o, dest);
                            }
                        }

                        return result;
                    }
                } catch (IllegalArgumentException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new JsonSerializeException(e);
                }
            }
        }
    }

    public <T> T fromJson(@NonNull String json, @NonNull Class<T> expected) throws JsonParseException, JsonValidationException {
        JsonElement e = JsonParser.parseString(json, this.settings);

        return this.fromJson(e, expected);
    }

    public <T> T fromJson(@NonNull String json, @NonNull TypeToken<T> token) throws JsonParseException, JsonValidationException {
        JsonElement e = JsonParser.parseString(json, this.settings);

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
        try {
            T result = this.fromJson0(e, token);
            Class<?> expected = token.getTypeClass();

            // Execute the deserializer methods.
            if (e.isJsonObject()) {
                JsonObject source = e.getAsObject();
                Collection<JsonDeserializerMethodImpl> serializerMethods = JsonReflectionUtil.getJsonDeserializerMethodsForClass(expected);

                for (JsonDeserializerMethodImpl m : serializerMethods) {
                    m.accept(result, source);
                }
            }

            // Validate. (These throw on error.)
            Collection<JsonValidatorImpl> validators = JsonReflectionUtil.getJsonValidatorsForClass(expected);
            for (JsonValidatorImpl v : validators) {
                v.validate(result);
            }

            return result;

        } catch (IllegalArgumentException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | InvocationTargetException | ClassNotFoundException ex) {
            throw new JsonSerializeException(ex);
        }
    }

    @SuppressWarnings({
            "unchecked"
    })
    private <T> T fromJson0(JsonElement e, TypeToken<T> token) throws JsonParseException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        // Null check.
        if (e.isJsonNull()) {
            return null;
        }

        Class<?> expected = token.getTypeClass();

        // Json elements.
        if (JsonElement.class.isAssignableFrom(expected)) {
            if (expected == JsonElement.class) {
                return (T) e;
            } else if (e.getClass() == expected) {
                return (T) e;
            } else {
                throw new JsonParseException(String.format("Expected a %s but got a %s\n%s", expected.getSimpleName(), e.getClass().getSimpleName(), e));
            }
        }

        // Type resolvers.
        TypeResolver<T> resolver = (TypeResolver<T>) this.resolvers.get(expected);
        if (resolver != null) {
            return resolver.resolve(e, expected);
        }

        // Parse enums.
        if (Enum.class.isAssignableFrom(expected)) {
            String name = e.getAsString();

            for (Object enC : expected.getEnumConstants()) {
                Enum<?> en = (Enum<?>) enC;

                if (en.name().equals(name)) {
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

        // Object deserialization.
        {
            JsonClass classData = expected.getAnnotation(JsonClass.class);
            JsonSerializer<?> serializer;

            // Create the deserializer, or supply a default.
            if (classData != null) {
                Class<? extends JsonSerializer<?>> serializerClass = classData.serializer();
                Constructor<? extends JsonSerializer<?>> serializerConstructor = serializerClass.getConstructor();

                serializerConstructor.setAccessible(true);

                serializer = serializerConstructor.newInstance();
            } else {
                serializer = JsonSerializer.DEFAULT;
            }

            return (T) serializer.deserialize(e, expected, this);
        }
    }

    @Data
    @Accessors(chain = true)
    public static class Builder {
        @Getter(AccessLevel.NONE)
        private boolean json5FeaturesEnabled = false;

        // Make it more fluent. :^)
        public boolean areJson5FeaturesEnabled() {
            return this.json5FeaturesEnabled;
        }

        private boolean prettyPrintingEnabled = false;

        private String tabCharacter = "    ";

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        private Map<Class<?>, TypeResolver<?>> resolvers = DefaultTypeResolvers.get();

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
            return new RsonConfig(this.json5FeaturesEnabled, this.prettyPrintingEnabled, this.tabCharacter);
        }

    }

    @Getter
    @AllArgsConstructor
    public static class RsonConfig {
        @Getter(AccessLevel.NONE)
        private boolean json5FeaturesEnabled = false;

        private boolean prettyPrintingEnabled = false;

        private String tabCharacter = "    ";

        // Make it more fluent. :^)
        public boolean areJson5FeaturesEnabled() {
            return this.json5FeaturesEnabled;
        }

    }

}
