package org.raisercostin.jedio;

import org.raisercostin.jedio.op.CopyOptions;

public interface ReadableDirLocation extends BasicDirLocation {

  // Flux<SELF> findFilesAsFlux(boolean recursive);
  // Iterator<SELF> findFiles(boolean recursive);
  // Flux<DirLocation<?>> findDirs(boolean recursive);
  // Iterator<SELF> ls();
  // Iterator<SELF> ls(boolean recursive);
  // Flux<SELF> findFilesAndDirs(boolean recursive);

  void copyTo(DirLocation dir, CopyOptions copyOptions);
}