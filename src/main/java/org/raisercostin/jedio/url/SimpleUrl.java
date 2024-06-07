package org.raisercostin.jedio.url;

import java.net.URL;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.raisercostin.jedio.url.impl.ModifiedURI;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
public class SimpleUrl {
  @SneakyThrows
  public static String resolve(String url, String childOrAbsolute) {
    if (childOrAbsolute.startsWith("javascript:")) {
      return childOrAbsolute;
    }
    if (url == null || childOrAbsolute.startsWith("javascript:")) {
      return resolve((URL) null, childOrAbsolute);
    } else {
      return resolve(new URL(url), childOrAbsolute);
    }
  }

  @SneakyThrows
  public static String resolve(URL url, String childOrAbsolute) {
    if (StringUtils.isEmpty(childOrAbsolute)) {
      return url.toExternalForm();
    }
    ModifiedURI child = new ModifiedURI(childOrAbsolute, false);
    if (url == null || child.isAbsoluteURI()) {
      return new URL(child.getEscapedURI()).toExternalForm();
    }
    child.normalize();
    ModifiedURI base = new ModifiedURI(url.toExternalForm(), true);
    ModifiedURI result = new ModifiedURI(base, child);
    return new URL(result.toString()).toExternalForm();
  }

  @SneakyThrows
  public static SimpleUrl from(String uri) {
    return new SimpleUrl(new ModifiedURI(uri, false));
  }

  public static SimpleUrl from(String uri, boolean keepQuery) {
    return from(uri).keepQuery(keepQuery);
  }

  ModifiedURI uri;

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
