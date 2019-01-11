package org.raisercostin.jedio;

import reactor.core.publisher.Flux;

/** Location that is known to exist. */
public interface ExistingLocation extends ReferenceLocation {
  NonExistingLocation delete(DeleteOptions options);
}
