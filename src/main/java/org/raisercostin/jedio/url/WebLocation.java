package org.raisercostin.jedio.url;

import io.vavr.API;
import io.vavr.collection.Iterator;
import io.vavr.collection.Seq;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.raisercostin.jedio.ReadableDirLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.impl.ReadableDirLocationLike;
import org.raisercostin.jedio.url.JedioHttpClients.JedioHttpClient;
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
public class WebLocation implements ReadableDirLocation, ReadableDirLocationLike<WebLocation> {
  public final boolean isRoot;
  public final String webAddress;
  private static final Seq<String> prefixes1 = API.Seq(
      // "http://",
      "https://"
  //
  );
  private static final Seq<String> prefixes2 = API.Seq(
      // "",
      "www.");
  private static final Seq<String> suffixes = API.Seq("", "/", "/favicon.ico", "/robots.txt", "/sitemap.xml",
      "/sitemap.xml.gz", "/sitemap.gz");

  // (http|https)://(wwww)?\.raisercostin\.org(/(favicon.ico|robots.txt|sitemap.xml|sitemap.xml.gz|sitemap.gz))?
  @Override
  public Flux<WebLocation> findFilesAndDirs(boolean recursive) {
    return Flux.fromIterable(ls());
  }

  @Override
  public Iterator<WebLocation> ls() {
    return prefixes1
        .flatMap(
            prefix1 -> prefixes2.flatMap(prefix2 -> suffixes.map(suffix -> prefix1 + prefix2 + webAddress + suffix)))
        .iterator().map(x -> child(x));
  }

  private static final JedioHttpClient client = JedioHttpClients.createHighPerfHttpClient();

  public HttpClientLocation asHttpClientLocation() {
    return new HttpClientLocation(webAddress, false, client);
  }

  public HttpStandardJavaLocation asHttpStandardJavaLocation() {
    return new HttpStandardJavaLocation(webAddress, false);
  }

  @Override
  public WebLocation child(String path) {
    return new WebLocation(false, path);
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    throw new RuntimeException("Doesn't have content so it cannot be read.");
  }
}
