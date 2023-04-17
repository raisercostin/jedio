package org.jedio.feature;

import lombok.ToString;

@ToString(callSuper = true)
public class TextFeature extends GenericFeature<String> implements Feature<String> {
  public TextFeature(String name, String description, String compileDefault, String propertyName, boolean realtime,
      boolean runtimeValueReadOnly)
  {
    super(name, description, compileDefault, propertyName, realtime, runtimeValueReadOnly);
  }
}
