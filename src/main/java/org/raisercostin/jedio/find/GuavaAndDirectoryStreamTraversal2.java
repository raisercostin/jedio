package org.raisercostin.jedio.find;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import reactor.core.publisher.Flux;

/**
 * Reimplemented com.google.common.io.Files.fileTraverser using
 * Files.newDirectoryStream.
 */
public class GuavaAndDirectoryStreamTraversal2 implements FileTraversal2 {
  private static LinkOption[] options = { LinkOption.NOFOLLOW_LINKS };

  public Flux<Path> traverse(Path start, TraversalFilter filter) {
    try {
      PathMatcher all = new PathMatcher() {
        @Override
        public boolean matches(Path path) {
          if (filter.shouldPrune(path))
            return false;
          if (Files.isDirectory(path))
            return true;
          return filter.matches(path);
        }
      };
      Filter<Path> filter2 = createFilter(all);
      Iterable<Path> iterable =
          // fileTraverser(filter2).depthFirstPreOrder(start);
          // .breadthFirst(start);
          Files.newDirectoryStream(start, filter2);
      return Flux.fromIterable(iterable).filter(path -> filter.matches(path))
          .sort((x, y) -> Boolean.compare(Files.isDirectory(y, options), Files.isDirectory(x, options)));
      // .map(x->{System.out.println("found "+x);return x;});
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // copied from newDirectoryStream(file,regex)
  public static Filter<Path> createFilter(PathMatcher all) throws IOException {
    return new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(Path path) {
        return all.matches(path);
      }
    };
  }
}