package org.jedio.feature;

import lombok.ToString;

@ToString(callSuper = true)
public class LongFeature extends GenericFeature<Long> implements Feature<Long> {
  public LongFeature(String name, String description, Long compileDefault, String propertyName, boolean realtime,
      boolean runtimeValueReadOnly)
  {
    super(name, description, compileDefault, propertyName, realtime, runtimeValueReadOnly);
  }
}
