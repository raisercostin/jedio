package org.raisercostin.jedio;

public interface BasicDirLocation extends ExistingLocation {

  @Override
  BasicDirLocation child(RelativeLocation path);

  ChangeableLocation asChangableLocation();
}