package co.casterlabs.rakurai.json;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.element.JsonElement;
import lombok.NonNull;

public interface TypeResolver<T> {

    public @Nullable T resolve(@NonNull JsonElement value, @NonNull Class<?> type);

    public @Nullable JsonElement writeOut(@NonNull T value, @NonNull Class<?> type);

}
