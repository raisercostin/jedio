package org.raisercostin.jedio;

import java.util.function.Function;

import org.apache.commons.io.FilenameUtils;
import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.GuavaAndDirectoryStreamTraversalWithVirtualDirs.PathWithAttributes;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.util.sugar;

import io.vavr.control.Option;
import lombok.val;
import reactor.core.publisher.Flux;

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
    return existingOrElse(NonExistingLocation::mkdir);
  }

  Flux<PathWithAttributes> find(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore);

  ReferenceLocation create(String path);

  default ReferenceLocation withExtension(String extension) {
    return create(FilenameUtils.removeExtension(absoluteAndNormalized()) + "." + extension);
  }

  default ReferenceLocation withName(String name) {
    return create(FilenameUtils.concat(FilenameUtils.getFullPath(absoluteAndNormalized()), name));
  }

  default ReferenceLocation withName(Function<String, String> newName) {
    val fullName = absoluteAndNormalized();
    return create(
        FilenameUtils.concat(FilenameUtils.getFullPath(fullName), newName.apply(FilenameUtils.getName(fullName))));
  }

  DirLocation asDir();

  default boolean hasExtension(String extension) {
    return getName().endsWith("." + extension);
  }

  default boolean isEmpty() {
    return !exists() || length() == 0;
  }

  long length();
}
