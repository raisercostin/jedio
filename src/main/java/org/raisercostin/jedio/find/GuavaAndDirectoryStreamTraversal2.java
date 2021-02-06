package org.raisercostin.jedio.find;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import reactor.core.publisher.Flux;

/**
 * Reimplemented com.google.common.io.Files.fileTraverser using Files.newDirectoryStream.
 */
public class GuavaAndDirectoryStreamTraversal2 implements FileTraversal2 {
  private static LinkOption[] options = { LinkOption.NOFOLLOW_LINKS };

  @SuppressWarnings("resource")
  @Override
  public Flux<Path> traverse(Path start, TraversalFilter filter) {
    try {
      PathMatcher all = path -> {
        if (filter.shouldPrune(path)) {
          return false;
        }
        if (Files.isDirectory(path)) {
          return true;
        }
        return filter.matches(path);
      };
      Filter<Path> filter2 = createFilter(all);
      DirectoryStream<Path> iterable =
          // fileTraverser(filter2).depthFirstPreOrder(start);
          // .breadthFirst(start);
          Files.newDirectoryStream(start, filter2);

      Stream<Path> a = StreamSupport
        .stream(Spliterators.spliteratorUnknownSize(iterable.iterator(), Spliterator.DISTINCT), false)
        .onClose(asUncheckedRunnable(iterable));
      return Flux.fromStream(a)
        .filter(path -> filter.matches(path))
        .sort((x, y) -> Boolean.compare(Files.isDirectory(y, options), Files.isDirectory(x, options)));
      // .map(x->{System.out.println("found "+x);return x;});
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Runnable asUncheckedRunnable(Closeable c) {
    return () -> {
      try {
        c.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  // copied from newDirectoryStream(file,regex)
  public static Filter<Path> createFilter(PathMatcher all) throws IOException {
    return path -> all.matches(path);
  }
}
