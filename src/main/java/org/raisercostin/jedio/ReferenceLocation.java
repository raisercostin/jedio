package org.raisercostin.jedio;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.function.Function;

import io.vavr.Function2;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import org.jedio.sugar;
import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.PathWithAttributes;
import org.raisercostin.jedio.impl.LinkLocationLike;
import org.raisercostin.jedio.impl.NonExistingLocationLike;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jedio.url.impl.ModifiedURI;
import reactor.core.publisher.Flux;

public interface ReferenceLocation extends Location {

  ReferenceLocation child(RelativeLocation path);

  ReferenceLocation child(String path);

  String absolute();

  String normalized();

  String canonical();

  String absoluteAndNormalized();

  String real();

  String getName();

  /** A form that is parsable back to the same type. Usually contains the schema/protocol. */
  String toExternalForm();

  @Override
  default String toExternalUri() {
    return toExternalForm();
  }

  URL toUrl();

  URI toUri();

  ModifiedURI toApacheUri();

  Option<RelativeLocation> stripAncestor(BasicDirLocation x);

  default Option<RelativeLocation> relativize(BasicDirLocation ancestor) {
    return stripAncestor(ancestor);
  }

  /**
   * Returns a new location inside `to` with the same relative path as the current item is inside `from`. For example
   * file `Location.file("c:\a\b\c.txt").relative("c:\","c:\x") equals Location.file("c:\x\a\b\c.txt")`
   */
  // TODO override properly in ReferenceLocationLike
  // default Option<ReferenceLocation> relativeRef(BasicDirLocation<?> from, BasicDirLocation<?> to);
  // <T extends ReferenceLocation> Option<T> findAncestor(Function<ReferenceLocation, Boolean> fn);

  ReferenceLocation mkdirOnParentIfNeeded();

  // <T extends ReferenceLocation> Option<T> parent();
  Option<ReferenceLocation> parentRef();

  // <T extends ReferenceLocation> Option<T> existing();
  Option<ReferenceLocation> existingRef();

  Option<NonExistingLocationLike<?>> nonExisting();

  NonExistingLocation nonExistingOrElse(Function<DirLocation, NonExistingLocation> fn);

  ReferenceLocation existingOrElse(Function<NonExistingLocation, DirLocation> fn);

  boolean exists();

  default ReferenceLocation backupName() {
    if (!exists()) {
      return this;
    }
    int counter = 1;
    ReferenceLocation newFile;
    do {
      int counter2 = counter;
      newFile = withBasename(x -> x + "-" + counter2);
      counter++;
    } while (newFile.exists());
    return newFile;
  }

  /**A dir has files but not content.*/
  boolean isDir();

  /**A file has content.*/
  boolean isFile();

  /**A file has content.*/
  @sugar
  default boolean hasContent() {
    return isFile();
  }

  void symlinkTo(ReferenceLocation parent);

  void junctionTo(ReferenceLocation parent);

  Option<LinkLocationLike> asSymlink();

  boolean isSymlink();

  ReferenceLocation mkdirIfNeeded();

  Flux<PathWithAttributes> find2(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore);

  Flux<PathWithAttributes> find(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore,
      boolean dirsFirst);

  ReferenceLocation create(String path);

  /** dirname(/some/path/a.test)=/some/path */
  String dirname();

  /** basename(/some/path/a.test)=a */
  String basename();

  /** extension(/some/path/a.test)=.test */
  String extension();

  /** filename(/some/path/a.test)=a.text */
  String filename();

  ReferenceLocation withName(String name);

  ReferenceLocation withName(Function<String, String> newName);

  ReferenceLocation withBasename(Function<String, String> newBasename);

  ReferenceLocation withExtension(String newExtension);

  ReferenceLocation withExtension(Function<String, String> newExtension);

  ReferenceLocation withBasenameAndExtension(Function2<String, String, String> newBasenameAndExtension);

  ReferenceLocation meta();

  ReferenceLocation meta(String meta, String extension);

  ReferenceLocation withNameAndExtension(Function<String, String> newBasename);

  boolean hasExtension(String extension);

  boolean isEmptyFile();

  long length();

  WritableFileLocation asWritableFile();

  ReadableFileLocation asReadableFile();

  DirLocation asDir();

  ReadableDirLocation asReadableDir();

  WritableDirLocation asWritableDir();

  PathLocation asPathLocation();

  @SneakyThrows
  default FileTime createdDateTime() {
    return (FileTime) Files.getAttribute(asPathLocation().toPath(), "basic:creationTime", LinkOption.NOFOLLOW_LINKS);
  }

  @SneakyThrows
  default FileTime modifiedDateTime() {
    return Files.getLastModifiedTime(asPathLocation().toPath(), LinkOption.NOFOLLOW_LINKS);
    //    return (FileTime) Files.getAttribute(path.asPathLocation().toPath(), "basic:modifiedTime",
    //      LinkOption.NOFOLLOW_LINKS);
  }

  @SneakyThrows
  default BasicFileAttributes basicAttributes(LinkOption... options) {
    return Files.readAttributes(asPathLocation().toPath(), BasicFileAttributes.class, options);
  }

  @SneakyThrows
  default PosixFileAttributes posixAttributes(LinkOption... options) {
    return Files.readAttributes(asPathLocation().toPath(), PosixFileAttributes.class, options);
  }

  @SneakyThrows
  default DosFileAttributes dosAttributes(LinkOption... options) {
    return Files.readAttributes(asPathLocation().toPath(), DosFileAttributes.class, options);
  }
}
