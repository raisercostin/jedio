package org.raisercostin.util;

//Copied from apache-commons ExceptionUtils
public class ExceptionUtils {
  public static <R> R rethrowNowrap(final Throwable throwable) {
    return ExceptionUtils.<R, RuntimeException>typeErasure(throwable);
  }

  public static <R> R rethrowWrap(final Throwable throwable) {
    throw new RuntimeException(throwable);
  }

  public static <R extends RuntimeException> R nowrap(final Throwable throwable) {
    return ExceptionUtils.<R, RuntimeException>typeErasure(throwable);
  }

  public static <R extends RuntimeException> R wrap(final Throwable throwable) {
    throw new RuntimeException(throwable);
  }

  @SuppressWarnings("unchecked")
  // claim that the typeErasure invocation throws a RuntimeException
  private static <R, T extends Throwable> R typeErasure(final Throwable throwable) throws T {
    throw (T) throwable;
  }
}