package org.raisercostin.jedio;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.vavr.Lazy;
import org.apache.commons.httpclient.URI;
import org.jedio.sugar;
import org.raisercostin.jedio.classpath.ClasspathLocation;
import org.raisercostin.jedio.fs.stream.InputStreamLocation;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jedio.url.HttpClientLocation;
import org.raisercostin.jedio.url.JedioHttpClient;
import org.raisercostin.jedio.url.SimpleUrl;
import org.raisercostin.jedio.url.WebLocation;

public class Locations {
  private static Lazy<JedioHttpClient> defaultClient = Lazy.of(() -> JedioHttpClient.createHighPerfHttpClient());

  @sugar
  public static RelativeLocation relative(Path path) {
    return relative(path.normalize().toString());
  }

  public static RelativeLocation relative(String path) {
    return RelativeLocation.create(path);
  }

  public static ClasspathLocation classpath(String path) {
    return new ClasspathLocation(path);
  }

  public static PathLocation pathFromRelative(String relativePath) {
    return pathFromRelative(relative(relativePath));
  }

  public static PathLocation pathFromRelative(RelativeLocation relative) {
    return current().child(relative).mkdirIfNeeded();
  }

  public static PathLocation current() {
    return new PathLocation(Paths.get("."));
  }

  public static PathLocation path(Path path) {
    return new PathLocation(path);
  }

  @sugar
  public static PathLocation path(File path) {
    return path(path.toPath());
  }

  @sugar
  public static PathLocation path(String path) {
    return path(Paths.get(path));
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

  public static HttpClientLocation url(String sourceHyperlink, String relativeOrAbsoluteHyperlink) {
    return HttpClientLocation.url(sourceHyperlink, relativeOrAbsoluteHyperlink, defaultClient.get());
  }

  public static HttpClientLocation url(String url, JedioHttpClient client) {
    return new HttpClientLocation(url, false, client);
  }
}
