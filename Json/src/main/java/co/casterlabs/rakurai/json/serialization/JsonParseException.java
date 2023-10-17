package co.casterlabs.rakurai.json.serialization;

import java.io.IOException;

import lombok.NonNull;

public class JsonParseException extends IOException {
    private static final long serialVersionUID = 898397259977630282L;

    public JsonParseException(@NonNull String reason) {
        super(reason);
    }

    public JsonParseException(@NonNull String reason, @NonNull Throwable cause) {
        super(reason, cause);
    }

    public JsonParseException(@NonNull Throwable cause) {
        super(cause);
    }

}
