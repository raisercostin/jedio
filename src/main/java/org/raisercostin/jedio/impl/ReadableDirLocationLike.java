package org.raisercostin.jedio.impl;

import io.vavr.collection.Iterator;
import org.jedio.sugar;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.ReadableDirLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.op.CopyOptions.CopyEvent;
import reactor.core.publisher.Flux;

/** ReadableDir means you can find children (you can list). */
public interface ReadableDirLocationLike<SELF extends ReadableDirLocationLike<SELF>>
    extends BasicDirLocationLike<SELF>, ReadableDirLocation {
  @Deprecated // @deprecated to reimplement in a efficient way
  default Flux<SELF> findFilesAsFlux(boolean recursive) {
    return findFilesAndDirs(recursive).filter(x -> x.isFile());// .map(x -> x);
  }

  @sugar
  default Iterator<SELF> findFiles(boolean recursive) {
    return Iterator.ofAll(findFilesAsFlux(recursive).toIterable());
  }

  @Deprecated // @deprecated to reimplement in a efficient way
  default Flux<DirLocationLike<?>> findDirs(boolean recursive) {
    return findFilesAndDirs(recursive).filter(x -> x.isDir()).map(x -> (DirLocationLike) x);
  }

  default Iterator<SELF> ls() {
    return ls(false);
  }

  @sugar
  default Iterator<SELF> ls(boolean recursive) {
    return Iterator.ofAll(findFilesAndDirs(recursive).toIterable());
  }

  @Override
  default void copyTo(DirLocation dir, CopyOptions copyOptions) {
    findFilesAsFlux(true).doOnSubscribe(s -> copyOptions.reportOperationEvent(CopyEvent.CopyDirStarted, this, dir))
        // .filter(item -> !item.equals(dir))
        .map(item -> {
          WritableFileLocation copied = item.asReadableFile().copyTo(item.relative(this, dir).get().asWritableFile(),
              copyOptions);
          return copied;
        }).timeout(copyOptions.timeoutOnItem())
        .doOnComplete(() -> copyOptions.reportOperationEvent(CopyEvent.CopyDirFinished, this, dir))
        .blockLast(copyOptions.timeoutTotal());
  }

  Flux<SELF> findFilesAndDirs(boolean recursive);
}
