package org.raisercostin.jedio.memory;

import java.io.InputStream;
import java.util.function.Function;

import io.vavr.control.Option;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.LinkLocation;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.PathWithAttributes;
import org.raisercostin.jedio.op.DeleteOptions;
import org.raisercostin.jedio.path.PathLocation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StringLocation implements ReadableFileLocation {
  public final String content;

  public StringLocation(String content) {
    this.content = content;
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
  public NonExistingLocation delete(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public ReferenceLocation child(RelativeLocation path) {
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
  public boolean exists() {
    return true;
  }

  @Override
  public WritableFileLocation asWritableFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    return this;
  }

  @Override
  public boolean isDir() {
    return false;
  }

  @Override
  public boolean isFile() {
    return true;
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
    return Option.none();
  }

  @Override
  public boolean isSymlink() {
    return false;
  }

  @Override
  public Flux<PathWithAttributes> find(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore,
      boolean dirsFirst) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public ReferenceLocation create(String path) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public DirLocation asDir() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  // length in chars or bytes????
  @Override
  public long length() {
    return content.length();
  }

  @Override
  public Option<String> readIfExists() {
    return Option.of(content);
  }

  @Override
  public InputStream unsafeInputStream() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String readContent() {
    return content;
  }

  @Override
  public Mono<String> readContentAsync() {
    return Mono.just(content);
  }
}
