package org.raisercostin.jedio.find;

import java.nio.file.Path;
import java.util.function.Function;

import org.raisercostin.jedio.find.GuavaAndDirectoryStreamTraversalWithVirtualFolders.PathWithAttributes;

import reactor.core.publisher.Flux;

/**
 * The traversal has two important filters: - performance/pruning filter - that
 * can cut out entire folders from final result (you don't need to test that
 * cuts only folders since this will affect performance. - final filter - to
 * define what is the external result. Files or folders that are matched by both
 * pruning and filter will not be returned.
 */
public interface FileTraversal2 {
  default <T> Flux<T> traverse(Path start, TraversalFilter filter, Function<Path, T> f, boolean recursive) {
    return traverse(start, filter).map(x -> f.apply(x));
  }

  Flux<Path> traverse(Path start, TraversalFilter filter);

  default Flux<PathWithAttributes> traverse2(Path start, TraversalFilter filter, boolean recursive) {
    throw new RuntimeException("Not implemented yet!!!");
  }
}