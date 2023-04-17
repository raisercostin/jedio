package org.jedio.feature;

import lombok.ToString;
import org.slf4j.Logger;

@ToString(callSuper = true)
public class BooleanFeature extends GenericFeature<Boolean> implements Feature<Boolean> {
  public BooleanFeature(String name, String description, Boolean compileDefault, String propertyName,
      boolean realtime)
  {
    super(name, description, compileDefault, propertyName, realtime, false);
  }

  public boolean isEnabled() {
    return value();
  }

  public Logger log(Class<?> clazz) {
    return org.slf4j.LoggerFactory.getLogger(clazz);
  }

  public void enable() {
    setRuntimeValue(true);
  }

  public void disable() {
    setRuntimeValue(false);
  }
}
