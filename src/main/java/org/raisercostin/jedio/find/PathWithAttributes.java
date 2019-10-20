package org.raisercostin.jedio.find;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import io.vavr.Lazy;

// TODO add lombok
public class PathWithAttributes {
  public final Path path;
  // The attributes are not read except if needed
  final Lazy<BasicFileAttributes> attrs;

  public PathWithAttributes(Path path) {
    this.path = path;
    this.attrs = Lazy.of(() -> readAttrs(path));
  }

  public boolean isDirectory() {
    if (isVirtualDirectory())
      return true;
    if (isInsideVirtualDirectory())
      // TODO fix
      return false;
    return attrs.get().isDirectory();
  }

  public FileTime lastModifiedTime() {
    if (isInsideVirtualDirectory())
      // TODO fix
      return FileTime.fromMillis(0);
    return attrs.get().lastModifiedTime();
  }

  private boolean isInsideVirtualDirectory() {
    // TODO fix this (this allows only one level virtual dir)
    return path.getParent().toString().toLowerCase().endsWith(".pdf");
  }

  public boolean isVirtualDirectory() {
    return path.toString().toLowerCase().endsWith(".pdf");
    // return GuavaAndDirectoryStreamTraversalWithVirtualDirs.this.isVirtualDir.apply(path);
  }

  // could be cached?
  public static BasicFileAttributes readAttrs(Path path) {
    try {
      // attempt to get attrmptes without following links
      return Files.readAttributes(path, BasicFileAttributes.class);
      // attrs = Files.readAttributes(path, BasicFileAttributes.class,
      // LinkOption.NOFOLLOW_LINKS);
    } catch (IOException ioe) {
      try {
        // if (!GuavaAndDirectoryStreamTraversal3.this.followLinks)
        // throw ioe;
        return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
