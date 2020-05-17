package org.jedio;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A program element annotated &#64;Deprecated is one that programmers
 * are discouraged from using, typically because it is dangerous,
 * or because a better alternative exists.  Compilers warn when a
 * deprecated program element is used or overridden in non-deprecated code.
 *
 * @author  Neal Gafter
 * @since 1.5
 * @jls 9.6.3.6 @Deprecated
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PACKAGE, PARAMETER, TYPE })
public @interface deprecated {
  //the same as message
  String value() default "";

  String message() default "";

  String since() default "";
}
