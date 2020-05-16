package org.raisercostin.jedio;

import reactor.core.publisher.Flux;

/** Writable by me or others? */
public interface ChangeableLocation<SELF extends ChangeableLocation<SELF, FileSELF>, FileSELF extends ReadableFileLocation<FileSELF>>
    extends FileLocation<SELF>, DirLocation<SELF, FileSELF> {
  Flux<FileAltered> watch();
}
