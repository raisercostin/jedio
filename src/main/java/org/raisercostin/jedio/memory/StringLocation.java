package org.raisercostin.jedio.memory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import io.vavr.control.Option;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.impl.LinkLocationLike;
import org.raisercostin.jedio.impl.NonExistingLocationLike;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;
import org.raisercostin.jedio.impl.WritableFileLocationLike;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.op.DeleteOptions;
import reactor.core.publisher.Mono;

public class StringLocation
    implements FileLocation, NonExistingLocation,
    ReadableFileLocationLike<StringLocation>, WritableFileLocationLike<StringLocation>,
    NonExistingLocationLike<StringLocation> {
  public String content;

  public StringLocation(String content) {
    this.content = content;
  }

  @Override
  public StringLocation deleteFile(DeleteOptions options) {
    this.content = null;
    return this;
  }

  @Override
  public StringLocation delete(DeleteOptions options) {
    this.content = null;
    return this;
  }

  @Override
  public boolean exists() {
    return content != null;
  }

  @Override
  public StringLocation asWritableFile() {
    return this;
  }

  @Override
  public ReadableFileLocationLike asReadableFile() {
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
  public Option<LinkLocationLike> asSymlink() {
    return Option.none();
  }

  @Override
  public boolean isSymlink() {
    return false;
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
  public String readContent(Charset charset) {
    return content;
  }

  @Override
  public Mono<String> readContentAsync() {
    return Mono.just(content);
  }

  @Override
  public StringLocation write(String content, String encoding) {
    this.content = content;
    return this;
  }

  @Override
  public StringLocation copyFrom(ReadableFileLocationLike source, CopyOptions options) {
    if (this.content != null && !options.replaceExisting()) {
      throw new RuntimeException("Cannot overwrite [" + this + "] with content from " + source);
    }
    this.content = source.readContent();
    return this;
  }
}
