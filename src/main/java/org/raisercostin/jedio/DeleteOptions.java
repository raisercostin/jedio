package org.raisercostin.jedio;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter(value = AccessLevel.NONE)
@Setter(value = AccessLevel.NONE)
@Builder
public class DeleteOptions {
  public final boolean deleteByRename;
  // case class CopyOptions(overwriteIfAlreadyExists: Boolean = false, copyMeta:
  // Boolean, optionalMeta: Boolean, monitor: OperationMonitor =
  // LoggingOperationMonitor)

  public static DeleteOptions deleteByRename() {
    return new DeleteOptions(true);
  }

  public static DeleteOptions deletePermanent() {
    return new DeleteOptions(false);
  }

  public static DeleteOptions deleteToBintray() {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
