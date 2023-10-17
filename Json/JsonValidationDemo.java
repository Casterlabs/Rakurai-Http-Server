package co.casterlabs.rakurai.json;

import java.util.List;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.validation.JsonValidate;
import lombok.ToString;

public class JsonValidationDemo {

    public static void main(String[] args) throws Exception {
        // Enable assertions programatically.
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);

        String json = "[{\"_id\": 1, \"name\": \"Casterlabs\"},{\"_id\": 2, \"name\": null}]";

        List<User> users = Rson.DEFAULT.fromJson(json, new TypeToken<List<User>>() {
        });

        System.out.println(users);
    }

    @ToString
    @JsonClass(exposeAll = true)
    public static final class User {
        public int _id;
        public String name;

        @JsonValidate
        private void validate() {
            assert this.name != null;
        }

    }

}
