package org.raisercostin.jedio.memory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import io.vavr.control.Option;
import org.raisercostin.jedio.LinkLocation;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.op.DeleteOptions;
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
  public NonExistingLocation delete(DeleteOptions options) {
    this.content = null;
    return this;
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
  public Option<LinkLocation> asSymlink() {
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
}
