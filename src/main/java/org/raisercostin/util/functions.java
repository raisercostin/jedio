package org.raisercostin.util;

public class functions {
  @FunctionalInterface
  public static interface JedioFunction<T, R> {
    R apply(T t) throws Throwable;
  }

  @FunctionalInterface
  public static interface JedioProcedure<T> {
    void apply(T t) throws Throwable;
  }
}