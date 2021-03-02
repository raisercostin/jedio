package org.jedio;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.checkerframework.checker.nullness.qual.Nullable;

//Copied from apache-commons ExceptionUtils
public class RichThrowable {
  public static <R> R rethrowNowrap(final Throwable throwable) {
    return RichThrowable.<R, RuntimeException>sneakyThrow(throwable, null);
  }

  public static <R> R rethrowWrap(final Throwable throwable) {
    throw new RuntimeException(throwable);
  }

  public static <R extends RuntimeException> R nowrap(final Throwable throwable) {
    return nowrap(throwable, null);
  }

  public static <R extends RuntimeException> R wrap(final Throwable throwable) {
    throw new RuntimeException(throwable);
  }

  public static <R extends RuntimeException> R wrap(final Throwable throwable, @Nullable String format,
      Object... args) {
    throw new RuntimeException(String.format(format == null ? "" : format, args), throwable);
  }

  @FunctionalInterface
  public interface MyCheckedException<R> {
    R apply() throws Throwable;
  }

  public static <T> T wrap(MyCheckedException<T> function) {
    return wrap(function, null);
  }

  public static <T> T wrap(MyCheckedException<T> function, @Nullable String format, Object... args) {
    try {
      return function.apply();
    } catch (Throwable e) {
      throw wrap(e, format, args);
    }
  }

  public static <T> T nowrap(MyCheckedException<T> function) {
    return nowrap(function, null);
  }

  public static <T> T nowrap(MyCheckedException<T> function, @Nullable String format, Object... args) {
    try {
      return function.apply();
    } catch (Throwable e) {
      throw nowrap(e, format, args);
    }
  }

  public static <R extends RuntimeException> R nowrap(final Throwable throwable, @Nullable String format,
      Object... args) {
    return RichThrowable.<R, RuntimeException>sneakyThrow(throwable, format, args);
  }

  @SuppressWarnings("unchecked")
  //TODO remove this method since suppressed exceptions are not logged properly by lombok
  // DEV-NOTE: we do not plan to expose this as public API
  // claim that the typeErasure invocation throws a RuntimeException
  private static <R, T extends Throwable> R sneakyThrow(final Throwable throwable, @Nullable String format,
      Object... args)
      throws T {
    if (format != null) {
      throwable.addSuppressed(new RuntimeException(String.format(format, args)));
    }
    throw (T) throwable;
  }

  public static <T> T tryWithSuppressed(MyCheckedException<T> function, String format, Object... args) {
    try {
      return function.apply();
    } catch (Throwable e) {
      throw nowrap(e, format, args);
    }
  }

  public static String toString(Throwable e) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw, true);
    e.printStackTrace(pw);
    return sw.getBuffer().toString();
  }
}
