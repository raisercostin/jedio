package org.raisercostin.jedio.memory;

import java.io.ByteArrayInputStream;
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
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.op.DeleteOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class StringLocation<SELF extends StringLocation<SELF>>
    implements ReadableFileLocation<SELF>, WritableFileLocation<SELF>, NonExistingLocation<SELF> {
  public String content;

  public StringLocation(String content) {
    this.content = content;
  }

  @Override
  public NonExistingLocation deleteFile(DeleteOptions options) {
    this.content = null;
    return this;
  }

  @Override
  public void rename(FileLocation<?> asWritableFile) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation delete(DeleteOptions options) {
    this.content = null;
    return this;
  }

  @Override
  public SELF child(RelativeLocation path) {
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
  //
  //  @Override
  //  public Option<SELF> findAncestor(Function<ReferenceLocation<?>, Boolean> fn) {
  //    throw new RuntimeException("Not implemented yet!!!");
  //  }

  @Override
  public SELF makeDirOnParentIfNeeded() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<SELF> parent() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<SELF> existing() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<NonExistingLocation<?>> nonExisting() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation nonExistingOrElse(Function<DirLocation, NonExistingLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean exists() {
    return content != null;
  }

  @Override
  public WritableFileLocation asWritableFile() {
    return this;
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
  public SELF create(String path) {
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
    return new ByteArrayInputStream(content.getBytes());
  }

  @Override
  public String readContent() {
    return content;
  }

  @Override
  public Mono<String> readContentAsync() {
    return Mono.just(content);
  }

  @Override
  public SELF write(String content, String encoding) {
    this.content = content;
    return (SELF) this;
  }

  @Override
  public SELF copyFrom(ReadableFileLocation source, CopyOptions options) {
    if (this.content != null && !options.replaceExisting()) {
      throw new RuntimeException("Cannot overwrite [" + this + "] with content from " + source);
    }
    this.content = source.readContent();
    return (SELF) this;
  }

  @Override
  public DirLocation mkdir() {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
