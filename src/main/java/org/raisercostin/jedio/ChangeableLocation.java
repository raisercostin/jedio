package org.raisercostin.jedio;

import reactor.core.publisher.Flux;

/** Writable by me or others? */
public interface ChangeableLocation extends FileLocation, DirLocation {
  Flux<FileAltered> watch();
}
