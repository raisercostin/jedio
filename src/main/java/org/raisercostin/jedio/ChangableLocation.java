package org.raisercostin.jedio;

import reactor.core.publisher.Flux;

/** Writable by me or others?*/
public interface ChangableLocation extends FileLocation {
  Flux<FileAltered> watch();
}
