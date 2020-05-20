package org.raisercostin.jedio.memory;

import java.io.InputStream;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.raisercostin.jedio.ReadableFileLocation;
import reactor.core.publisher.Mono;

@Data
@Getter(value = AccessLevel.NONE)
@Setter(value = AccessLevel.NONE)
public class MemoizedLocation<SELF extends MemoizedLocation<SELF>> implements ReadableFileLocation<SELF> {
  private final ReadableFileLocation<?> location;
  private String content;

  public MemoizedLocation(ReadableFileLocation<?> location) {
    this.location = location;
  }

  @Override
  public boolean exists() {
    return true;
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
  public String readContent() {
    if (content == null) {
      content = location.readContent();
    }
    return content;
  }

  @Override
  public Mono<String> readContentAsync() {
    if (content == null) {
      return location.readContentAsync().map(x -> content = x);
    } else {
      return Mono.just(content);
    }
  }
}
