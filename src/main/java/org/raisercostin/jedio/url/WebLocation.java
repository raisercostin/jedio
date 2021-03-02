package org.raisercostin.jedio.url;

import java.nio.file.attribute.FileTime;

import io.vavr.API;
import io.vavr.collection.Iterator;
import io.vavr.collection.Seq;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jedio.struct.RichIterable;
import org.raisercostin.jedio.ReadableDirLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.impl.ReadableDirLocationLike;
import reactor.core.publisher.Flux;

/**
 * A location that is generic: a web address `raisercostin.org` has the following children: - http://raisercostin.org -
 * https://raisercostin.org -
 * (http|https)://(wwww)?\.raisercostin\.org(/(favicon.ico|robots.txt|sitemap.xml|sitemap.xml.gz|sitemap.gz))?
 */
@Data
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@AllArgsConstructor
@ToString
public class WebLocation implements ReadableDirLocation, ReadableDirLocationLike<@NonNull WebLocation> {
  public final boolean isRoot;
  public final String webAddress;
  private static final RichIterable<String> prefixes1 = RichIterable.of(
    // "http://",
    "https://"
  //
  );
  private static final RichIterable<String> prefixes2 = RichIterable.of(
    // "",
    "www.");
  private static final RichIterable<String> suffixes = RichIterable.of(
    "",
    "/",
    "/favicon.ico",
    "/robots.txt",
    "/sitemap.xml",
    "/sitemap.xml.gz",
    "/sitemap.gz");

  // (http|https)://(wwww)?\.raisercostin\.org(/(favicon.ico|robots.txt|sitemap.xml|sitemap.xml.gz|sitemap.gz))?
  @Override
  public Flux<@NonNull WebLocation> findFilesAndDirs(boolean recursive) {
    return Flux.fromIterable(ls().iterable());
  }

  @Override
  public RichIterable<@NonNull WebLocation> ls() {
    return prefixes1
      .flatMap(
        prefix1 -> prefixes2.flatMap(prefix2 -> suffixes.map(suffix -> prefix1 + prefix2 + this.webAddress + suffix)))
      .map(x -> child(x));
  }

  private static final JedioHttpClient client = JedioHttpClient.createHighPerfHttpClient();

  public HttpClientLocation asHttpClientLocation() {
    return new HttpClientLocation(this.webAddress, false, client);
  }

  public HttpStandardJavaLocation asHttpStandardJavaLocation() {
    return new HttpStandardJavaLocation(this.webAddress, false);
  }

  @Override
  public WebLocation child(String path) {
    return new WebLocation(false, path);
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    throw new RuntimeException("Doesn't have content so it cannot be read.");
  }

  @Override
  public boolean isDir() {
    return isRoot;
  }

  @Override
  public String absoluteAndNormalized() {
    return webAddress;
  }

  @Override
  public FileTime createdDateTime() {
    return FileTime.fromMillis(System.currentTimeMillis());
  }
}
