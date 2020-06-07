package org.raisercostin.jedio.impl;

import org.raisercostin.jedio.BasicDirLocation;
import org.raisercostin.jedio.RelativeLocation;

/** Location that is known to exist and that can have child locations. */
public interface BasicDirLocationLike<SELF extends BasicDirLocationLike<SELF>>
    extends ExistingLocationLike<SELF>, BasicDirLocation {
  @Override
  default SELF child(RelativeLocation path) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default ChangeableLocationLike<?> asChangableLocation() {
    return (ChangeableLocationLike<?>) this;
  }
}
