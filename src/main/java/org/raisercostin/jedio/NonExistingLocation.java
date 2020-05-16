package org.raisercostin.jedio;

import org.raisercostin.util.sugar;

/** Location that is known to exist. */
public interface NonExistingLocation<SELF extends NonExistingLocation<SELF>> extends ReferenceLocation<SELF> {
  // TODO what if mkdir fails?
  DirLocation mkdir();

  @SuppressWarnings("unchecked")
  @sugar
  default SELF nonExistingChild(RelativeLocation path) {
    return (SELF) child(path).nonExisting().get();
  }

  @SuppressWarnings("unchecked")
  @sugar
  default SELF nonExistingChild(String path) {
    return (SELF) child(RelativeLocation.create(path)).nonExisting().get();
  }
}
