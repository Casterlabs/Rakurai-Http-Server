package co.casterlabs.rakurai.json.validation;

import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.NonNull;

public class JsonValidationException extends JsonParseException {
    private static final long serialVersionUID = 7738056083575239025L;

    public JsonValidationException(@NonNull String reason) {
        super(reason);
    }

    public JsonValidationException(@NonNull String reason, @NonNull Throwable cause) {
        super(reason, cause);
    }

    public JsonValidationException(@NonNull Throwable cause) {
        super(cause);
    }

}
