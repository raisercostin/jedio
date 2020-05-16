package org.raisercostin.jedio;

public interface Location<SELF extends Location<SELF>> {
  @SuppressWarnings("unchecked")
  default <T extends Location> T as(Class<T> clazz) {
    return (T) this;
  }
}
