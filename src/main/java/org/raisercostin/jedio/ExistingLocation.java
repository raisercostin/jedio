package org.raisercostin.jedio;

/** Location that is known to exist. */
public interface ExistingLocation extends ReferenceLocation {
  NonExistingLocation delete(DeleteOptions options);
}
