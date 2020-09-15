package org.jedio;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to show that is just syntax sugar. Should just make some conversions and then delegate to the heavy
 * methods.
 */
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface sugar {
  // the same as message
  String value() default "";

  String message() default "";
}
