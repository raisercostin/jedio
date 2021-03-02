package org.raisercostin.jedio.impl;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jedio.sugar;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.RelativeLocation;

/** Location that is known to exist. */
public interface NonExistingLocationLike<SELF extends @NonNull NonExistingLocationLike<SELF>>
    extends ReferenceLocationLike<SELF>, NonExistingLocation {
  // TODO what if mkdir fails?
  @Override
  default DirLocationLike<?> mkdir() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  @SuppressWarnings("unchecked")
  @sugar
  default SELF nonExistingChild(RelativeLocation path) {
    return (SELF) child(path).nonExisting().get();
  }

  @Override
  @SuppressWarnings("unchecked")
  @sugar
  default SELF nonExistingChild(String path) {
    return (SELF) child(RelativeLocation.create(path)).nonExisting().get();
  }
}
