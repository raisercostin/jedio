package org.jedio;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**Bug simplified */
class EclipseBugTypeInference {
  @Target({
      ElementType.ANNOTATION_TYPE,
      ElementType.METHOD,
      ElementType.FIELD,
      ElementType.TYPE,
      ElementType.PARAMETER })
  @Retention(RetentionPolicy.RUNTIME)
  @com.fasterxml.jackson.annotation.JacksonAnnotation
  //like com.fasterxml.jackson.databind.annotation.JsonDeserialize
  public @interface JsonDeserialize2 {
    public Class<? extends JsonDeserializer2> using() default JsonDeserializer2.class;
  }

  //like com.fasterxml.jackson.databind.JsonDeserializer
  public static abstract class JsonDeserializer2<T> {
  }

  @JsonDeserialize2(using = Foo1.BarBad.class)
  //compilation error      ^
  //Type mismatch: cannot convert from Class<EclipseBugTypeInference.Foo1.BarBad> to Class<? extends EclipseBugTypeInference.JsonDeserializer2>
  public interface Foo1<T> {
    public static class BarBad<T> extends JsonDeserializer2<T> {
    }
  }

  @JsonDeserialize2(using = BarGood.class)
  public interface Foo2<T> {
    public static class BarBad<T> extends JsonDeserializer2<T> {
    }
  }

  public static class BarGood<T> extends JsonDeserializer2<T> {
  }
}
