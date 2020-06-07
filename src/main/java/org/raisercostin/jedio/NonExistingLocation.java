package org.raisercostin.jedio;

public interface NonExistingLocation extends ReferenceLocation {

  // TODO what if mkdir fails?
  DirLocation mkdir();

  NonExistingLocation nonExistingChild(RelativeLocation path);

  NonExistingLocation nonExistingChild(String path);
}