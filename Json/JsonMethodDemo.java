package co.casterlabs.rakurai.json;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.annotating.JsonDeserializationMethod;
import co.casterlabs.rakurai.json.annotating.JsonExclude;
import co.casterlabs.rakurai.json.annotating.JsonSerializationMethod;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonString;
import co.casterlabs.rakurai.json.validation.JsonValidate;
import lombok.NonNull;
import lombok.ToString;

public class JsonMethodDemo {

    public static void main(String[] args) throws Exception {
        // Enable assertions programatically.
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);

        // Here we have a list of users with String ids, in Java we want them as ints.
        String json = "[{\"_id\": \"1\", \"name\": \"Casterlabs\"},{\"_id\": \"2\" \"name\": null}]";

        List<User> users = Rson.DEFAULT.fromJson(json, new TypeToken<List<User>>() {
        });

        System.out.println(users);
        System.out.println(Rson.DEFAULT.toJsonString(users)); // To show that we can modify the output with methods.
    }

    @ToString
    @JsonClass(exposeAll = true)
    public static final class User {
        // So Rson won't try to serialize the type.
        public @JsonExclude int _id;
        public String name;

        // The higher the weight the sooner it'll be executed.
        // In our case, we want the ID to get deserialized before
        // the name (so we can generate a default based on the ID)
        @JsonDeserializationMethod(value = "_id", weight = 1)
        private void _jsonId(@NonNull JsonElement e) {
            // Exceptions will be caught and rethrown as JsonParseException.
            this._id = Integer.parseInt(e.getAsString());
        }

        @JsonSerializationMethod("_id")
        private @Nullable JsonElement _jsonId() {
            // Keep the type as JsonString in the json.
            return new JsonString(String.valueOf(this._id));
        }

        @JsonDeserializationMethod("name")
        private void _jsonName(@NonNull JsonElement e) {
            // If it's JsonNull this'll return false,
            // and it's guaranteed to not be null.
            if (e.isJsonString()) {
                this.name = e.getAsString();
            } else {
                this.name = String.format("New User (no. %s)", this._id);
            }
        }

        // JsonValidate will be executed after the methods.
        // If needed, the methods can also do their own validation and throw an error.
        @JsonValidate
        private void validate() {
            assert this.name != null;
        }

    }

}
