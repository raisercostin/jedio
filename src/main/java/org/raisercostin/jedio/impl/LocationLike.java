package org.raisercostin.jedio.impl;

import org.raisercostin.jedio.Location;

public interface LocationLike<SELF extends LocationLike<SELF>> extends Location {
  @SuppressWarnings("unchecked")
  default <T extends LocationLike> T as(Class<T> clazz) {
    return (T) this;
  }
}
