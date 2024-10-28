package io.github.olufemithompson.multibeanconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as eligible for multiple distinct bean instances.
 * <br>
 * <br>
 * Applying @MultiBean allows creating multiple instances of the same class
 * with different configurations within the same application context.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MultiBean {

}
