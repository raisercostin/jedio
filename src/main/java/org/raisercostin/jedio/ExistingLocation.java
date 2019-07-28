package org.raisercostin.jedio;

import org.raisercostin.jedio.op.DeleteOptions;

/** Location that is known to exist. */
public interface ExistingLocation extends ReferenceLocation {
  NonExistingLocation delete(DeleteOptions options);
}
