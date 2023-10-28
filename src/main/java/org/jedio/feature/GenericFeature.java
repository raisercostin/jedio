package org.jedio.feature;

import java.time.Duration;
import java.util.function.BiConsumer;

import ch.qos.logback.classic.Level;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.vavr.CheckedFunction2;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.ToString;
import org.jedio.BiCheckedConsumer;
import org.raisercostin.nodes.Nodes;
import reactor.core.Disposable;

@ToString
public class GenericFeature<T> implements Feature<T> {
  private static final Config config = ConfigFactory.load("revobet");

  public static <T> GenericFeature<T> create(String name, String description, T compileDefault, String propertyName,
      boolean realtime) {
    return create(name, description, compileDefault, propertyName, realtime, false);
  }

  public static <T> GenericFeature<T> create(String name, String description, T compileDefault, String propertyName,
      boolean realtime, boolean runtimeReadonly) {
    return new GenericFeature<>(name, description, compileDefault, propertyName, realtime, runtimeReadonly);
  }

  public static DurationFeature duration(String name, String description, Duration compileDefault, String propertyName,
      boolean realtime) {
    return new DurationFeature(name, description, compileDefault, propertyName, realtime);
  }

  public static BooleanFeature booleanFeature(String name, String description, Boolean compileDefault,
      String propertyName,
      boolean realtime) {
    return new BooleanFeature(name, description, compileDefault, propertyName, realtime);
  }

  public static BooleanFeature booleanFeature(String name, Boolean compileDefault,
      String propertyName, boolean realtime) {
    return booleanFeature(name, "", compileDefault, propertyName, realtime);
  }

  public static LoggerFeature loggerFeature(String name, Boolean compileDefault, String propertyName,
      boolean realtime, String loggerName) {
    return LoggerFeature.loggerFeature(name, "", compileDefault, propertyName, realtime, loggerName);
  }

  public static LoggerFeature loggerFeature(String name, Boolean compileDefault, String propertyName,
      boolean realtime, String loggerName, Level inactiveLevel, Level activeLevel) {
    return LoggerFeature.loggerFeature(name, "", compileDefault, propertyName, realtime, loggerName, inactiveLevel,
      activeLevel);
  }

  public static TextFeature text(String name, String description, String compileDefault,
      String propertyName) {
    return new TextFeature(name, description, compileDefault, propertyName, false, false);
  }

  public static TextFeature text(String name, String description, String compileDefault,
      String propertyName, boolean realtime) {
    return new TextFeature(name, description, compileDefault, propertyName, realtime, false);
  }

  public static ActionFeature actionFeature(String name, String description, String... actions) {
    return new ActionFeature(name, description, actions);
  }

  public String name;
  public String description;
  public T compileValue;
  public Option<T> startValue;
  public Option<T> runtimeValue;
  public Option<T> oldRuntimeValue;
  /**If this feature can be influenced in realtime or will be available on restart.*/
  public boolean realtime;
  public String propertyName;
  public boolean runtimeValueReadOnly;
  private CheckedFunction2<T, T, T> onChange;
  private int counter;

  public GenericFeature(String name, String description, T compileDefault, String propertyName, boolean realtime,
      boolean runtimeValueReadOnly)
  {
    this.name = name;
    this.description = description;
    this.compileValue = compileDefault;
    String propertyNameCorrected = propertyName != null && propertyName.isEmpty() ? null : propertyName;
    this.propertyName = propertyNameCorrected;
    this.startValue = propertyNameCorrected != null && config.hasPath(propertyNameCorrected)
        ? Option.of(Try.of(() -> convert(config.getAnyRef(propertyNameCorrected)))
          .onFailure(
            x -> x.addSuppressed(new RuntimeException("while trying to get [" + propertyNameCorrected + "]")))
          .get())
        : Option.none();
    this.runtimeValue = Option.none();
    this.realtime = realtime;
    this.runtimeValueReadOnly = runtimeValueReadOnly;
    FeatureService.register(this);
  }

  @Override
  public String description() {
    return Strings.lenientFormat("%s(%s/%s with value [%s]. atCompile=[%s]  atStart=[%s] atRuntime=[%s], desc=[%s])",
      getClass().getSimpleName(), name(), runtimeProperty(), value(), compileTimeValue(), startValue(),
      runtimeValue(), description);
  }

  public String shortDescription() {
    return Strings.lenientFormat("feature(%s=%s)", name(), value());
  }

  @Override
  public String runtimeProperty() {
    return propertyName;
  }

  @Override
  public String name() {
    if (counter > 0) {
      return name + "#" + counter;
    }
    return name;
  }

  @Override
  public T compileTimeValue() {
    return compileValue;
  }

  @Override
  public Option<T> startValue() {
    return startValue;
  }

  @Override
  public Option<T> runtimeValue() {
    return runtimeValue;
  }

  public void setRuntimeValueForcedForTests(T runtimeValue) {
    if (!Objects.equal(this.runtimeValue, runtimeValue)
        || !equalsAsStrings(this.runtimeValue.getOrNull(), runtimeValue)) {
      T convertedValue = convert(runtimeValue);
      //TODO should compare with beforeRuntime?: && !convertedValue.equals(this.value())
      if (!Objects.equal(convertedValue, this.runtimeValue.getOrNull()) && !this.value().equals(convertedValue)) {
        Option<T> oldRuntimeValue = this.runtimeValue;
        T newValue = convertedValue;
        FeatureService.SERVICE_LOG.info("{}={} converted as [{}] oldValue={} for feature {}", name(), newValue,
          convertedValue,
          oldRuntimeValue, this);
        this.oldRuntimeValue = oldRuntimeValue;
        this.runtimeValue = Option.of(newValue);
        if (onChange != null) {
          try {
            T amededNewValue = onChange.apply(oldRuntimeValue.getOrNull(), newValue);
            if (newValue != amededNewValue) {
              this.runtimeValue = Option.of(amededNewValue);
            }
          } catch (Throwable e) {
            throw org.jedio.RichThrowable.nowrap(e);
          }
        }
      }
    }
  }

  @Override
  public void setRuntimeValue(T runtimeValue) {
    if (!runtimeValueReadOnly) {
      setRuntimeValueForcedForTests(runtimeValue);
    } else {
      FeatureService.SERVICE_LOG.debug("ignoring setRuntimeValue({}) for feature {}", runtimeValue, this);
    }
  }

  private boolean equalsAsStrings(T value1, T value2) {
    return Nodes.json.toString(value1).equals(Nodes.json.toString(value2));
  }

  public void registerNewCallbackOnChange(boolean call, BiConsumer<T, T> onChange) {
    registerNewCallbackOnChange(null, call, onChange);
  }

  public void registerNewCallbackOnChange(Disposable oldDisposable, boolean call, BiConsumer<T, T> onChange) {
    registerOnChange(oldDisposable, (from, to) -> {
      onChange.accept(from, to);
      return to;
    });
    if (call) {
      onChange.accept(value(), value());
    }
  }

  public void registerAction(Disposable oldDisposable, BiCheckedConsumer<T, T> onChange) {
    registerOnChange(oldDisposable, (from, to) -> {
      onChange.accept(from, to);
      return to;
    });
  }

  public void registerAction(BiCheckedConsumer<T, T> onChange) {
    registerOnChange(null, (from, to) -> {
      onChange.accept(from, to);
      return to;
    });
  }

  public void registerOnChange(Disposable oldDisposable, CheckedFunction2<T, T, T> onChange) {
    registerOnChangeFunction(onChange);
  }

  public void registerNewCallbackOnChange(BiCheckedConsumer<T, T> onChange) {
    registerNewCallbackOnChange(null, onChange);
  }

  public void registerNewCallbackOnChange(Disposable oldDisposable, BiCheckedConsumer<T, T> onChange) {
    registerOnChangeFunction((from, to) -> {
      onChange.accept(from, to);
      return to;
    });
  }

  private void registerOnChangeFunction(CheckedFunction2<T, T, T> onChange) {
    //      Preconditions.checkArgument(oldDisposable == null || oldDisposable == this.onChange,
    //        "On %s - The oldDisposable %s is different than the given one %s", this, this.onChange, oldDisposable);
    //      Preconditions.checkArgument(onChange == null || onChange == this.onChange,
    //        "On %s - The oldDisposable %s is different than the given one %s", this, this.onChange, oldDisposable);
    this.onChange = onChange;
  }

  @Override
  public void incrementName() {
    counter++;
  }
}
