package org.raisercostin.jedio.impl;

import java.net.URI;
import java.net.URL;
import java.util.function.Function;

import io.vavr.Function2;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.io.FilenameUtils;
import org.jedio.Cast;
import org.jedio.deprecated;
import org.jedio.sugar;
import org.raisercostin.jedio.BasicDirLocation;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.WritableDirLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.PathWithAttributes;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.path.PathLocation;
import reactor.core.publisher.Flux;

// TODO maybe should contain type <T> of the actual internal instance
public interface ReferenceLocationLike<SELF extends ReferenceLocationLike<SELF>>
    extends LocationLike<SELF>, ReferenceLocation {

  @Override
  default SELF child(RelativeLocation path) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  @sugar
  default SELF child(String path) {
    return child(RelativeLocation.create(path));
  }

  @Override
  @Deprecated
  default String absolute() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  @Deprecated
  default String normalized() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  @Deprecated
  default String canonical() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default String absoluteAndNormalized() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default String real() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default String getName() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  /** A form that is parsable back to the same type. Usually contains the schema/protocol. */
  @Override
  default String toExternalForm() {
    return toUrl().toExternalForm();
  }

  @Override
  @SneakyThrows
  default URL toUrl() {
    return toUri().toURL();
  }

  @Override
  default URI toUri() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  @SneakyThrows
  default org.apache.commons.httpclient.URI toApacheUri() {
    return new org.apache.commons.httpclient.URI(toUrl().toExternalForm(), true);
  }

  @Override
  default Option<RelativeLocation> stripAncestor(BasicDirLocation x) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  /**
   * Returns a new location inside `to` with the same relative path as the current item is inside `from`. For example
   * file `Location.file("c:\a\b\c.txt").relative("c:\","c:\x") equals Location.file("c:\x\a\b\c.txt")`
   */
  // @Override
  @SuppressWarnings("unchecked")
  default Option<SELF> relative(BasicDirLocation from, BasicDirLocation to) {
    Option<RelativeLocation> relative = stripAncestor(from);
    return relative.map(x -> (SELF) to.child(x));
  }

  // @Override
  default Option<SELF> findAncestor(Function<ReferenceLocationLike<?>, Boolean> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default SELF mkdirOnParentIfNeeded() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  // @Override
  default Option<SELF> parent() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @SuppressWarnings("unchecked")
  @Override
  default Option<ReferenceLocation> parentRef() {
    return Cast.cast(parent());
  }

  // @Override
  default Option<SELF> existing() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @SuppressWarnings("unchecked")
  @Override
  default Option<ReferenceLocation> existingRef() {
    return Cast.cast(existing());
  }

  @Override
  default Option<NonExistingLocationLike<?>> nonExisting() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default NonExistingLocation nonExistingOrElse(Function<DirLocation, NonExistingLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default SELF existingOrElse(Function<NonExistingLocation, DirLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default boolean exists() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default boolean isDir() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default boolean isFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default void symlinkTo(ReferenceLocation parent) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default void junctionTo(ReferenceLocation parent) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default Option<LinkLocationLike> asSymlink() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default boolean isSymlink() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  @sugar
  default SELF mkdirIfNeeded() {
    return existingOrElse(NonExistingLocation::mkdir);
  }

  @Override
  default Flux<PathWithAttributes> find2(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore) {
    return find(traversal, filter, recursive, gitIgnore, true);
  }

  @Override
  default Flux<PathWithAttributes> find(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore,
      boolean dirsFirst) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default SELF create(String path) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  /** dirname(/some/path/a.test)=/some/path */
  @Override
  default String dirname() {
    val fullname = absoluteAndNormalized();
    return FilenameUtils.getFullPath(fullname);
  }

  /** basename(/some/path/a.test)=a */
  @Override
  default String basename() {
    val fullname = absoluteAndNormalized();
    return FilenameUtils.getBaseName(fullname);
  }

  /** extension(/some/path/a.test)=.test */
  @Override
  default String extension() {
    val fullname = absoluteAndNormalized();
    return FilenameUtils.getExtension(fullname);
  }

  /** filename(/some/path/a.test)=a.text */
  @Override
  default String filename() {
    val fullname = absoluteAndNormalized();
    return FilenameUtils.getName(fullname);
  }

  @Override
  default SELF withName(String name) {
    return create(FilenameUtils.concat(dirname(), name));
  }

  @Override
  default SELF withName(Function<String, String> newName) {
    val fullname = absoluteAndNormalized();
    return create(
        FilenameUtils.concat(FilenameUtils.getFullPath(fullname), newName.apply(FilenameUtils.getName(fullname))));
  }

  @Override
  default SELF withBasename(Function<String, String> newBasename) {
    val fullname = absoluteAndNormalized();
    return create(FilenameUtils.concat(FilenameUtils.getFullPath(fullname),
        newBasename.apply(FilenameUtils.getBaseName(fullname)) + "." + FilenameUtils.getExtension(fullname)));
  }

  @Override
  default SELF withExtension(String newExtension) {
    val fullname = absoluteAndNormalized();
    return create(FilenameUtils.removeExtension(fullname) + "." + newExtension);
  }

  @Override
  default SELF withExtension(Function<String, String> newExtension) {
    val fullname = absoluteAndNormalized();
    return create(
        FilenameUtils.removeExtension(fullname) + "." + newExtension.apply(FilenameUtils.getExtension(fullname)));
  }

  @Override
  default SELF withBasenameAndExtension(Function2<String, String, String> newBasenameAndExtension) {
    val fullname = absoluteAndNormalized();
    val basename = FilenameUtils.getBaseName(fullname);
    val extension = FilenameUtils.getExtension(fullname);
    return create(
        FilenameUtils.concat(FilenameUtils.getFullPath(fullname), newBasenameAndExtension.apply(basename, extension)));
  }

  @Override
  default SELF meta() {
    return meta("http", "json");
  }

  @Override
  @SuppressWarnings("unchecked")
  default SELF meta(String meta, String extension) {
    return CopyOptions.meta((SELF) this, meta, extension);
  }

  @Override
  @Deprecated
  @deprecated("use withBasename")
  default SELF withNameAndExtension(Function<String, String> newBasename) {
    return withBasename(newBasename);
  }

  @Override
  default boolean hasExtension(String extension) {
    return getName().endsWith("." + extension);
  }

  @Override
  default boolean isEmpty() {
    return !exists() || length() == 0;
  }

  @Override
  default long length() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  // various forced conversions

  @Override
  default WritableFileLocation asWritableFile() {
    return (WritableFileLocation) this;
  }

  @Override
  default DirLocation asDir() {
    return (DirLocation) this;
  }

  @Override
  default ReadableDirLocationLike<?> asReadableDir() {
    return (ReadableDirLocationLike<?>) this;
  }

  @Override
  default WritableDirLocation asWritableDir() {
    return (WritableDirLocation) this;
  }

  @Override
  default PathLocation asPathLocation() {
    return (PathLocation) this;
  }
}
