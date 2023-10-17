package co.casterlabs.rakurai.json.annotating;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface JsonSerializationMethod {

    String value() default "";

    int weight() default 0;

}
