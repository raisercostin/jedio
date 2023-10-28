package org.jedio.feature;

import lombok.ToString;

@ToString(callSuper = true)
public class EnumFeature<T extends Enum<T>> extends GenericFeature<T> implements Feature<T> {
  public EnumFeature(String name, String description, T compileDefault, String propertyName, boolean realtime,
      boolean runtimeValueReadOnly)
  {
    super(name, description, compileDefault, propertyName, realtime, runtimeValueReadOnly);
  }
  //
  //  @Override
  //  public T convertFromString(String stringValue, Class<T> clazz) {
  //    return Enum.valueOf(clazz, StringUtils.unwrap(stringValue, "\""));
  //  }
}
