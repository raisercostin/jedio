package org.raisercostin.jedio;

import reactor.core.publisher.Flux;

/** Writable by me or others? */
public interface ChangeableLocation<SELF extends ChangeableLocation<SELF>>
    extends FileLocation<SELF>, DirLocation<SELF> {
  Flux<FileAltered> watch();
}
