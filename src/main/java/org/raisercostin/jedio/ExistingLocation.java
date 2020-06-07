package org.raisercostin.jedio;

import org.raisercostin.jedio.op.DeleteOptions;

public interface ExistingLocation extends ReferenceLocation {
  NonExistingLocation delete(DeleteOptions options);
}