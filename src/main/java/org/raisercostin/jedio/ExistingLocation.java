package org.raisercostin.jedio;

import org.raisercostin.jedio.op.DeleteOptions;

/** Location that is known to exist. */
public interface ExistingLocation<SELF extends ExistingLocation<SELF>> extends ReferenceLocation<SELF> {
  default NonExistingLocation<?> delete(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
