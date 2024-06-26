package org.raisercostin.jedio.url;

import java.net.URL;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;
import org.raisercostin.jedio.url.impl.ModifiedURI;

@Data
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@ToString
public abstract class BaseHttpLocationLike<SELF extends @NonNull BaseHttpLocationLike<SELF>>
    implements ReadableFileLocationLike<@NonNull SELF> {
  //
  // @SneakyThrows
  // public static URI uriNormalized(String uri) {
  // URI res = new URI(uri, false);
  // res.normalize();
  // return res;
  // }
  //
  // @SneakyThrows
  // public static URI uri(String uri) {
  // return new URI(uri, false);
  // }

  @SneakyThrows
  public static String resolve(String url, String childOrAbsolute) {
    if (url == null) {
      return new URL(childOrAbsolute).toExternalForm();
    }
    if (childOrAbsolute == null || childOrAbsolute.isEmpty()) {
      return new URL(url).toExternalForm();
    }
    return resolve(new URL(url), childOrAbsolute);
  }

  public static String resolve(URL url, String child) {
    return SimpleUrl.resolve(url, child);
  }

  @SneakyThrows
  private static ModifiedURI toApacheUri(URL url, boolean escaped) {
    return new ModifiedURI(url.toExternalForm(), escaped);
  }

  @SneakyThrows
  private static ModifiedURI toApacheUri(String url, boolean escaped) {
    return new ModifiedURI(url, escaped);
  }

  /** Url is always escaped properly. */
  public final URL url;

  BaseHttpLocationLike(String url, boolean escaped) {
    this(toApacheUri(url, escaped));
  }

  BaseHttpLocationLike(URL url, boolean escaped) {
    this(toApacheUri(url, escaped));
  }

  BaseHttpLocationLike(SimpleUrl url) {
    this(url.uri);
  }

  @SneakyThrows
  BaseHttpLocationLike(ModifiedURI uri) {
    this.url = new URL(uri.toString());
  }

  protected abstract SELF create(URL url, boolean escaped);

  @Override
  public String toExternalForm() {
    return this.url.toExternalForm();
  }

  @Override
  @SneakyThrows
  public java.net.URI toUri() {
    return this.url.toURI();
  }

  @Override
  public URL toUrl() {
    return this.url;
  }

  @Override
  @SneakyThrows
  public SELF child(String path) {
    return create(new URL(resolve(this.url, path)), true);
  }

  @Override
  public String absolute() {
    return toExternalForm();
  }

  @Override
  public String absoluteAndNormalized() {
    return toExternalForm();
  }
}
