package org.raisercostin.jedio.find;

import java.nio.file.Path;

import reactor.core.publisher.Flux;

public interface FileTraversal {
  default Flux<Path> traverse(Path start, boolean ignoreCase) {
    return traverse(start, "*", ignoreCase);
  }

  Flux<Path> traverse(Path start, String regex, boolean ignoreCase);
}