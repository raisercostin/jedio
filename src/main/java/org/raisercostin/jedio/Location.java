package org.raisercostin.jedio;

public interface Location {
  default <T extends Location> T as(Class<T> clazz) {
    return (T) this;
  }
}
