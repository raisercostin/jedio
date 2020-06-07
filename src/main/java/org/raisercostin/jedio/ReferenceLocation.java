package org.raisercostin.jedio;

import java.net.URI;
import java.net.URL;
import java.util.function.Function;

import io.vavr.Function2;
import io.vavr.control.Option;
import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.PathWithAttributes;
import org.raisercostin.jedio.impl.DirLocationLike;
import org.raisercostin.jedio.impl.LinkLocationLike;
import org.raisercostin.jedio.impl.NonExistingLocationLike;
import org.raisercostin.jedio.impl.ReadableDirLocationLike;
import org.raisercostin.jedio.impl.ReferenceLocationLike;
import org.raisercostin.jedio.impl.WritableDirLocationLike;
import org.raisercostin.jedio.path.PathLocation;
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

  URL toUrl();

  URI toUri();

  org.apache.commons.httpclient.URI toApacheUri();

  Option<RelativeLocation> stripAncestor(BasicDirLocation x);

  /**
   * Returns a new location inside `to` with the same relative path as the current item is inside `from`. For example
   * file `Location.file("c:\a\b\c.txt").relative("c:\","c:\x") equals Location.file("c:\x\a\b\c.txt")`
   */
  // TODO override properly in ReferenceLocationLike
  // default Option<ReferenceLocation> relativeRef(BasicDirLocation<?> from, BasicDirLocation<?> to);
  // <T extends ReferenceLocation> Option<T> findAncestor(Function<ReferenceLocation, Boolean> fn);

  ReferenceLocation mkdirOnParentIfNeeded();

  <T extends ReferenceLocation> Option<T> parent();

  <T extends ReferenceLocation> Option<T> existing();

  Option<NonExistingLocationLike<?>> nonExisting();

  NonExistingLocationLike<?> nonExistingOrElse(Function<DirLocationLike, NonExistingLocationLike> fn);

  ReferenceLocation existingOrElse(Function<NonExistingLocationLike<?>, DirLocationLike<?>> fn);

  boolean exists();

  boolean isDir();

  boolean isFile();

  void symlinkTo(ReferenceLocationLike<?> parent);

  void junctionTo(ReferenceLocationLike<?> parent);

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

  boolean isEmpty();

  long length();

  WritableFileLocation asWritableFile();

  ReadableFileLocation asReadableFile();

  DirLocationLike asDir();

  ReadableDirLocationLike<?> asReadableDir();

  WritableDirLocationLike asWritableDir();

  PathLocation asPathLocation();

}