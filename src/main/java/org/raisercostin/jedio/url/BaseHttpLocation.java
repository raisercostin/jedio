package org.raisercostin.jedio.url;

import java.io.InputStream;
import java.net.URL;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.httpclient.URI;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;

@Data
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@ToString
public abstract class BaseHttpLocation<SELF extends BaseHttpLocation<SELF>>
    implements ReferenceLocation<SELF>, ReadableFileLocation<SELF> {

  public static class UrlLocation extends BaseHttpLocation<UrlLocation> {
    @SneakyThrows
    /**
     * @param escaped
     *          <tt>true</tt> if URI character sequence is in escaped form. <tt>false</tt> otherwise.
     */
    public static UrlLocation of(String url, boolean escaped) {
      return new UrlLocation(url, escaped);
    }

    public static UrlLocation of(String url) {
      return new UrlLocation(url);
    }

    public UrlLocation(String url) {
      super(url, false);
    }

    public UrlLocation(String url, boolean escaped) {
      super(url, escaped);
    }

    public UrlLocation(URL url, boolean escaped) {
      super(url, escaped);
    }

    public UrlLocation(URI uri) {
      super(uri);
    }

    @Override
    public InputStream unsafeInputStream() {
      throw new RuntimeException("Not implemented yet!!!");
    }

    @Override
    public String readContent() {
      throw new RuntimeException("Not implemented yet!!!");
    }

    @Override
    protected UrlLocation create(URL resolve, boolean escaped) {
      return new UrlLocation(resolve, escaped);
    }
  }
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

  BaseHttpLocation(String url, boolean escaped) {
    this(toApacheUri(url, escaped));
  }

  BaseHttpLocation(URL url, boolean escaped) {
    this(toApacheUri(url, escaped));
  }

  @SneakyThrows
  BaseHttpLocation(URI uri) {
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
}