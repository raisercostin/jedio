package org.raisercostin.jedio.find;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Comparator;

import com.google.common.graph.Traverser;

import io.vavr.Lazy;
import reactor.core.publisher.Flux;

/**
 * Reimplemented com.google.common.io.Files.fileTraverser using
 * Files.newDirectoryStream. Use find/gfind like in ``` scoop install gow
 * #https://www.freebsd.org/cgi/man.cgi?query=gfind&manpath=FreeBSD+7.2-RELEASE+and+Ports
 * gfind d:/home/raiser/work/_old-2018-12-31 -printf "%A@ %C@ %d %D %F %G %i %l
 * %m %M %s %y %Y - %h %f %s\n" gfind d:/home/raiser/work/_old-2018-12-31
 * -printf "%10s %y %h %f\n" ls -al ```
 */
public class GuavaAndDirectoryStreamTraversal3 implements FileTraversal2 {
  private final LinkOption[] options;
  // private final boolean followLinks;

  public GuavaAndDirectoryStreamTraversal3(boolean followLinks) {
    // this.followLinks = followLinks;
    if (followLinks)
      options = new LinkOption[0];
    else
      options = new LinkOption[] { LinkOption.NOFOLLOW_LINKS };
  }

  // TODO add lombok
  public static class PathWithAttributes {
    public final Path path;
    // The attributes are not read except if needed
    public final Lazy<BasicFileAttributes> attrs;

    public PathWithAttributes(Path path) {
      this.path = path;
      this.attrs = Lazy.of(() -> readAttrs(path));
    }

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

  // TODO should return Path and attributes since they are already read.
  // TODO If symlink should resolve destination and list without following
  // symlinks and resolve using toFile().isDirectory
  // .toRealPath()
  public Flux<PathWithAttributes> traverse(Path start, TraversalFilter filter, boolean recursive) {
    try {
      PathMatcher all = new PathMatcher() {
        @Override
        public boolean matches(Path path) {
          if (filter.shouldPrune(path))
            return false;
          if (isDirectory(path))
            return true;
          return filter.matches(path);
        }

        private boolean isDirectory(Path path) {
          return readAttrs(path).isDirectory();
        }
      };
      Iterable<Path> iterable = recursive ? fileTraverser(createFilter(all)).depthFirstPreOrder(start)
          : Files.newDirectoryStream(start, createFilter(all));
      return Flux.fromIterable(iterable).map(x -> new PathWithAttributes(x)).filter(path -> filter.matches(path.path))
          .sort(foldersFirst());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Flux<Path> traverse(Path start, TraversalFilter filter) {
    return traverse(start, filter, false).map(x -> x.path);
  }

  private Comparator<? super PathWithAttributes> foldersFirst() {
    return (x, y) -> Boolean.compare(y.attrs.get().isDirectory(), x.attrs.get().isDirectory());
  }

  private Traverser<Path> fileTraverser(Filter<Path> filter) {
    return Traverser.forTree(file -> fileTreeChildren(file, filter));
  }

  private Iterable<Path> fileTreeChildren(Path file, Filter<Path> filter) {
    // check isDirectory() just because it may be faster than listFiles() on a
    // non-directory
    if (Files.isDirectory(file, options)) {
      try {
        DirectoryStream<Path> files = Files.newDirectoryStream(file, filter);
        if (files != null) {
          return files;
          // return Collections.unmodifiableList(Arrays.asList(files));
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return Collections.emptyList();
  }

  // copied from newDirectoryStream(file,regex)
  private static Filter<Path> createFilter(PathMatcher all) throws IOException {
    return new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path path) {
        return all.matches(path);
      }
    };
  }
}