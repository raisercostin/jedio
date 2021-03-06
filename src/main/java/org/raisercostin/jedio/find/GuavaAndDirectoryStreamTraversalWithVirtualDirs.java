package org.raisercostin.jedio.find;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.StreamSupport;

import com.google.common.base.Preconditions;
import com.google.common.graph.Traverser;
import io.vavr.Function1;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.raisercostin.jedio.Locations;
import reactor.core.publisher.Flux;

/**
 * Reimplemented com.google.common.io.Files.fileTraverser using Files.newDirectoryStream. Use find/gfind like in ```
 * scoop install gow #https://www.freebsd.org/cgi/man.cgi?query=gfind&manpath=FreeBSD+7.2-RELEASE+and+Ports gfind
 * d:/home/raiser/work/_old-2018-12-31 -printf "%A@ %C@ %d %D %F %G %i %l %m %M %s %y %Y - %h %f %s\n" gfind
 * d:/home/raiser/work/_old-2018-12-31 -printf "%10s %y %h %f\n" ls -al ```
 */
public class GuavaAndDirectoryStreamTraversalWithVirtualDirs implements FileTraversal2 {
  private final LinkOption[] options;
  // private final boolean followLinks;
  private Function1<Path, Boolean> isVirtualDir;

  public GuavaAndDirectoryStreamTraversalWithVirtualDirs(boolean followLinks, Function1<Path, Boolean> isVirtualDir) {
    this.isVirtualDir = isVirtualDir;
    // this.followLinks = followLinks;
    if (followLinks) {
      this.options = new LinkOption[0];
    } else {
      this.options = new LinkOption[] { LinkOption.NOFOLLOW_LINKS };
    }
  }

  // TODO should return Path and attributes since they are already read.
  // TODO If symlink should resolve destination and list without following
  // symlinks and resolve using toFile().isDirectory
  // .toRealPath()
  @Override
  public Flux<PathWithAttributes> traverse2(Path start, TraversalFilter filter) {
    try {
      PathMatcher all = new PathMatcher()
        {
          @Override
          public boolean matches(Path path) {
            if (filter.shouldPrune(path)) {
              return false;
            }
            if (isDirectory(path)) {
              return true;
            }
            return filter.matches(path);
          }

          private boolean isDirectory(Path path) {
            return PathWithAttributes.readAttrs(path).isDirectory();
          }
        };
      Iterable<Path> iterable = filter.recursive() ? fileTraverser(createFilter(all)).depthFirstPreOrder(start)
          : newVirtualDirectoryStream(start, createFilter(all));
      final Flux<PathWithAttributes> all2 = Flux.fromStream(StreamSupport.stream(iterable.spliterator(), false))
        .map(x -> new PathWithAttributes(x))
        .filter(path -> filter.matches(path.path));
      if (filter.dirsFirstInRecursive()) {
        return all2.sort(dirsFirst());
      } else {
        return all2;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private DirectoryStream<Path> newVirtualDirectoryStream(Path start, Filter<Path> filter) throws IOException {
    PathWithAttributes startWithAttr = new PathWithAttributes(start);
    if (startWithAttr.isVirtualDirectory()) {
      return newActualVirtualDirectoryStream(start, filter);
    } else {
      return Files.newDirectoryStream(start, filter);
    }
  }

  private DirectoryStream<Path> newActualVirtualDirectoryStream(Path pdfFile, Filter<Path> filter) {
    Preconditions.checkArgument(pdfFile.toString().toLowerCase().endsWith(".pdf"));
    return new DirectoryStream<Path>()
      {
        @Override
        public void close() {
          throw new RuntimeException("Not implemented yet!!!");
        }

        @Override
        public Iterator<Path> iterator() {
          return Locations.path(pdfFile).asReadableFile().usingInputStream((InputStream is) -> {
            try (PDDocument doc = PDDocument.load(is)) {
              return List.ofAll(doc.getPages())
                .zipWithIndex()
                .map(pageWithIndex -> toPath(pdfFile, pageWithIndex))
                .iterator();
            }
          });
        }

        private Path toPath(Path pdfFile, Tuple2<PDPage, Integer> pageWithIndex) {
          return pdfFile.resolve("page-" + pageWithIndex._2);
        }
      };
  }

  @Override
  public Flux<Path> traverse(Path start, TraversalFilter filter) {
    return traverse2(start, filter).map(x -> x.path);
  }

  private Comparator<? super PathWithAttributes> dirsFirst() {
    return (x, y) -> Boolean.compare(y.isDirectory(), x.isDirectory());
  }

  private Traverser<Path> fileTraverser(Filter<Path> filter) {
    return Traverser.forTree(file -> fileTreeChildren(file, filter));
  }

  private Iterable<Path> fileTreeChildren(Path file, Filter<Path> filter) {
    // check isDirectory() just because it may be faster than listFiles() on a
    // non-directory
    if (Files.isDirectory(file, this.options)) {
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
    return path -> all.matches(path);
  }
}
