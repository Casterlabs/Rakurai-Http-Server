package co.casterlabs.rakurai.json;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.annotating.JsonDeserializationMethod;
import co.casterlabs.rakurai.json.annotating.JsonExclude;
import co.casterlabs.rakurai.json.annotating.JsonField;
import co.casterlabs.rakurai.json.annotating.JsonSerializationMethod;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonNull;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.rakurai.json.serialization.JsonSerializeException;
import co.casterlabs.rakurai.json.validation.JsonValidate;
import co.casterlabs.rakurai.json.validation.JsonValidationException;
import lombok.NonNull;
import lombok.SneakyThrows;

class JsonReflectionUtil {

    /* -------------------- */
    /* Classes              */
    /* -------------------- */

    static <T> T newInstance(Class<T> clazz) throws InstantiationException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // Find a no-args constructor and make it accessible.
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);

        return constructor.newInstance();
    }

    static @Nullable Class<?> getCollectionComponent(Class<?> clazz) throws ClassNotFoundException {
        if (clazz.isArray()) {
            return clazz.getComponentType();
        }

        if (Collection.class.isAssignableFrom(clazz)) {
            Type type = clazz.getTypeParameters()[0].getBounds()[0];

            return typeToClass(type, clazz.getClassLoader());
        }

        return null;
    }

    static Class<?>[] getCollectionComponentForField(Field field) throws ClassNotFoundException {
        Class<?> type = field.getType();

        if (type.isArray()) {
            return new Class<?>[] {
                    type.getComponentType()
            };
        }

        if (Collection.class.isAssignableFrom(type)) {
            ParameterizedType genericType = (ParameterizedType) field.getGenericType();
            Type parameter = genericType.getActualTypeArguments()[0];

            return new Class<?>[] {
                    typeToClass(parameter, field.getDeclaringClass().getClassLoader())
            };
        }

        if (Map.class.isAssignableFrom(type)) {
            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            Type[] typeArguments = pt.getActualTypeArguments();

            return new Class<?>[] {
                    typeToClass(typeArguments[0], field.getDeclaringClass().getClassLoader()),
                    typeToClass(typeArguments[1], field.getDeclaringClass().getClassLoader())
            };
        }

        return new Class<?>[0];
    }

    @SneakyThrows // We assume that no error will be thrown, ever.
    static Class<?> typeToClass(@NonNull Type type, @Nullable ClassLoader cl) {
        if (type instanceof Class) {
            return (Class<?>) type; // Sometimes Java actually gives us a Class<?>!
        }

        return Class.forName(type.getTypeName(), false, cl);
    }

    /* -------------------- */
    /* Annotating           */
    /* -------------------- */

    static @Nullable Collection<JsonValidatorImpl> getJsonValidatorsForClass(Class<?> clazz) {
        List<JsonValidatorImpl> validators = new LinkedList<>();

        for (Method method : getAllDeclaredMethods(clazz)) {
            // Must be void() marked with @JsonValidate
            if (!method.isAnnotationPresent(JsonValidate.class)) continue;
            if (method.getReturnType() != Void.TYPE) continue;
            if (method.getParameterCount() != 0) continue;

            try {
                method.setAccessible(true);
            } catch (Exception ignored) {} // Swallow the error and continue about our day.

            validators.add((@NonNull Object inst) -> {
                try {
                    method.invoke(inst);
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    // Ignore it.
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause == null) return; // Ignore?

                    if (cause instanceof JsonValidationException) {
                        throw (JsonValidationException) cause; // Rethrow.
                    }

                    throw new JsonValidationException(cause);
                }
            });
        }

        return validators;
    }

    static @Nullable Collection<JsonDeserializerMethodImpl> getJsonDeserializerMethodsForClass(Class<?> clazz) {
        List<JsonDeserializerMethodImpl> deserializerMethods = new LinkedList<>();

        for (Method method : getAllDeclaredMethods(clazz)) {
            // Must be void(JsonElement) marked with @JsonDeserializationMethod.
            if (!method.isAnnotationPresent(JsonDeserializationMethod.class)) continue;
            if (method.getReturnType() != Void.TYPE) continue;
            if (method.getParameterCount() != 1) continue;
            if (!JsonElement.class.isAssignableFrom(method.getParameterTypes()[0])) continue;

            try {
                method.setAccessible(true);
            } catch (Exception ignored) {} // Swallow the error and continue about our day.

            JsonDeserializationMethod annotation = method.getAnnotation(JsonDeserializationMethod.class);
            deserializerMethods.add(new JsonDeserializerMethodImpl() {
                @Override
                public JsonDeserializationMethod getAnnotation() {
                    return annotation;
                }

                @Override
                public void accept(@NonNull Object inst, @NonNull JsonObject source) throws JsonParseException {
                    try {
                        JsonElement elem = source.get(annotation.value());
                        if (elem == null) return; // Not present.

                        method.invoke(inst, elem);
                    } catch (IllegalAccessException | IllegalArgumentException e) {
                        // Ignore it.
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause == null) return; // Ignore?

                        if (cause instanceof JsonParseException) {
                            throw (JsonParseException) cause; // Rethrow.
                        }

                        throw new JsonParseException(cause);
                    }
                }
            });
        }

        // Sort by weight (execution priority).
        deserializerMethods.sort((o1, o2) -> {
            return o1.getAnnotation().weight() < o2.getAnnotation().weight() ? 1 : -1;
        });

        return deserializerMethods;
    }

    static @Nullable Collection<JsonSerializerMethodImpl> getJsonSerializerMethodsForClass(Class<?> clazz) {
        List<JsonSerializerMethodImpl> serializerMethods = new LinkedList<>();

        for (Method method : getAllDeclaredMethods(clazz)) {
            // Must be JsonElement() marked with @JsonSerializationMethod.
            if (!method.isAnnotationPresent(JsonSerializationMethod.class)) continue;
            if (method.getReturnType() != JsonElement.class) continue;
            if (method.getParameterCount() != 0) continue;

            try {
                method.setAccessible(true);
            } catch (Exception ignored) {} // Swallow the error and continue about our day.

            JsonSerializationMethod annotation = method.getAnnotation(JsonSerializationMethod.class);
            serializerMethods.add(new JsonSerializerMethodImpl() {
                @Override
                public JsonSerializationMethod getAnnotation() {
                    return annotation;
                }

                @Override
                public void generate(@NonNull Object inst, @NonNull JsonObject dest) throws JsonSerializeException {
                    try {
                        JsonElement result = (JsonElement) method.invoke(inst);

                        if (result == null) {
                            result = JsonNull.INSTANCE;
                        }

                        dest.put(annotation.value(), result);
                    } catch (IllegalAccessException | IllegalArgumentException e) {
                        // Ignore it.
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause == null) return; // Ignore?

                        if (cause instanceof JsonSerializeException) {
                            throw (JsonSerializeException) cause; // Rethrow.
                        }

                        throw new JsonSerializeException(cause);
                    }
                }
            });
        }

        // Sort by weight (execution priority).
        serializerMethods.sort((o1, o2) -> {
            return o1.getAnnotation().weight() < o2.getAnnotation().weight() ? 1 : -1;
        });

        return serializerMethods;
    }

    /* -------------------- */
    /* Wrappers             */
    /* -------------------- */

    static interface JsonValidatorImpl {

        public void validate(@NonNull Object inst) throws JsonValidationException;

    }

    static interface JsonDeserializerMethodImpl {

        public JsonDeserializationMethod getAnnotation();

        public void accept(@NonNull Object inst, @NonNull JsonObject source) throws JsonParseException;

    }

    static interface JsonSerializerMethodImpl {

        public JsonSerializationMethod getAnnotation();

        public void generate(@NonNull Object inst, @NonNull JsonObject dest) throws JsonSerializeException;

    }

    /* -------------------- */
    /* Method lookup        */
    /* -------------------- */

    static List<Method> getAllDeclaredMethods(Class<?> clazz) {
        List<Method> methods = new LinkedList<>();

        getAllDeclaredMethods0(methods, clazz);

        return methods;
    }

    static void getAllDeclaredMethods0(List<Method> methods, Class<?> clazz) {
        Method[] declared = clazz.getDeclaredMethods();

        for (Method m : declared) {
            methods.add(m);
        }

        Class<?> superClass = clazz.getSuperclass();

        if (superClass != null) {
            getAllDeclaredMethods0(methods, superClass);
        }
    }

    /* -------------------- */
    /* Field lookup         */
    /* -------------------- */

    static Map<String, Field> getFields(Class<?> type) {
        Map<String, Field> fields = new HashMap<>();
        List<Class<?>> toScan = new LinkedList<>();

        Class<?> currentClass = type;
        while (currentClass != null) {
            boolean exposeSuper = false;

            if (currentClass.isAnnotationPresent(JsonClass.class)) {
                JsonClass classAnnotation = currentClass.getAnnotation(JsonClass.class);

                exposeSuper = classAnnotation.exposeSuper();
            }

            if (exposeSuper) {
                toScan.addAll(Arrays.asList(currentClass.getInterfaces()));

                if (currentClass.getSuperclass() != null) {
                    toScan.add(currentClass.getSuperclass());
                }
            }

            toScan.add(currentClass);

            currentClass = currentClass.getSuperclass();
        }

        for (Class<?> clazz : toScan) {
            boolean exposeAll = false;

            if (clazz.isAnnotationPresent(JsonClass.class)) {
                JsonClass classAnnotation = clazz.getAnnotation(JsonClass.class);

                exposeAll = classAnnotation.exposeAll();
            }

            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.isAnnotationPresent(JsonExclude.class)) {
                    if (exposeAll || field.isAnnotationPresent(JsonField.class)) {
                        String fieldName = field.getName();

                        if (field.isAnnotationPresent(JsonField.class)) {
                            JsonField fieldAnnotation = field.getAnnotation(JsonField.class);

                            if (!fieldAnnotation.value().isEmpty()) {
                                fieldName = fieldAnnotation.value();
                            }
                        }

                        fields.put(fieldName, field);
                    }
                }
            }
        }

        return fields;
    }

}
