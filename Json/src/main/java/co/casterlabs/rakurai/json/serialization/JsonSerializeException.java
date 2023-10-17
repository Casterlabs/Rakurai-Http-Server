package co.casterlabs.rakurai.json.serialization;

import lombok.NonNull;

public class JsonSerializeException extends RuntimeException {
    private static final long serialVersionUID = -3251245129018955799L;

    public JsonSerializeException(@NonNull String reason) {
        super(reason);
    }

    public JsonSerializeException(@NonNull Throwable e) {
        super(e);
    }

}
