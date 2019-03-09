package org.raisercostin.jedio;

import org.raisercostin.util.sugar;

import reactor.core.publisher.Flux;

/** Location that is known to exist and that can have child locations. */
public interface DirLocation extends ExistingLocation {
  NonExistingLocation deleteDir(DeleteOptions options);

  ReferenceLocation child(RelativeLocation path);

  @sugar
  default ReferenceLocation child(String path) {
    return child(RelativeLocation.create(path));
  }

  @sugar
  default DirLocation childDir(String path) {
    return child(path).mkdirIfNecessary();
  }

  ChangableLocation asChangableLocation();

  Flux<ExistingLocation> findFilesAndDirs();

  Flux<FileLocation> findFiles();
}
