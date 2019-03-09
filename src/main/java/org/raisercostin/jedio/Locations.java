package org.raisercostin.jedio;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.raisercostin.jedio.classpath.ClasspathLocation;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jedio.url.UrlLocation;
import org.raisercostin.util.sugar;

public class Locations {

  @sugar
  public static RelativeLocation relative(Path path) {
    return relative(path.normalize().toString());
  }

  public static RelativeLocation relative(String path) {
    return RelativeLocation.create(path);
  }

  @sugar
  public static ReferenceLocation dir(String path) {
    return dir(Paths.get(path));
  }

  @sugar
  public static ReferenceLocation dir(File path) {
    return dir(path.toPath());
  }

  public static ReferenceLocation dir(Path path) {
    // check if absolute?
    return new PathLocation(path);
  }

  @sugar
  public static DirLocation existingDir(Path path) {
    return dir(path).mkdirIfNecessary();
  }

  @sugar
  public static DirLocation existingDir(String path) {
    return existingDir(Paths.get(path));
  }

  public static ReadableFileLocation classpath(String path) {
    return new ClasspathLocation(path);
  }

  public static DirLocation classpathDir(String path) {
    return new ClasspathLocation(path);
  }

  public static DirLocation dirFromRelative(String relativePath) {
    return dirFromRelative(relative(relativePath));
  }

  public static DirLocation dirFromRelative(RelativeLocation relative) {
    return current().child(relative).mkdirIfNecessary();
  }

  public static DirLocation current() {
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

  public static UrlLocation url(String url) {
    return new UrlLocation(url);
  }
}
