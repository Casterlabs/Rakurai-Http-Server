package co.casterlabs.rakurai.json;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.JsonClass;
import co.casterlabs.rakurai.json.JsonField;
import co.casterlabs.rakurai.json.JsonSerializer;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.TypeResolver;
import co.casterlabs.rakurai.json.Test.TestObject;
import co.casterlabs.rakurai.json.Test.TestObjectDeux;
import co.casterlabs.rakurai.json.Test.TestObjectDeuxSerializer;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.element.JsonString;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.NonNull;
import lombok.ToString;

public class JsonDemo {
    private static final Rson RSON = new Rson.Builder()
        .registerTypeResolver(new TypeResolver<UUID>() {
            @Override
            public @Nullable UUID resolve(@NonNull JsonElement value, @NonNull Class<?> type) {
                return UUID.fromString(value.getAsString());
            }

            @Override
            public @Nullable JsonElement writeOut(@NonNull UUID value, @NonNull Class<?> type) {
                return new JsonString(value.toString());
            }
        }, UUID.class)
        .build();

    public static void main(String[] args) throws Exception {
        // We do a false run to allow the JVM to warm up.
        demoSerialization();
        demoDeserialization();

        System.out.println("\n\nIgnore the previous text, that was a warmup.\n\n");

        long serStart = System.currentTimeMillis();
        demoSerialization();
        long serEnd = System.currentTimeMillis();

        long deserStart = System.currentTimeMillis();
        demoDeserialization();
        long deserEnd = System.currentTimeMillis();

        System.out.printf("\n\n Serialization demo took %d ms, Deserialization demo took %d ms.", serEnd - serStart, deserEnd - deserStart);
    }

    public static void demoSerialization() throws Exception {
        System.out.println("Objects are easily serialized.");

        TestObject test = new TestObject();

        System.out.println(RSON.toJson(test));

        //

        System.out.println("\nIt even works for arrays and collections...");

        System.out.println(RSON.toJson(Arrays.asList(test)));

        //

        System.out.println("\nMaps work as well.");

        Map<String, TestObject> test3 = Collections.singletonMap("test3", test);

        System.out.println(RSON.toJson(test3));
    }

    public static void demoDeserialization() throws Exception {
        System.out.println("\nWe can also deserialize back into a TestObject.");

        // It can also handle missing values, a non present value won't do anything.
        // A null value will set the field to null (or 0 or false, for primitives)
        String json = "{\"my_string\": \"My String\",\"my_string_two\": \"My String Two\", \"uuid\": \"d8f68c5e-deff-41d6-8acb-37d1cdebdaeb\"}";

        System.out.println(json);

        TestObject deser_test = RSON.fromJson(json, TestObject.class);

        System.out.println(deser_test);

        //

        System.out.println("\nDeserializing into arrays also works.");

        String json2 = "[{\"my_string\": \"My String\",\"my_string_two\": \"My String Two\", \"uuid\": \"d8f68c5e-deff-41d6-8acb-37d1cdebdaeb\"}]";

        System.out.println(json2);

        TestObject[] deser_test2 = RSON.fromJson(json2, TestObject[].class);

        System.out.println(Arrays.toString(deser_test2));
    }

    @ToString
    public static class TestObject {
        // Fields must be exposed via @JsonField or @JsonClass(exposeAll = true),
        // otherwise they will be left out entirely.
        @JsonField
        private int number = 1234;

        // You can register type adapters to serialize types.
        @JsonField
        private UUID uuid = UUID.randomUUID();

        // You can construct the annotation with a preferred name,
        // it'll be respected during serialization and deserialization.
        @JsonField("my_string")
        private String myString = "I love Rakurai <3";

        @JsonField("my_string_two")
        private TestObjectDeux deux = new TestObjectDeux();

    }

    // Using lombok's ToString annotation for convenience.
    @ToString
    @JsonClass(serializer = TestObjectDeuxSerializer.class)
    public static class TestObjectDeux {
        private String str = "I also love Rakurai <3";

    }

    public static class TestObjectDeuxSerializer implements JsonSerializer<TestObjectDeux> {

        @Override
        public JsonElement serialize(@NonNull Object value, @NonNull Rson rson) {
            // You can override the serialization and deserialization
            // with your own custom implementation.
            return new JsonString(((TestObjectDeux) value).str);
        }

        @Override
        public @Nullable TestObjectDeux deserialize(@NonNull JsonElement value, @NonNull Class<?> type, @NonNull Rson rson) throws JsonParseException {
            // Here we reconstruct it for the deserialization example.
            TestObjectDeux t = new TestObjectDeux();

            t.str = value.getAsString();

            return t;
        }

    }

}
