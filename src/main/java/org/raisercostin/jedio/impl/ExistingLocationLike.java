package org.raisercostin.jedio.impl;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.raisercostin.jedio.ExistingLocation;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.op.DeleteOptions;

/** Location that is known to exist. */
public interface ExistingLocationLike<SELF extends @NonNull ExistingLocationLike<SELF>>
    extends ReferenceLocationLike<SELF>, ExistingLocation {
  @Override
  default NonExistingLocation delete(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
