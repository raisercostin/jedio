package org.raisercostin.jedio.find;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.raisercostin.jedio.Locations;

import com.google.common.base.Preconditions;
import com.google.common.graph.Traverser;

import io.vavr.Function1;
import io.vavr.Lazy;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import lombok.val;
import reactor.core.publisher.Flux;

/**
 * Reimplemented com.google.common.io.Files.fileTraverser using
 * Files.newDirectoryStream. Use find/gfind like in ``` scoop install gow
 * #https://www.freebsd.org/cgi/man.cgi?query=gfind&manpath=FreeBSD+7.2-RELEASE+and+Ports
 * gfind d:/home/raiser/work/_old-2018-12-31 -printf "%A@ %C@ %d %D %F %G %i %l
 * %m %M %s %y %Y - %h %f %s\n" gfind d:/home/raiser/work/_old-2018-12-31
 * -printf "%10s %y %h %f\n" ls -al ```
 */
public class GuavaAndDirectoryStreamTraversalWithVirtualFolders implements FileTraversal2 {
  private final LinkOption[] options;
  // private final boolean followLinks;
  private Function1<Path, Boolean> isVirtualFolder;

  public GuavaAndDirectoryStreamTraversalWithVirtualFolders(boolean followLinks,
      Function1<Path, Boolean> isVirtualFolder) {
    this.isVirtualFolder = isVirtualFolder;
    // this.followLinks = followLinks;
    if (followLinks)
      options = new LinkOption[0];
    else
      options = new LinkOption[] { LinkOption.NOFOLLOW_LINKS };
  }

  // TODO add lombok
  public class PathWithAttributes {
    public final Path path;
    // The attributes are not read except if needed
    private final Lazy<BasicFileAttributes> attrs;

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
      // TODO fix this (this allows only one level virtual folder)
      return path.getParent().toString().toLowerCase().endsWith(".pdf");
    }

    public boolean isVirtualDirectory() {
      return GuavaAndDirectoryStreamTraversalWithVirtualFolders.this.isVirtualFolder.apply(path);
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
  public Flux<PathWithAttributes> traverse2(Path start, TraversalFilter filter, boolean recursive) {
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
          : newVirtualDirectoryStream(start, createFilter(all));
      return Flux.fromIterable(iterable).map(x -> new PathWithAttributes(x)).filter(path -> filter.matches(path.path))
          .sort(foldersFirst());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private DirectoryStream<Path> newVirtualDirectoryStream(Path start, Filter<Path> filter) throws IOException {
    PathWithAttributes startWithAttr = new PathWithAttributes(start);
    if (startWithAttr.isVirtualDirectory())
      return newActualVirtualDirectoryStream(start, filter);
    else
      return Files.newDirectoryStream(start, filter);
  }

  private DirectoryStream<Path> newActualVirtualDirectoryStream(Path pdfFile, Filter<Path> filter) {
    Preconditions.checkArgument(pdfFile.toString().toLowerCase().endsWith(".pdf"));
    return new DirectoryStream<Path>() {
      @Override
      public void close() throws IOException {
        throw new RuntimeException("Not implemented yet!!!");
      }

      @Override
      public Iterator<Path> iterator() {
        try (val is = Locations.folder(pdfFile).asReadableFile().unsafeInputStream()) {
          val doc = PDDocument.load(is);
          return List.ofAll(doc.getPages()).zipWithIndex().map(pageWithIndex -> toPath(pdfFile, pageWithIndex))
              .iterator();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      private Path toPath(Path pdfFile, Tuple2<PDPage, Integer> pageWithIndex) {
        return pdfFile.resolve("page-" + pageWithIndex._2);
      }
    };
  }

  public Flux<Path> traverse(Path start, TraversalFilter filter) {
    return traverse2(start, filter, false).map(x -> x.path);
  }

  private Comparator<? super PathWithAttributes> foldersFirst() {
    return (x, y) -> Boolean.compare(y.isDirectory(), x.isDirectory());
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