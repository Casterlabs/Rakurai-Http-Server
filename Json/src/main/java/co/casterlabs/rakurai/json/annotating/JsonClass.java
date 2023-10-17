package co.casterlabs.rakurai.json.annotating;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import co.casterlabs.rakurai.json.DefaultJsonSerializer;

@Retention(RUNTIME)
@Target(TYPE)
public @interface JsonClass {

    Class<? extends JsonSerializer<?>> serializer() default DefaultJsonSerializer.class;

    boolean exposeAll() default false;

    boolean exposeSuper() default false;

}
