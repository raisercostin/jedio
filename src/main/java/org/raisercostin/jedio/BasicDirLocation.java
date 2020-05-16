package org.raisercostin.jedio;

/** Location that is known to exist and that can have child locations. */
public interface BasicDirLocation<SELF extends BasicDirLocation<SELF>> extends ExistingLocation<SELF> {
  @Override
  default SELF child(RelativeLocation path) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default ChangeableLocation<?> asChangableLocation() {
    return (ChangeableLocation<?>) this;
  }
}
