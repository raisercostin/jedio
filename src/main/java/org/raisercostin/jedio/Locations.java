package org.raisercostin.jedio;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

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
  public static ReferenceLocation folder(String path) {
    return folder(Paths.get(path));
  }

  @sugar
  public static ReferenceLocation folder(File path) {
    return folder(path.toPath());
  }

  public static ReferenceLocation folder(Path path) {
    // check if absolute?
    return new PathLocation(path);
  }

  @sugar
  public static FolderLocation existingFolder(Path path) {
    return folder(path).mkdirIfNecessary();
  }

  @sugar
  public static FolderLocation existingFolder(String path) {
    return existingFolder(Paths.get(path));
  }

  public static ReadableFileLocation classpath(String path) {
    return new ClasspathLocation(path);
  }

  public static FolderLocation classpathFolder(String path) {
    return new ClasspathLocation(path);
  }

  public static FolderLocation folderFromRelative(String relativePath) {
    return folderFromRelative(relative(relativePath));
  }

  public static FolderLocation folderFromRelative(RelativeLocation relative) {
    return current().child(relative).mkdirIfNecessary();
  }

  public static FolderLocation current() {
    return new PathLocation(Paths.get("."));
  }

  public static ExistingLocation existingFolderOrFile(Path x) {
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
