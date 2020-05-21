package org.raisercostin.jedio.url;

import java.net.URL;

import lombok.SneakyThrows;
import org.apache.commons.httpclient.URI;
import org.apache.commons.lang3.StringUtils;

public class SimpleUrl {
  @SneakyThrows
  public static URL resolve(String url, String childOrAbsolute) {
    if (url == null) {
      return resolve((URL) null, childOrAbsolute);
    } else {
      return resolve(new URL(url), childOrAbsolute);
    }
  }

  @SneakyThrows
  public static URL resolve(URL url, String childOrAbsolute) {
    if (StringUtils.isEmpty(childOrAbsolute)) {
      return url;
    }
    URI child = new URI(childOrAbsolute, false);
    if (url == null || child.isAbsoluteURI()) {
      return new URL(child.getEscapedURI());
    }
    child.normalize();
    URI base = new URI(url.toExternalForm(), true);
    URI result = new URI(base, child);
    return new URL(result.toString());
  }
}
