package org.raisercostin.jedio.url;

import java.io.InputStream;
import java.net.URL;

import io.vavr.control.Try;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.httpclient.URI;
import org.apache.commons.lang3.StringUtils;
import org.jedio.ExceptionUtils;
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
    * @param escaped <tt>true</tt> if URI character sequence is in escaped form.
    *                <tt>false</tt> otherwise.
    */
    public static UrlLocation of(String url, boolean escaped) {
      return new UrlLocation(new URI(url, escaped));
    }

    public static UrlLocation of(String url) {
      return of(url, false);
    }

    public UrlLocation(String url) {
      super(url);
    }

    public UrlLocation(URL url) {
      super(url);
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
    protected UrlLocation create(URL resolve) {
      return new UrlLocation(resolve);
    }
  }
  //
  //  @SneakyThrows
  //  public static URI uriNormalized(String uri) {
  //    URI res = new URI(uri, false);
  //    res.normalize();
  //    return res;
  //  }
  //
  //  @SneakyThrows
  //  public static URI uri(String uri) {
  //    return new URI(uri, false);
  //  }

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

  @SneakyThrows
  public static URL resolve(URL url, String childOrAbsolute) {
    if (StringUtils.isEmpty(childOrAbsolute)) {
      return url;
    }
    try {
      Try<URL> childAsAbsolute = createAbsolute(childOrAbsolute);
      if (childAsAbsolute.isSuccess()) {
        return url.toURI().resolve(childAsAbsolute.get().toURI()).toURL();
      }
      //TODO what about query&fragments?
      childOrAbsolute = StringUtils.removeStart(childOrAbsolute, "/");
      String baseUrl = StringUtils.removeEnd(url.toExternalForm(), "/");
      return new URL(baseUrl + "/" + childOrAbsolute);
    } catch (Exception e) {
      throw ExceptionUtils.nowrap(e, "Trying to create a relativize(%s,%s)", url, childOrAbsolute);
    }
  }

  private static Try<URL> createAbsolute(String childOrAbsolute) {
    return Try.of(() -> new URL(childOrAbsolute));
  }

  @SneakyThrows
  private static URI toApacheUri(URL url) {
    return new URI(url.toExternalForm(), false);
  }

  @SneakyThrows
  private static URI toApacheUri(String url) {
    return new URI(url, false);
  }

  public final URL url;

  BaseHttpLocation(String url) {
    this(toApacheUri(url));
  }

  BaseHttpLocation(URL url) {
    this(toApacheUri(url));
  }

  @SneakyThrows
  BaseHttpLocation(URI uri) {
    this.url = new URL(uri.toString());
  }

  protected abstract SELF create(URL url);

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
    return create(resolve(url, path));
  }
}