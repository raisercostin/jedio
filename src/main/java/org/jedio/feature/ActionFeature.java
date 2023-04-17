package org.jedio.feature;

import io.vavr.control.Option;
import lombok.ToString;
import org.jedio.struct.RichIterable;

@ToString(callSuper = true)
public final class ActionFeature extends GenericFeature<String> implements Feature<String> {
  private String[] actions;
  private Option<String> defaultValue;

  public ActionFeature(String name, String description, String... actions) {
    super(name, description, "", "", true, false);
    this.actions = actions;
    this.defaultValue = Option.of("Choose from " + RichIterable.of(actions).mkString(","));
    revertBack();
  }

  @Override
  public void doAction(String action) {
    this.setRuntimeValue(action);
    revertBack();
  }

  public void revertBack() {
    this.runtimeValue = defaultValue;
  }
}
