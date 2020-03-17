package org.raisercostin.jedio;

import java.util.function.Function;

import io.vavr.control.Option;
import lombok.val;
import org.apache.commons.io.FilenameUtils;
import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.PathWithAttributes;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.util.sugar;
import reactor.core.publisher.Flux;

// TODO maybe should contain type <T> of the actual internal instance
public interface ReferenceLocation extends Location {
  ReferenceLocation child(RelativeLocation path);

  @sugar
  default ReferenceLocation child(String path) {
    return child(RelativeLocation.create(path));
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

  default Option<RelativeLocation> stripAncestor(BasicDirLocation x) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  /**
   * Returns a new location inside `to` with the same relative path as the current item is inside `from`. For example
   * file `Location.file("c:\a\b\c.txt").relative("c:\","c:\x") equals Location.file("c:\x\a\b\c.txt")`
   */
  default Option<ReferenceLocation> relative(BasicDirLocation from, BasicDirLocation to) {
    Option<RelativeLocation> relative = stripAncestor(from);
    return relative.map(x -> to.child(x));
  }

  Option<ReferenceLocation> findAncestor(Function<ReferenceLocation, Boolean> fn);

  PathLocation makeDirOnParentIfNeeded();

  Option<? extends ReferenceLocation> parent();

  Option<DirLocation> existing();

  Option<NonExistingLocation> nonExisting();

  NonExistingLocation nonExistingOrElse(Function<DirLocation, NonExistingLocation> fn);

  DirLocation existingOrElse(Function<NonExistingLocation, DirLocation> fn);

  boolean exists();

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

  default Flux<PathWithAttributes> find2(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore) {
    return find(traversal, filter, recursive, gitIgnore, true);
  }

  Flux<PathWithAttributes> find(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore,
      boolean dirsFirst);

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

  default boolean hasExtension(String extension) {
    return getName().endsWith("." + extension);
  }

  default boolean isEmpty() {
    return !exists() || length() == 0;
  }

  long length();

  // various forced conversions

  default WritableFileLocation asWritableFile() {
    return (WritableFileLocation) this;
  }

  default ReadableFileLocation asReadableFile() {
    return (ReadableFileLocation) this;
  }

  default DirLocation asDir() {
    return (DirLocation) this;
  }

  default ReadableDirLocation asReadableDir() {
    return (ReadableDirLocation) this;
  }

  default WritableDirLocation asWritableDir() {
    return (WritableDirLocation) this;
  }

  default PathLocation asPathLocation() {
    return (PathLocation) this;
  }
}
