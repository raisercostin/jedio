package org.raisercostin.jedio.op;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public interface CopyOptions {
  boolean replaceExisting();
  default boolean makeDirIfNeeded() {
    return true;
  }

  @Data
  @Getter(value = AccessLevel.NONE)
  @Setter(value = AccessLevel.NONE)
  @Builder
  public class SimpleCopyOptions implements CopyOptions{
    public final boolean replaceExisting;

    @Override
    public boolean replaceExisting() {
      return replaceExisting;
    }
  }
  // case class CopyOptions(overwriteIfAlreadyExists: Boolean = false, copyMeta:
  // Boolean, optionalMeta: Boolean, monitor: OperationMonitor =
  // LoggingOperationMonitor)

  public static CopyOptions copyDefault() {
    return copyDoNotOverwrite();
  }

  public static CopyOptions copyDoNotOverwrite() {
    return new SimpleCopyOptions(false);
  }

  public static CopyOptions copyOverwrite() {
    return new SimpleCopyOptions(true);
  }
}
