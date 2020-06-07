package org.raisercostin.jedio.impl;

import org.raisercostin.jedio.ExistingLocation;
import org.raisercostin.jedio.op.DeleteOptions;

/** Location that is known to exist. */
public interface ExistingLocationLike<SELF extends ExistingLocationLike<SELF>>
    extends ReferenceLocationLike<SELF>, ExistingLocation {
  @Override
  default NonExistingLocationLike<?> delete(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
