package co.casterlabs.rakurai.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@ToString
public abstract class TypeToken<T> {

    private @Getter Type type;
    private @Getter Class<?> typeClass;
    private @Getter Type[] typeArguments = {};

    public TypeToken() {
        Type superclass = this.getClass().getGenericSuperclass();

        if (superclass instanceof Class) {
            throw new RuntimeException("TypeToken MUST be initialized with a type.");
        }

        this.type = getType(superclass);
        this.init();
    }

    private TypeToken(Type type) {
        this.type = type;
        this.init();
    }

    private void init() {
        if (this.type instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType) this.type;

            this.typeClass = (Class<?>) p.getRawType();
            this.typeArguments = p.getActualTypeArguments();
        } else {
            Class<?> c = (Class<?>) this.type;
            this.typeClass = c;
        }
    }

    private static Type getType(Type superclass) {
        if (superclass instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) superclass;
            Type[] types = pt.getActualTypeArguments();

            return types[0];
        } else {
            return superclass;
        }
    }

    public boolean isArrayType() {
        return this.typeClass.isArray();
    }

    public static <T> TypeToken<T> of(@NonNull Class<T> clazz) {
        return new TypeToken<T>(clazz) {
        };
    }

    public static <T> TypeToken<T> of(@NonNull Class<T> clazz, @NonNull Type... typeArguments) {
        TypeToken<T> tt = new TypeToken<T>(clazz) {
        };

        // Jank fix.
        tt.typeArguments = typeArguments;

        return tt;
    }

}
