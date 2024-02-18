package org.raisercostin.jedio;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.regex.Matcher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Preconditions;
import io.vavr.Function1;
import io.vavr.Lazy;
import io.vavr.Tuple3;
import io.vavr.control.Either;
import lombok.SneakyThrows;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.jedio.sugar;
import org.jedio.regex.RichRegex;
import org.raisercostin.jedio.classpath.ClasspathLocation;
import org.raisercostin.jedio.fs.stream.InputStreamLocation;
import org.raisercostin.jedio.memory.InMemoryLocation;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jedio.url.HttpClientLocation;
import org.raisercostin.jedio.url.JedioHttpClient;
import org.raisercostin.jedio.url.SimpleUrl;
import org.raisercostin.jedio.url.WebClientLocation;
import org.raisercostin.jedio.url.WebLocation;
import org.raisercostin.jedio.url.impl.ModifiedURI;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;

public class Locations {
  private static Lazy<JedioHttpClient> defaultClient = Lazy.of(() -> JedioHttpClient.createHighPerfHttpClient());

  public enum Scheme {
    http,
    https,
    /**
     * This scheme is a catch all from a string about a file:
     * - starting with '/' is absolute
     * - starting with './' or '../' is relative to current dir or parent of current dir
     * - starting without the other prefixes is just relative (without defining relative to what)
     *   This is not allowed to be relative to current since it creates security risks
     * See other schemes.
     */
    path,
    file,
    relative,
    current
  }

  public static RelativeLocation relative(Path path) {
    return RelativeLocation.relative(path);
  }

  public static RelativeLocation relative(String path) {
    return RelativeLocation.relative(path);
  }

  public static ClasspathLocation classpath(String path) {
    return ClasspathLocation.classpath(path);
  }

  /**Will create the location even if the resource does not exists.*/
  public static ClasspathLocation classpathOptional(String path) {
    return ClasspathLocation.classpathOptional(path);
  }

  public static PathLocation pathFromRelative(String relativePath) {
    return PathLocation.pathFromRelative(relativePath);
  }

  public static PathLocation pathFromRelative(RelativeLocation relative) {
    return PathLocation.pathFromRelative(relative);
  }

  public static PathLocation current() {
    return PathLocation.current();
  }

  public static PathLocation path(Path path) {
    return PathLocation.path(path);
  }

  public static PathLocation path(File file) {
    return PathLocation.path(file);
  }

  public static PathLocation path(String path) {
    return PathLocation.path(path);
  }

  public static PathLocation pathFromExternalForm(String path) {
    return PathLocation.pathFromExternalForm(path);
  }

  public static PathLocation path(java.net.URI uri) {
    return PathLocation.path(uri);
  }

  /**A path under standard java temp folder defined by `java.io.tmpdir`.*/
  public static PathLocation temp() {
    return PathLocation.temp();
  }

  /**A path under standard java temp folder defined by `java.io.tmpdir`.*/
  @sugar
  public static PathLocation tempDir(String prefix) {
    return PathLocation.tempDir(prefix);
  }

  @SneakyThrows
  @sugar
  public static PathLocation tempFile(String prefix, String sufix) {
    return PathLocation.tempFile(prefix, sufix);
  }

  public static InputStreamLocation stream(String name, InputStream inputStream) {
    return InputStreamLocation.stream(name, inputStream);
  }

  public static InMemoryLocation memory(String name, String content) {
    return new InMemoryLocation(name, content);
  }

  public static WebLocation web(String webAddress) {
    return new WebLocation(true, webAddress);
  }

  public static <S extends RequestHeadersUriSpec<S>> WebClientLocation httpGet(String url,
      Function1<RequestHeadersUriSpec<S>, RequestHeadersUriSpec<S>> req) {
    return WebClientLocation.httpGet(url, req);
  }

  public static WebClientLocation httpGet(String url) {
    return WebClientLocation.httpGet(url);
  }

  public static WebClientLocation httpGet(ModifiedURI uri) {
    return WebClientLocation.httpGet(uri);
  }

  public static HttpClientLocation urlApache(String url) {
    return new HttpClientLocation(url, false, defaultClient.get());
  }

  public static HttpClientLocation urlApache(ModifiedURI uri) {
    return urlApache(new SimpleUrl(uri));
  }

  // public static HttpClientLocation url(URL url) {
  // return url(new SimpleUrl(url));
  // }

  public static HttpClientLocation urlApache(SimpleUrl url) {
    return HttpClientLocation.url(url, defaultClient.get());
  }

  @SneakyThrows
  public static HttpClientLocation urlApachePost(String url, String body) {
    return HttpClientLocation.url(url, new HttpPost(), new StringEntity(body), defaultClient.get());
  }

  public static HttpClientLocation urlApache(String sourceHyperlink, String relativeOrAbsoluteHyperlink) {
    return HttpClientLocation.url(sourceHyperlink, relativeOrAbsoluteHyperlink, defaultClient.get());
  }

  public static HttpClientLocation urlApache(String url, JedioHttpClient client) {
    return HttpClientLocation.url(url, client);
  }

  /**Create a location. Shold have a schema.*/
  public static ReadableFileLocation readable(String externalUrl) {
    return (ReadableFileLocation) location(externalUrl);
  }

  /**
   * Create a location. Shold have a schema.
   */
  public static Location location(URL externalUrl) {
    return location(externalUrl.toExternalForm());
  }

  /**
   * Create a location. Shold have a schema.
   */
  @SneakyThrows
  public static Location location(java.net.URI externalUrl) {
    return location(externalUrl.toURL());
  }

  public static Location locationStrict(String url) {
    return location(url, null);
  }

  /**Location via a url with scheme and fallback to path.*/
  public static Location location(String url) {
    return location(url, Scheme.path);
  }

  /**Create a location. Shold have a schema.
   * */
  @JsonCreator
  @SneakyThrows
  public static Location location(String externalUrl, Scheme defaultScheme) {
    Either<String, Tuple3<Matcher, String, String>> schemaAndUrl = RichRegex.regexp2("^([a-z]+)\\:(.*)$",
      externalUrl);
    String schema;
    String path;
    if (schemaAndUrl.isLeft()) {
      if (defaultScheme == null) {
        throw new IllegalArgumentException(
          "Couldn't find a protocol scheme as `<scheme>:<rest>` in [" + externalUrl + "]: " + schemaAndUrl.getLeft()
              + "]");
      }
      schema = defaultScheme.toString();
      path = externalUrl;
    } else {
      schema = schemaAndUrl.get()._2;
      path = schemaAndUrl.get()._3;
    }
    switch (schema) {
      case "http":
      case "https":
        return httpGet(externalUrl);
      case "classpath":
        return classpath(path);
      case "path":
        return pathAbsoluteRelativeOrRelativeToCurrent(externalUrl);
      case "file":
        return pathFromExternalForm(externalUrl);
      case "relative":
        return pathFromRelative(relative(path));
      case "current":
        Preconditions.checkArgument(path.length() == 0, "Current schema should have no relative part.");
        return current();
      case "web":
        return web(path);
      case "mem":
        return mem(path);
      default:
        throw new IllegalArgumentException(
          "Don't know protocol [" + schema + "] for externalUrl [" + externalUrl + "]");
    }
  }

  public static Location pathAbsoluteRelativeOrRelativeToCurrent(String path) {
    if (path.startsWith("/")) {
      return pathFromExternalForm("file://" + path);
    }
    if (path.startsWith("./") || path.startsWith("../")) {
      return current().child(path);
    }
    return relative(path);
  }

  public static String toExternalUri(Location location) {
    return location.toExternalUri();
  }

  private static InMemoryLocation mem(String content) {
    return new InMemoryLocation(content);
  }

  public static ClasspathLocation classpath(Class<?> clazz, String relative) {
    return ClasspathLocation.classpath(clazz, relative);
  }

  public static InputStreamLocation stream(ClassPathResource classPathResource) {
    return InputStreamLocation.stream(classPathResource);
  }

  public static PathLocation path(ClassPathResource classPathResource) {
    return PathLocation.path(classPathResource);
  }

  @SneakyThrows
  public static Location url(ClassPathResource classPathResource) {
    return location(classPathResource.getURL());
  }
}
