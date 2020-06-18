package org.raisercostin.jedio;

import reactor.core.publisher.Flux;

/** Writable by me or others? */
public interface ChangeableLocation extends BasicFileLocation, DirLocation {
  Flux<FileAltered> watch();
}
