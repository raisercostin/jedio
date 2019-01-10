package org.raisercostin.jedio;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    // check if absolute?
    return folder(Paths.get(path));
  }

  public static ReferenceLocation folder(Path path) {
    return new PathLocation(path);
  }

  public static ReferenceLocation folder(File path) {
    // check if absolute?
    return new PathLocation(path.toPath());
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
}
