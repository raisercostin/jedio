package org.raisercostin.jedio.url;

import java.net.URL;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.URI;
import org.apache.commons.lang3.StringUtils;

@Value
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
@Slf4j
public class SimpleUrl {
  String url;

  @SneakyThrows
  public static URL resolve(URL url, String childOrAbsolute) {
    if (StringUtils.isEmpty(childOrAbsolute)) {
      return url;
    }
    URI child = new URI(childOrAbsolute, false);
    child.normalize();
    if (child.isAbsoluteURI()) {
      return new URL(child.getEscapedURI());
    }
    Preconditions.checkArgument(!child.hasAuthority(), "Child should not have authority. %s", child);
    //ignore child query & fragment
    String childPath = Strings.nullToEmpty(child.getPath());
    if (childPath.isEmpty()) {
      return url;
    }
    URI base = new URI(url.toExternalForm(), true);
    if (childPath.startsWith("/")) {
      base.setPath(child.getPath());
    } else {
      String basePath = Strings.nullToEmpty(base.getPath());
      if (basePath.isEmpty()) {
        base.setPath("/" + childPath);
      } else {
        base.setPath(StringUtils.removeEnd(basePath, "/") + "/" + childPath);
      }
    }
    base.setFragment("");
    base.setQuery("");
    base.normalize();
    return new URL(base.toString());
    //
    //    //escape specially for java.net.URI
    //    String childOrAbsoluteEscaped = new URI(childOrAbsolute, false).toString();
    //    //child is absolute
    //    Try<URL> childAsAbsolute = createAbsolute(childOrAbsoluteEscaped);
    //    if (childAsAbsolute.isSuccess()) {
    //      return url.toURI().resolve(childAsAbsolute.get().toURI()).toURL();
    //    }
    //    String relativeChild = childOrAbsolute;
    //    Preconditions.checkArgument(url.getQuery() == null);
    //    Preconditions.checkArgument(url.getRef() == null);
    //    if (url.getPath().isEmpty()) {
    //      //child is relative
    //      if (!relativeChild.startsWith("/")) {
    //        relativeChild = "/" + relativeChild;
    //      }
    //    } else {
    //      if (!url.toExternalForm().endsWith("/")) {
    //        url = new URL(url.toString() + "/");
    //      }
    //    }
    //    return url.toURI().resolve(childOrAbsoluteEscaped).toURL();
    //
    //
    //
    //

    //    boolean absolute = childOrAbsolute.startsWith("/");
    //    String beforePath = toBeforePath(url);
    //    try {
    //      Try<URL> childAsAbsolute = createAbsolute(childOrAbsolute);
    //      if (childAsAbsolute.isSuccess()) {
    //        return url.toURI().resolve(childAsAbsolute.get().toURI()).toURL();
    //      }
    //      //TODO what about query&fragments?
    //      childOrAbsolute = StringUtils.removeStart(childOrAbsolute, "/");
    //      String baseUrl = StringUtils.removeEnd(toUnescaped(url), "/");
    //      return toUrl(baseUrl + "/" + childOrAbsolute, false);
    //    } catch (Exception e) {
    //      throw ExceptionUtils.nowrap(e, "Trying to create a relativize(%s,%s)", url, childOrAbsolute);
    //    }
  }

  @SneakyThrows
  private static URL toUrl(String url, boolean escaped) {
    return new URL(new URI(url, escaped).toString());
  }

  @SneakyThrows
  private static String toBeforePath(URL url) {
    URI uri = new URI(url.toExternalForm(), false);
    return String.format("%s://%s", url.getProtocol(), url.getAuthority());
  }

  private static Try<URL> createAbsolute(String childOrAbsolute) {
    return Try.of(() -> new URL(childOrAbsolute));
  }

  @SneakyThrows
  private static String toUnescaped(URL url) {
    return new URI(url.toExternalForm(), false).toString();
  }
}
