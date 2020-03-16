package org.raisercostin.jedio.op;

import java.time.Duration;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.With;
import org.raisercostin.jedio.ExistingLocation;

public interface CopyOptions {
  static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CopyOptions.class);

  Duration timeoutOnItem = Duration.ofSeconds(1);
  Duration timeoutTotal = Duration.ofSeconds(60);
  boolean reportSteps = false;

  boolean replaceExisting();

  default boolean makeDirIfNeeded() {
    return true;
  }

  @FunctionalInterface
  public static interface OperationListener {
    void reportOperationEvent(String event, ExistingLocation item);
  }

  @Data
  @Getter(value = AccessLevel.NONE)
  @Setter(value = AccessLevel.NONE)
  @With
  public class SimpleCopyOptions implements CopyOptions {
    public final boolean replaceExisting;
    public final OperationListener operationListener;

    @Override
    public boolean replaceExisting() {
      return replaceExisting;
    }

    public CopyOptions withDefaultReporting() {
      return this.withOperationListener((event, item) -> log.info("copy {}: {}", event, item));
    }

    @Override
    public boolean reportSteps() {
      return operationListener != null;
    }

    @Override
    public void reportOperationEvent(String event, ExistingLocation item) {
      operationListener.reportOperationEvent(event, item);
    }
  }
  // case class CopyOptions(overwriteIfAlreadyExists: Boolean = false, copyMeta:
  // Boolean, optionalMeta: Boolean, monitor: OperationMonitor =
  // LoggingOperationMonitor)

  public static SimpleCopyOptions copyDefault() {
    return copyDoNotOverwrite();
  }

  public static SimpleCopyOptions copyDoNotOverwrite() {
    return new SimpleCopyOptions(false, null);
  }

  public static SimpleCopyOptions copyOverwrite() {
    return new SimpleCopyOptions(true, null);
  }

  default Duration timeoutOnItem() {
    return timeoutOnItem;
  }

  default Duration timeoutTotal() {
    return timeoutTotal;
  }

  default boolean reportSteps() {
    return reportSteps;
  }

  default void reportOperationEvent(String event, ExistingLocation item) {
  }
}
