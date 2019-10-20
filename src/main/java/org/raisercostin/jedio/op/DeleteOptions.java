package org.raisercostin.jedio.op;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public interface DeleteOptions {
  boolean deleteByRename();

  @Data
  @Getter(value = AccessLevel.NONE)
  @Setter(value = AccessLevel.NONE)
  @Builder
  public static class SimpleDeleteOptions implements DeleteOptions {
    public final boolean deleteByRename;

    @Override
    public boolean deleteByRename() {
      return deleteByRename;
    }
  }

  // case class CopyOptions(overwriteIfAlreadyExists: Boolean = false, copyMeta:
  // Boolean, optionalMeta: Boolean, monitor: OperationMonitor =
  // LoggingOperationMonitor)

  public static DeleteOptions deleteByRenameOption() {
    return new SimpleDeleteOptions(true);
  }

  public static DeleteOptions deletePermanent() {
    return new SimpleDeleteOptions(false);
  }

  public static DeleteOptions deleteToBintray() {
    return deleteByRenameOption();
  }

  public static DeleteOptions deleteDefault() {
    return deleteToBintray();
  }
}
