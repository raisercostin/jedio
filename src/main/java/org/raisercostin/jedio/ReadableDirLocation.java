package org.raisercostin.jedio;

import io.vavr.collection.Iterator;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.util.sugar;
import reactor.core.publisher.Flux;

/** ReadableDir means you can find children (you can list). */
public interface ReadableDirLocation<SELF extends ReadableDirLocation<SELF, FileSELF>, FileSELF extends ReadableFileLocation<FileSELF>>
    extends BasicDirLocation<SELF> {
  @Deprecated // @deprecated to reimplement in a efficient way
  default Flux<FileSELF> findFilesAsFlux(boolean recursive) {
    return findFilesAndDirs(recursive).filter(x -> x.isFile()).map(x -> (FileSELF) x);
  }

  @sugar
  default Iterator<FileSELF> findFiles(boolean recursive) {
    return Iterator.ofAll(findFilesAsFlux(recursive).toIterable());
  }

  @Deprecated // @deprecated to reimplement in a efficient way
  default Flux<DirLocation<?, ?>> findDirs(boolean recursive) {
    return findFilesAndDirs(recursive).filter(x -> x.isDir()).map(x -> (DirLocation) x);
  }

  default Iterator<SELF> ls() {
    return ls(false);
  }

  @sugar
  default Iterator<SELF> ls(boolean recursive) {
    return Iterator.ofAll(findFilesAndDirs(recursive).toIterable());
  }

  Flux<SELF> findFilesAndDirs(boolean recursive);

  default void copyTo(DirLocation<?, ?> dir, CopyOptions copyOptions) {
    findFilesAsFlux(true).doOnSubscribe(s -> copyOptions.reportOperationEvent("copyDirStart", this))
      // .filter(item -> !item.equals(dir))
      .map(item -> {
        WritableFileLocation<?> copied = item.asReadableFile()
          .copyTo(item.relative(this, dir).get().asWritableFile(),
            copyOptions);
        return copied;
      })
      .timeout(copyOptions.timeoutOnItem())
      .doOnComplete(() -> copyOptions.reportOperationEvent("copyDirEnd", this))
      .blockLast(copyOptions.timeoutTotal());
  }
}
