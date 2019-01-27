package org.raisercostin.jedio;

import io.vavr.control.Option;
import io.vavr.control.Try;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import lombok.Data;
import reactor.core.publisher.Flux;

import org.apache.commons.io.IOUtils;

/**
 * What is Absolute, Relative and Canonical Path
 * <li>https://javarevisited.blogspot.com/2014/08/difference-between-getpath-getabsolutepath-getcanonicalpath-java.html
 * <li>https://stackoverflow.com/questions/30920623/difference-between-getcanonicalpath-and-torealpath
 *
 * @author raiser
 */
@Data
public class ClasspathLocation implements FolderLocation, ExistingLocation, ReferenceLocation, ReadableFileLocation {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ClasspathLocation.class);
  private static final ClassLoader specialClassLoader = Option.of(ClasspathLocation.class.getClassLoader()).getOrElse(ClassLoader.class.getClassLoader());

  private final String path;

  public ClasspathLocation(String path) {
    this.path = fixPath(path);
  }

  private String fixPath(String path) {
    return org.springframework.util.StringUtils.cleanPath(path);
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

  @Override
  public Option<String> read() {
    return Try.ofSupplier(() -> readContent()).toOption();
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
  public String absoluteAndNormalized() {
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
  public Option<RelativeLocation> stripAncestor(FolderLocation x) {
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
  public Option<FolderLocation> existing() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<NonExistingLocation> nonExisting() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation nonExistingOrElse(Function<FolderLocation, NonExistingLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public FolderLocation existingOrElse(Function<NonExistingLocation, FolderLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean exists() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public WritableFileLocation asWritableFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean isFolder() {
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
  public NonExistingLocation deleteFolder(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public ReferenceLocation child(RelativeLocation path) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public InputStream unsafeInputStream() {
    return specialClassLoader.getResourceAsStream(path);
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
  public ChangableLocation asChangableLocation() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<ExistingLocation> find() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<FileLocation> findFiles() {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
