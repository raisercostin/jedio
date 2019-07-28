package org.raisercostin.jedio;

public interface Location {
  @SuppressWarnings("unchecked")
  default <T extends Location> T as(Class<T> clazz) {
    return (T) this;
  }
}
