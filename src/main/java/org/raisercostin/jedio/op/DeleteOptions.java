package org.raisercostin.jedio.op;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.With;

public interface DeleteOptions extends OperationOptions {
  boolean deleteByRename();

  boolean ignoreNonExisting();

  @Data
  @Getter(value = AccessLevel.NONE)
  @Setter(value = AccessLevel.NONE)
  @Builder
  @With
  public static class SimpleDeleteOptions implements DeleteOptions {
    public final boolean deleteByRename;
    public final boolean ignoreNonExisting;

    @Override
    public boolean deleteByRename() {
      return this.deleteByRename;
    }

    @Override
    public boolean ignoreNonExisting() {
      return this.ignoreNonExisting;
    }
  }

  // case class CopyOptions(overwriteIfAlreadyExists: Boolean = false, copyMeta:
  // Boolean, optionalMeta: Boolean, monitor: OperationMonitor =
  // LoggingOperationMonitor)

  static DeleteOptions deleteByRenameOption() {
    return new SimpleDeleteOptions(true, false);
  }

  static DeleteOptions deletePermanent() {
    return new SimpleDeleteOptions(false, false);
  }

  static DeleteOptions deleteToBintray() {
    return deleteByRenameOption();
  }

  static DeleteOptions deleteDefault() {
    return deleteToBintray();
  }
}
