package org.jedio.feature;

import lombok.ToString;

@ToString(callSuper = true)
public class IntegerFeature extends GenericFeature<Integer> implements Feature<Integer> {
  public IntegerFeature(String name, String description, Integer compileDefault, String propertyName, boolean realtime,
      boolean runtimeValueReadOnly)
  {
    super(name, description, compileDefault, propertyName, realtime, runtimeValueReadOnly);
  }
}
