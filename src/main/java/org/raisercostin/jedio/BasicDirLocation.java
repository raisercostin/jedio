package org.raisercostin.jedio;

/** Location that is known to exist and that can have child locations. */
public interface BasicDirLocation<SELF extends BasicDirLocation<SELF>> extends ExistingLocation<SELF> {
  @Override
  SELF child(RelativeLocation path);

  ChangeableLocation<?> asChangableLocation();
}
