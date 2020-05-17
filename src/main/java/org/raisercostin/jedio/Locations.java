package org.raisercostin.jedio;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.raisercostin.jedio.classpath.ClasspathLocation;
import org.raisercostin.jedio.fs.stream.InputStreamLocation;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jedio.url.HttpClientLocation;
import org.raisercostin.jedio.url.JedioHttpClients;
import org.raisercostin.jedio.url.JedioHttpClients.JedioHttpClient;
import org.raisercostin.util.sugar;

public class Locations {
  private static JedioHttpClient defaultClient = JedioHttpClients.createHighPerfHttpClient();

  @sugar
  public static RelativeLocation relative(Path path) {
    return relative(path.normalize().toString());
  }

  public static RelativeLocation relative(String path) {
    return RelativeLocation.create(path);
  }

  @sugar
  public static PathLocation dir(String path) {
    return dir(Paths.get(path));
  }

  @sugar
  public static PathLocation dir(File path) {
    return dir(path.toPath());
  }

  public static PathLocation dir(Path path) {
    // check if absolute?
    return new PathLocation(path);
  }

  @sugar
  public static PathLocation existingDir(Path path) {
    return dir(path).mkdirIfNecessary();
  }

  @sugar
  public static PathLocation existingDir(String path) {
    return existingDir(Paths.get(path));
  }

  public static ClasspathLocation classpath(String path) {
    return new ClasspathLocation(path);
  }

  public static ClasspathLocation classpathDir(String path) {
    return new ClasspathLocation(path);
  }

  public static PathLocation dirFromRelative(String relativePath) {
    return dirFromRelative(relative(relativePath));
  }

  public static PathLocation dirFromRelative(RelativeLocation relative) {
    return current().child(relative).mkdirIfNecessary();
  }

  public static PathLocation current() {
    return new PathLocation(Paths.get("."));
  }

  public static ExistingLocation existingDirOrFile(Path x) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  public static FileLocation existingFile(Path path) {
    return new PathLocation(path);
  }

  @sugar
  public static FileLocation existingFile(File path) {
    return existingFile(path.toPath());
  }

  @sugar
  public static FileLocation existingFile(String path) {
    return existingFile(Paths.get(path));
  }

  public static ReadableFileLocation readableFile(String path) {
    return existingFile(path).asReadableFile();
  }

  public static WritableFileLocation writableFile(String path) {
    return existingFile(path).asWritableFile();
  }

  public static HttpClientLocation url(String url) {
    return new HttpClientLocation(url, defaultClient);
  }

  public static InputStreamLocation stream(InputStream inputStream) {
    return new InputStreamLocation(inputStream);
  }

  public static WebLocation web(String webAddress) {
    return new WebLocation(true, webAddress);
  }
}
