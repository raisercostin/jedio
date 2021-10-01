package org.jedio.feature;

import io.vavr.control.Option;
import org.raisercostin.nodes.Nodes;

/**The value of the feature is evaluated in the following order:
1. dynamic - persisted/database overwrite
2. run time - system properties overwrite
3. run time - environment overwrite
4. compile time value
*/
public interface Feature<T> {
  String name();

  T compileTimeValue();

  @SuppressWarnings("unchecked")
  default T convert(Object value) {
    if (value == null) {
      return null;
    }
    Class<T> clazz = (Class<T>) compileTimeValue().getClass();
    if (clazz.isAssignableFrom(value.getClass())) {
      return (T) value;
    }
    String stringValue = Nodes.json.toString(value);
    return convertFromString(stringValue, clazz);
    //T result = Nodes.json.toObject(stringValue, clazz);
    //return result;
    //    T result = (T) ConvertUtils.convert(value, clazz);
    //    if (clazz.isAssignableFrom(result.getClass())) {
    //      return result;
    //    }
    //    throw new RuntimeException("Couldn't convert value [" + value + "] to [" + clazz + "]. Result was [" + result
    //        + "] of type " + result.getClass());
  }

  default T convertFromString(String stringValue, Class<T> clazz) {
    return Nodes.json.toObject(stringValue, clazz);
  }

  default T value() {
    return runtimeValue().getOrElse(beforeRuntime());
  }

  default T beforeRuntime() {
    return startValue().getOrElse(compileTimeValue());
  }

  default Option<T> startValue() {
    return Option.none();
  }

  default Option<T> runtimeValue() {
    return Option.none();
  }

  default String runtimeProperty() {
    return "";
  }

  default void setRuntimeValue(T v) {
  }

  default void doAction(String action) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default String description() {
    return toString();
  }

  /**Reinitialization if needed*/
  default void postConstruct() {
  }
}
