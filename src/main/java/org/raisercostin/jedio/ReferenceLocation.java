package org.raisercostin.jedio;

import io.vavr.control.Option;
import reactor.core.publisher.Flux;

import java.util.function.Function;

import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.GuavaAndDirectoryStreamTraversalWithVirtualDirs;
import org.raisercostin.jedio.find.GuavaAndDirectoryStreamTraversalWithVirtualDirs.PathWithAttributes;
import org.raisercostin.util.sugar;

// TODO maybe should contain type <T> of the actual internal instance
public interface ReferenceLocation {
  ReferenceLocation child(RelativeLocation path);

  @sugar
  default ReferenceLocation child(String path) {
    return child(RelativeLocation.create(path));
  }

  default PathLocation asPathLocation() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Deprecated
  String absolute();

  @Deprecated
  String normalized();

  @Deprecated
  String canonical();

  String absoluteAndNormalized();

  String real();

  String getName();

  Option<RelativeLocation> stripAncestor(DirLocation x);

  Option<ReferenceLocation> findAncestor(Function<ReferenceLocation, Boolean> fn);

  PathLocation makeDirOnParentIfNeeded();

  Option<? extends ReferenceLocation> parent();

  Option<DirLocation> existing();

  Option<NonExistingLocation> nonExisting();

  NonExistingLocation nonExistingOrElse(Function<DirLocation, NonExistingLocation> fn);

  DirLocation existingOrElse(Function<NonExistingLocation, DirLocation> fn);

  boolean exists();

  // maybe option?
  WritableFileLocation asWritableFile();

  ReadableFileLocation asReadableFile();

  boolean isDir();

  boolean isFile();

  void symlinkTo(ReferenceLocation parent);

  void junctionTo(ReferenceLocation parent);

  Option<LinkLocation> asSymlink();

  boolean isSymlink();

  @sugar
  default DirLocation mkdirIfNecessary() {
    return existingOrElse(x -> x.mkdir());
  }

  Flux<PathWithAttributes> find(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore);
}
