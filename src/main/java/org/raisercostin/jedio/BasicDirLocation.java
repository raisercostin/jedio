package org.raisercostin.jedio;

import org.raisercostin.util.sugar;

/** Location that is known to exist and that can have child locations. */
public interface BasicDirLocation extends ExistingLocation {
  ReferenceLocation child(RelativeLocation path);

  @sugar
  default DirLocation childDir(String path) {
    return child(path).mkdirIfNecessary();
  }

  ChangeableLocation asChangableLocation();
}
