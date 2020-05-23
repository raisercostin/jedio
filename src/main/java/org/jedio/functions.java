package org.jedio;

public class functions {
  @FunctionalInterface
  public interface JedioFunction<T, R> {
    R apply(T t) throws Throwable;
  }

  @FunctionalInterface
  public interface JedioProcedure<T> {
    void apply(T t) throws Throwable;
  }
}