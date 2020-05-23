package org.raisercostin.jedio;

import java.net.URI;
import java.net.URL;
import java.util.function.Function;

import io.vavr.Function2;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.FilenameUtils;
import org.jedio.deprecated;
import org.jedio.sugar;
import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.PathWithAttributes;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.path.PathLocation;
import reactor.core.publisher.Flux;

// TODO maybe should contain type <T> of the actual internal instance
public interface ReferenceLocation<SELF extends ReferenceLocation<SELF>> extends Location<SELF> {
  default SELF child(RelativeLocation path) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @sugar
  default SELF child(String path) {
    return child(RelativeLocation.create(path));
  }

  @Deprecated
  default String absolute() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Deprecated
  default String normalized() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Deprecated
  default String canonical() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default String absoluteAndNormalized() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default String real() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default String getName() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  /** A form that is parsable back to the same type. Usually contains the schema/protocol. */
  default String toExternalForm() {
    return toUrl().toExternalForm();
  }

  @SneakyThrows
  default URL toUrl() {
    return toUri().toURL();
  }

  default URI toUri() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default Option<RelativeLocation> stripAncestor(BasicDirLocation<?> x) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  /**
   * Returns a new location inside `to` with the same relative path as the current item is inside `from`. For example
   * file `Location.file("c:\a\b\c.txt").relative("c:\","c:\x") equals Location.file("c:\x\a\b\c.txt")`
   */
  @SuppressWarnings("unchecked")
  default Option<SELF> relative(BasicDirLocation<?> from, BasicDirLocation<?> to) {
    Option<RelativeLocation> relative = stripAncestor(from);
    return relative.map(x -> (SELF) to.child(x));
  }

  default Option<SELF> findAncestor(Function<ReferenceLocation<?>, Boolean> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default SELF makeDirOnParentIfNeeded() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default Option<? extends SELF> parent() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default Option<SELF> existing() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default Option<NonExistingLocation<?>> nonExisting() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default NonExistingLocation<?> nonExistingOrElse(Function<DirLocation, NonExistingLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default SELF existingOrElse(Function<NonExistingLocation<?>, DirLocation<?>> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default boolean exists() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default boolean isDir() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default boolean isFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default void symlinkTo(ReferenceLocation<?> parent) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default void junctionTo(ReferenceLocation<?> parent) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default Option<LinkLocation> asSymlink() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default boolean isSymlink() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @sugar
  default SELF mkdirIfNecessary() {
    return existingOrElse(NonExistingLocation::mkdir);
  }

  default Flux<PathWithAttributes> find2(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore) {
    return find(traversal, filter, recursive, gitIgnore, true);
  }

  default Flux<PathWithAttributes> find(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore,
      boolean dirsFirst) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default SELF create(String path) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  /** dirname(/some/path/a.test)=/some/path */
  default String dirname() {
    val fullname = absoluteAndNormalized();
    return FilenameUtils.getFullPath(fullname);
  }

  /** basename(/some/path/a.test)=a */
  default String basename() {
    val fullname = absoluteAndNormalized();
    return FilenameUtils.getBaseName(fullname);
  }

  /** extension(/some/path/a.test)=.test */
  default String extension() {
    val fullname = absoluteAndNormalized();
    return FilenameUtils.getExtension(fullname);
  }

  /** filename(/some/path/a.test)=a.text */
  default String filename() {
    val fullname = absoluteAndNormalized();
    return FilenameUtils.getName(fullname);
  }

  default SELF withName(String name) {
    return create(FilenameUtils.concat(dirname(), name));
  }

  default SELF withName(Function<String, String> newName) {
    val fullname = absoluteAndNormalized();
    return create(
      FilenameUtils.concat(FilenameUtils.getFullPath(fullname), newName.apply(FilenameUtils.getName(fullname))));
  }

  default SELF withBasename(Function<String, String> newBasename) {
    val fullname = absoluteAndNormalized();
    return create(FilenameUtils.concat(FilenameUtils.getFullPath(fullname),
      newBasename.apply(FilenameUtils.getBaseName(fullname)) + "." + FilenameUtils.getExtension(fullname)));
  }

  default SELF withExtension(String newExtension) {
    val fullname = absoluteAndNormalized();
    return create(FilenameUtils.removeExtension(fullname) + "." + newExtension);
  }

  default SELF withExtension(Function<String, String> newExtension) {
    val fullname = absoluteAndNormalized();
    return create(
      FilenameUtils.removeExtension(fullname) + "." + newExtension.apply(FilenameUtils.getExtension(fullname)));
  }

  default SELF withBasenameAndExtension(Function2<String, String, String> newBasenameAndExtension) {
    val fullname = absoluteAndNormalized();
    val basename = FilenameUtils.getBaseName(fullname);
    val extension = FilenameUtils.getExtension(fullname);
    return create(
      FilenameUtils.concat(FilenameUtils.getFullPath(fullname), newBasenameAndExtension.apply(basename, extension)));
  }

  default SELF meta() {
    return meta("http", "json");
  }

  @SuppressWarnings("unchecked")
  default SELF meta(String meta, String extension) {
    return CopyOptions.meta((SELF) this, meta, extension);
  }

  @Deprecated
  @deprecated("use withBasename")
  default SELF withNameAndExtension(Function<String, String> newBasename) {
    return withBasename(newBasename);
  }

  default boolean hasExtension(String extension) {
    return getName().endsWith("." + extension);
  }

  default boolean isEmpty() {
    return !exists() || length() == 0;
  }

  default long length() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  // various forced conversions

  default WritableFileLocation asWritableFile() {
    return (WritableFileLocation) this;
  }

  default ReadableFileLocation<?> asReadableFile() {
    return (ReadableFileLocation<?>) this;
  }

  default DirLocation asDir() {
    return (DirLocation) this;
  }

  default ReadableDirLocation<?> asReadableDir() {
    return (ReadableDirLocation<?>) this;
  }

  default WritableDirLocation asWritableDir() {
    return (WritableDirLocation) this;
  }

  default PathLocation asPathLocation() {
    return (PathLocation) this;
  }
}
