package org.raisercostin.jedio.memory;

import java.io.InputStream;
import java.nio.charset.Charset;

import com.google.common.base.Charsets;
import io.vavr.control.Option;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.impl.LinkLocationLike;
import org.raisercostin.jedio.impl.NonExistingLocationLike;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;
import org.raisercostin.jedio.impl.WritableFileLocationLike;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.op.DeleteOptions;
import reactor.core.publisher.Mono;

public class InMemoryLocation
    implements FileLocation, NonExistingLocation, ReadableFileLocationLike<@NonNull InMemoryLocation>,
    WritableFileLocationLike<@NonNull InMemoryLocation>, NonExistingLocationLike<@NonNull InMemoryLocation> {
  public String content;

  public InMemoryLocation(String content) {
    this.content = content;
  }

  @Override
  public InMemoryLocation deleteFile(DeleteOptions options) {
    this.content = null;
    return this;
  }

  @Override
  public InMemoryLocation delete(DeleteOptions options) {
    this.content = null;
    return this;
  }

  @Override
  public boolean exists() {
    return this.content != null;
  }

  @Override
  public InMemoryLocation asWritableFile() {
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
    return this.content.length();
  }

  @Override
  public Option<String> readIfExists() {
    return Option.of(this.content);
  }

  @Override
  public InputStream unsafeInputStream() {
    //return new ByteArrayInputStream(this.content.getBytes());
    return IOUtils.toInputStream(this.content, Charsets.UTF_8);
  }

  @Override
  public String readContentSync(Charset charset) {
    return this.content;
  }

  @Override
  public Mono<String> readContentAsync(Charset charset) {
    return Mono.just(this.content);
  }

  @Override
  public InMemoryLocation write(String content, Charset charset) {
    this.content = content;
    return this;
  }

  @Override
  public InMemoryLocation copyFrom(ReadableFileLocation source, CopyOptions options) {
    if (this.content != null && !options.replaceExisting()) {
      throw new RuntimeException("Cannot overwrite [" + this + "] with content from " + source);
    }
    this.content = source.readContent();
    return this;
  }
}
