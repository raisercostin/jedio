package org.raisercostin.jedio;

import java.util.function.Function;

import io.vavr.API;
import io.vavr.collection.Iterator;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.PathWithAttributes;
import org.raisercostin.jedio.url.UrlLocation;
import reactor.core.publisher.Flux;

/**A location that is generic: a web address `raisercostin.org` has the following children:
 * - http://raisercostin.org
 * - https://raisercostin.org
 * - (http|https)://(wwww)?\.raisercostin\.org(/(favicon.ico|robots.txt|sitemap.xml|sitemap.xml.gz|sitemap.gz))?
 */
@Data
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@AllArgsConstructor
@ToString
public class WebLocation implements ReadableDirLocation<WebLocation>, Location<WebLocation> {
  public final boolean isRoot;
  public final String webAddress;

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
  public WebLocation makeDirOnParentIfNeeded() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<WebLocation> parent() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<WebLocation> existing() {
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
  public WebLocation existingOrElse(Function<NonExistingLocation, DirLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean exists() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean isDir() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean isFile() {
    throw new RuntimeException("Not implemented yet!!!");
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
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean isSymlink() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<PathWithAttributes> find(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore,
      boolean dirsFirst) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public WebLocation create(String path) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public long length() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  private static final Seq<String> prefixes = API.Seq("http://", "https://");
  private static final Seq<String> suffixes = API.Seq("", "/", "/favicon.ico", "/robots.txt", "/sitemap.xml",
    "/sitemap.xml.gz", "/sitemap.gz");

  //(http|https)://(wwww)?\.raisercostin\.org(/(favicon.ico|robots.txt|sitemap.xml|sitemap.xml.gz|sitemap.gz))?
  @Override
  public Flux<WebLocation> findFilesAndDirs(boolean recursive) {
    return Flux.fromIterable(ls());
  }

  @Override
  public Iterator<WebLocation> ls() {
    Seq<String> all = prefixes.map(prefix -> prefix + webAddress)
      .flatMap(x -> suffixes.map(suffix -> x + suffix));
    if (webAddress.startsWith("www.")) {
      return all.map(x -> child(x)).iterator();
    } else {
      return all.appendAll(all.map(x -> "wwww." + x)).map(x -> child(x)).iterator();
    }
  }

  public UrlLocation asUrlLocation() {
    return new UrlLocation(webAddress);
  }

  @Override
  public WebLocation child(String path) {
    return new WebLocation(false, path);
  }
}
