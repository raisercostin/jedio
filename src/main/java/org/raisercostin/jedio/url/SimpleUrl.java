package org.raisercostin.jedio.url;

import java.net.URL;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.apache.commons.httpclient.URI;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
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

  @SneakyThrows
  public static SimpleUrl from(String uri) {
    return new SimpleUrl(new URI(uri, false));
  }

  public static SimpleUrl from(String uri, boolean keepQuery) {
    return from(uri).keepQuery(keepQuery);
  }

  URI uri;

  @SneakyThrows
  public SimpleUrl withoutQuery() {
    this.uri.setEscapedQuery(null);
    return this;
  }

  private SimpleUrl keepQuery(boolean keepQuery) {
    return keepQuery ? this : withoutQuery();
  }

  public String toExternalForm() {
    return this.uri.toString();
  }

}
