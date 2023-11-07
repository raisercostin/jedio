package namek.util;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Documents that the annotated class/field/method is safe for concurrent access. */
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface threadsafe {}
