package org.raisercostin.jedio.op;

import java.time.Duration;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.With;
import org.raisercostin.jedio.ExistingLocation;
import org.raisercostin.jedio.MetaInfo.StreamAndMeta;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.WritableFileLocation;

public interface CopyOptions {
  org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CopyOptions.class);

  Duration timeoutOnItem = Duration.ofSeconds(1);
  Duration timeoutTotal = Duration.ofSeconds(60);
  boolean reportSteps = false;

  boolean replaceExisting();

  boolean copyMeta();

  default boolean makeDirIfNeeded() {
    return true;
  }

  @FunctionalInterface
  public interface OperationListener {
    void reportOperationEvent(CopyEvent event, Throwable exception, ExistingLocation<?> src, ReferenceLocation<?> dst,
        Object... args);
  }

  @Data
  @Getter(value = AccessLevel.NONE)
  @Setter(value = AccessLevel.NONE)
  @With
  public class SimpleCopyOptions implements CopyOptions {
    public final boolean replaceExisting;
    public final OperationListener operationListener;
    public final boolean copyMeta;

    @Override
    public boolean replaceExisting() {
      return replaceExisting;
    }

    public CopyOptions withDefaultReporting() {
      return this.withOperationListener(
        (event, exception, src, dst, args) -> log.info("copy {}: {} -> {} details:{}", event, src, dst, args,
          exception));
    }

    @Override
    public boolean reportSteps() {
      return operationListener != null;
    }

    @Override
    public void reportOperationEvent(CopyEvent event, ExistingLocation<?> src, ReferenceLocation<?> dst,
        Object... args) {
      operationListener.reportOperationEvent(event, null, src, dst, args);
    }

    @Override
    public void reportOperationEvent(CopyEvent event, Throwable exception, ExistingLocation<?> src,
        ReferenceLocation<?> dst,
        Object... args) {
      operationListener.reportOperationEvent(event, exception, src, dst, args);
    }

    @Override
    public boolean copyMeta() {
      return copyMeta;
    }
  }
  // case class CopyOptions(overwriteIfAlreadyExists: Boolean = false, copyMeta:
  // Boolean, optionalMeta: Boolean, monitor: OperationMonitor =
  // LoggingOperationMonitor)

  static SimpleCopyOptions copyDefault() {
    return copyDoNotOverwrite();
  }

  static SimpleCopyOptions copyDoNotOverwrite() {
    return new SimpleCopyOptions(false, null, true);
  }

  static SimpleCopyOptions copyOverwrite() {
    return new SimpleCopyOptions(true, null, true);
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

  public enum CopyEvent {
    Unknown,
    CopyFileTriggered("Copy file triggered."),
    IgnoreSourceDoesNotExists,
    IgnoreDestinationMetaExists,
    IgnoreDestinationExists,
    CopyFileStarted,
    CopyReplacing("A replace of content started"),
    CopyFileFinished,
    CopyFailed,
    CopyDirStarted,
    CopyDirFinished,
    CopyMeta(
        "Copy metadata. For http you will get the request and response: headers and other details. For all will get the exception and the source.");

    String description;

    CopyEvent() {
      this.description = name();
    }

    CopyEvent(String description) {
      this.description = description;
    }
  }

  default void reportOperationEvent(CopyEvent event, Throwable e, ExistingLocation<?> src, ReferenceLocation<?> dst,
      Object... args) {
  }

  default void reportOperationEvent(CopyEvent event, ExistingLocation<?> src, ReferenceLocation<?> dst,
      Object... args) {
  }

  /**Destination can be changed based on the input and metadata.*/
  @SuppressWarnings("unchecked")
  default <T extends WritableFileLocation<?>> T amend(T dest, StreamAndMeta streamAndMeta) {
    String code = streamAndMeta.meta.httpMetaResponseStatusCode().get();
    if (code.equals("200")) {
      return dest;
    } else {
      return (T) dest.meta("http" + code, "html");
    }
  }
}
