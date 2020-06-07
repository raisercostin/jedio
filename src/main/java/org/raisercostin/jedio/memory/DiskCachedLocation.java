package org.raisercostin.jedio.memory;

import java.io.InputStream;
import java.nio.charset.Charset;

import io.vavr.Function1;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.impl.DirLocationLike;
import org.raisercostin.jedio.impl.LinkLocationLike;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;
import org.raisercostin.jedio.impl.ReferenceLocationLike;
import reactor.core.publisher.Mono;

@Data
@Getter(value = AccessLevel.NONE)
@Setter(value = AccessLevel.NONE)
public class DiskCachedLocation implements ReadableFileLocation, ReadableFileLocationLike<DiskCachedLocation> {

  @Data
  @Getter(value = AccessLevel.NONE)
  @Setter(value = AccessLevel.NONE)
  @AllArgsConstructor
  public static class Root {
    public DirLocationLike<?> dir;
    public Function1<String, String> transformer;

    public DiskCachedLocation cached(ReadableFileLocationLike<?> x) {
      return new DiskCachedLocation(this, x);
    }

    private String slug(ReadableFileLocationLike<?> location) {
      return location.absoluteAndNormalized().replaceAll("[:\\\\/#?.&]", "-");
    }

    public ReferenceLocationLike<?> locationFor(ReadableFileLocationLike<?> location) {
      return dir.child(slug(location));
    }
  }

  public static Root cacheAt(DirLocationLike dir, Function1<String, String> transformer) {
    return new Root(dir, transformer);
  }

  private final Root cache;
  private final ReadableFileLocationLike<?> location;

  public DiskCachedLocation(Root cache, ReadableFileLocationLike<?> location) {
    this.cache = cache;
    this.location = location;
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public DiskCachedLocation asReadableFile() {
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
    return readContent().length();
  }

  @Override
  public Option<String> readIfExists() {
    return Option.of(readContent());
  }

  @Override
  public String readContent(Charset charset) {
    ReferenceLocationLike cached = cache.locationFor(location);
    if (cached.exists()) {
      return cached.asReadableFile().readContent(charset);
    } else {
      String content = location.readContent(charset);
      cached.asWritableFile().write(cache.transformer.apply(content));
      return content;
    }
  }

  @Override
  public Mono<String> readContentAsync() {
    ReferenceLocationLike cached = cache.locationFor(location);
    if (cached.exists()) {
      return cached.asReadableFile().readContentAsync();
    } else {
      return location.readContentAsync().map(content -> {
        cached.asWritableFile().write(cache.transformer.apply(content));
        return content;
      });
    }
  }

  @Override
  public InputStream unsafeInputStream() {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
