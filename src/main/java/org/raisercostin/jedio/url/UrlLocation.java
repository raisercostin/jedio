package org.raisercostin.jedio.url;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

import lombok.SneakyThrows;
import org.apache.commons.httpclient.URI;

public class UrlLocation extends BaseHttpLocationLike<UrlLocation> {
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
  public String readContentSync(Charset charset) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  protected UrlLocation create(URL resolve, boolean escaped) {
    return new UrlLocation(resolve, escaped);
  }
}
