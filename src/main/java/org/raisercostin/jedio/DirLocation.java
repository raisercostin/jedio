package org.raisercostin.jedio;

public interface DirLocation<SELF extends DirLocation<SELF, FileSELF>, FileSELF extends ReadableFileLocation<FileSELF>>
    extends ReadableDirLocation<SELF, FileSELF>, WritableDirLocation<SELF> {
}
