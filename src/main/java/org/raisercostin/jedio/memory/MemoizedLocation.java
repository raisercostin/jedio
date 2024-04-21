package org.raisercostin.jedio.memory;

import java.io.InputStream;
import java.nio.charset.Charset;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;
import org.raisercostin.jedio.op.OperationOptions.ReadOptions;
import reactor.core.publisher.Mono;

@Data
@Getter(value = AccessLevel.NONE)
@Setter(value = AccessLevel.NONE)
public class MemoizedLocation implements ReadableFileLocation, ReadableFileLocationLike<@NonNull MemoizedLocation> {
  private final ReadableFileLocation location;
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private String content;

  public MemoizedLocation(ReadableFileLocation location) {
    this.location = location;
  }

  @Override
  public boolean exists() {
    return true;
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
  public boolean isSymlink() {
    return false;
  }

  // length in chars or bytes????
  @Override
  public long length() {
    return readContent().length();
  }

  @Override
  public Option<String> readIfExists() {
    return Option.of(readContent());
  }

  @Override
  public InputStream unsafeInputStream() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String readContentSync(ReadOptions options) {
    if (this.content == null) {
      this.content = this.location.readContentSync(options);
    }
    return this.content;
  }

  @Override
  public Mono<String> readContentAsync(ReadOptions options) {
    if (this.content == null) {
      return this.location.readContentAsync(options).map(x -> this.content = x);
    } else {
      return Mono.just(this.content);
    }
  }
}
