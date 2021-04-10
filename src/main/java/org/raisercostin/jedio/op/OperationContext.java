package org.raisercostin.jedio.op;

import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Value
@lombok.AllArgsConstructor(access = AccessLevel.PUBLIC)
@lombok.NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@lombok.With
@lombok.Builder(toBuilder = true)
@lombok.Getter(value = AccessLevel.NONE)
@lombok.Setter(value = AccessLevel.NONE)
@FieldDefaults(level = AccessLevel.PUBLIC, makeFinal = true)
@Slf4j
public class OperationContext {
  public static OperationContext defaultContext = new OperationContext(CopyOptions.copyDefault(),
    DeleteOptions.deleteDefault(), false);
  CopyOptions copy;
  DeleteOptions delete;
  boolean report;

  public <R> R operation(String operationName, Object object, Supplier<R> operation) {
    return operation(operationName, object, operation, null);
  }

  public <R> R operation(String operationName, Object object, Supplier<R> operation, String formatter, Object... args) {
    try {
      reportStart(operationName, object, formatter, args);
      R res = operation.get();
      reportFinish(operationName, object, res, formatter, args);
      return res;
    } catch (Throwable e) {
      reportThrowable(operationName, object, e, formatter, args);
      throw e;
    }
  }

  private void reportStart(String operationName, Object object, String formatter, Object... args) {
    if (report && log.isInfoEnabled()) {
      String description = formatter == null ? "" : String.format(formatter, args);
      log.info("op> {} start   {}: {}", operationName, object, description);
    }
  }

  private void reportFinish(String operationName, Object object, Object result, String formatter, Object... args) {
    if (report && log.isInfoEnabled()) {
      String description = formatter == null ? "" : String.format(formatter, args);
      if (result != this) {
        log.info("op> {} finish  {} => {}: {}", operationName, object, result, description);
      } else {
        log.info("op> {} finish  {}: {}", operationName, object, description);
      }
    }
  }

  private void reportThrowable(String operationName, Object object, Throwable error, String formatter, Object... args) {
    if (report && log.isWarnEnabled()) {
      String description = formatter == null ? "" : String.format(formatter, args);
      log.warn("op> {} error  {}: {}", operationName, object, description, error);
    }
  }
}
