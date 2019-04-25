package org.raisercostin.jedio.classpath;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.raisercostin.jedio.ChangeableLocation;
import org.raisercostin.jedio.DeleteOptions;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.ExistingLocation;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.LinkLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.GuavaAndDirectoryStreamTraversalWithVirtualDirs.PathWithAttributes;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jedio.url.UrlLocation;

import com.google.common.base.Preconditions;

import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.Data;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * What is Absolute, Relative and Canonical Path
 * <li>https://javarevisited.blogspot.com/2014/08/difference-between-getpath-getabsolutepath-getcanonicalpath-java.html
 * <li>https://stackoverflow.com/questions/30920623/difference-between-getcanonicalpath-and-torealpath
 *
 * @author raiser
 */
@Data
public class ClasspathLocation implements DirLocation, ExistingLocation, ReferenceLocation, ReadableFileLocation {
  private static final ClassLoader specialClassLoader = Option.of(ClasspathLocation.class.getClassLoader())
      .getOrElse(ClassLoader.class.getClassLoader());

  private final String path;

  public ClasspathLocation(String path) {
    this.path = fixPath(path);
  }

  private String fixPath(String path) {
    return org.springframework.util.StringUtils.cleanPath(path);
  }

  @Override
  public Option<String> readIfExists() {
    return Try.ofSupplier(() -> readContent()).toOption();
  }

  @Override
  public String absoluteAndNormalized() {
    return Locations.existingFile(toPath()).absoluteAndNormalized();
  }

  private Path toPath() {
    try {
      return Paths.get(ClasspathLocation.class.getClassLoader().getResource(this.path).toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean exists() {
    try (InputStream stream = ClasspathLocation.class.getClassLoader().getResourceAsStream(path)) {
      return stream != null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ReferenceLocation child(RelativeLocation child) {
    return Locations.classpath(fixPath(path + "/" + child.getLocation()));
  }

  @Override
  public InputStream unsafeInputStream() {
    final InputStream res = specialClassLoader.getResourceAsStream(path);
    Preconditions.checkNotNull(res);
    return res;
  }

  public String readContent() {
    try (BufferedInputStream b = new BufferedInputStream(
        ClasspathLocation.class.getClassLoader().getResourceAsStream(path))) {
      return IOUtils.toString(b, "UTF-8");
    } catch (IOException e) {
      throw new RuntimeException("Can't read resource [" + path + "]", e);
    }
  }

  @Override
  public ClasspathLocation create(String path) {
    return new ClasspathLocation(path);
  }

  @Override
  public DirLocation asDir() {
    return this;
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    return this;
  }

  @Override
  public WritableFileLocation asWritableFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean isDir() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean isFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public void symlinkTo(ReferenceLocation parent) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public void junctionTo(ReferenceLocation parent) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<LinkLocation> asSymlink() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean isSymlink() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation delete(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation deleteDir(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public ChangeableLocation asChangableLocation() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<ExistingLocation> findFilesAndDirs() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<FileLocation> findFiles() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<DirLocation> findDirs() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<PathWithAttributes> find(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String real() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String getName() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<RelativeLocation> stripAncestor(DirLocation x) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<ReferenceLocation> findAncestor(Function<ReferenceLocation, Boolean> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public PathLocation makeDirOnParentIfNeeded() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<? extends ReferenceLocation> parent() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<DirLocation> existing() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<NonExistingLocation> nonExisting() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation nonExistingOrElse(Function<DirLocation, NonExistingLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public DirLocation existingOrElse(Function<NonExistingLocation, DirLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String absolute() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String normalized() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String canonical() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation deleteFile(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public void rename(FileLocation asWritableFile) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public long length() {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
