package org.raisercostin.jedio;

import org.raisercostin.util.sugar;

/** Location that is known to exist. */
public interface NonExistingLocation extends ReferenceLocation {
  // TODO what if mkdir fails?
  DirLocation mkdir();

  @sugar
  default NonExistingLocation nonExistingChild(RelativeLocation path) {
    return child(path).nonExisting().get();
  }

  @sugar
  default NonExistingLocation nonExistingChild(String path) {
    return child(RelativeLocation.create(path)).nonExisting().get();
  }
}
