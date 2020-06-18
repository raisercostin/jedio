package org.raisercostin.jedio.url;

import java.net.URL;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.httpclient.URI;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;

@Data
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@ToString
public abstract class BaseHttpLocationLike<SELF extends BaseHttpLocationLike<SELF>>
    implements ReadableFileLocationLike<SELF> {
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
  public static URL resolve(String url, String childOrAbsolute) {
    if (url == null) {
      return new URL(childOrAbsolute);
    }
    if (childOrAbsolute == null || childOrAbsolute.isEmpty()) {
      return new URL(url);
    }
    return resolve(new URL(url), childOrAbsolute);
  }

  public static URL resolve(URL url, String child) {
    return SimpleUrl.resolve(url, child);
  }

  @SneakyThrows
  private static URI toApacheUri(URL url, boolean escaped) {
    return new URI(url.toExternalForm(), escaped);
  }

  @SneakyThrows
  private static URI toApacheUri(String url, boolean escaped) {
    return new URI(url, escaped);
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
  BaseHttpLocationLike(URI uri) {
    this.url = new URL(uri.toString());
  }

  protected abstract SELF create(URL url, boolean escaped);

  @Override
  public String toExternalForm() {
    return url.toExternalForm();
  }

  @Override
  @SneakyThrows
  public java.net.URI toUri() {
    return url.toURI();
  }

  @Override
  public URL toUrl() {
    return url;
  }

  @Override
  public SELF child(String path) {
    return create(resolve(url, path), true);
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
