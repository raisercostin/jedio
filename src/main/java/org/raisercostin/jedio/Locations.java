package org.raisercostin.jedio;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Preconditions;
import io.vavr.Lazy;
import io.vavr.Tuple3;
import io.vavr.control.Either;
import lombok.SneakyThrows;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jedio.sugar;
import org.jedio.regex.RichRegex;
import org.raisercostin.jedio.classpath.ClasspathLocation;
import org.raisercostin.jedio.fs.stream.InputStreamLocation;
import org.raisercostin.jedio.memory.InMemoryLocation;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jedio.url.HttpClientLocation;
import org.raisercostin.jedio.url.JedioHttpClient;
import org.raisercostin.jedio.url.SimpleUrl;
import org.raisercostin.jedio.url.WebLocation;
import org.raisercostin.jedio.url.impl.URI;

public class Locations {
  private static Lazy<JedioHttpClient> defaultClient = Lazy.of(() -> JedioHttpClient.createHighPerfHttpClient());

  public static RelativeLocation relative(Path path) {
    return RelativeLocation.relative(path);
  }

  public static RelativeLocation relative(String path) {
    return RelativeLocation.relative(path);
  }

  public static ClasspathLocation classpath(String path) {
    return ClasspathLocation.classpath(path);
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

  public static PathLocation temp() {
    return PathLocation.temp();
  }

  public static InputStreamLocation stream(InputStream inputStream) {
    return new InputStreamLocation(inputStream);
  }

  public static WebLocation web(String webAddress) {
    return new WebLocation(true, webAddress);
  }

  public static HttpClientLocation url(String url) {
    return new HttpClientLocation(url, false, defaultClient.get());
  }

  public static HttpClientLocation url(URI uri) {
    return url(new SimpleUrl(uri));
  }

  // public static HttpClientLocation url(URL url) {
  // return url(new SimpleUrl(url));
  // }

  public static HttpClientLocation url(SimpleUrl url) {
    return HttpClientLocation.url(url, defaultClient.get());
  }

  @SneakyThrows
  public static HttpClientLocation urlPost(String url, String body) {
    return HttpClientLocation.url(url, new HttpPost(), new StringEntity(body), defaultClient.get());
  }

  public static HttpClientLocation url(String sourceHyperlink, String relativeOrAbsoluteHyperlink) {
    return HttpClientLocation.url(sourceHyperlink, relativeOrAbsoluteHyperlink, defaultClient.get());
  }

  public static HttpClientLocation url(String url, JedioHttpClient client) {
    return HttpClientLocation.url(url, client);
  }

  /**Create a location. Shold have a schema.*/
  public static ReadableFileLocation readable(String externalUrl) {
    return (ReadableFileLocation) location(externalUrl);
  }

  /**Create a location. Shold have a schema.*/
  @JsonCreator
  @SneakyThrows
  public static Location location(String externalUrl) {
    Either<String, Tuple3<Matcher, String, String>> schemaAndUrl2 = RichRegex.regexp2("^([a-z]+)\\:(.*)$",
      externalUrl);
    if (schemaAndUrl2.isLeft()) {
      throw new IllegalArgumentException(
        "Couldn't find a protocol scheme as `<scheme>:<rest>` in [" + externalUrl + "]: " + schemaAndUrl2.getLeft()
            + "]");
    }
    Tuple3<Matcher, String, String> schemaAndUrl = schemaAndUrl2.get();
    switch (schemaAndUrl._2) {
      case "http":
      case "https":
        return url(externalUrl);
      case "classpath":
        return classpath(schemaAndUrl._3);
      case "file":
        return pathFromExternalForm(externalUrl);
      case "relative":
        return pathFromRelative(relative(schemaAndUrl._3));
      case "current":
        Preconditions.checkArgument(schemaAndUrl._3.length() == 0, "Current schema should have no relative part.");
        return current();
      case "web":
        return web(schemaAndUrl._3);
      case "mem":
        return mem(schemaAndUrl._3);
      default:
        throw new IllegalArgumentException(
          "Don't know protocol [" + schemaAndUrl._2 + "] for externalUrl [" + externalUrl + "]");
    }
  }

  public static String toExternalUri(Location location) {
    return location.toExternalUri();
  }

  private static InMemoryLocation mem(String content) {
    return new InMemoryLocation(content);
  }
}
