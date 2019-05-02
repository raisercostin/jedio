package org.raisercostin.jedio;

import java.io.InputStream;

import io.vavr.control.Option;
import reactor.core.publisher.Mono;

public interface ReadableFileLocation extends FileLocation {
  long length();

  Option<String> readIfExists();

  // TODO @Deprecated(message="replace with functional equivalent that also
  // closes the stream: safeInputStream")
  InputStream unsafeInputStream();

  @Deprecated
  String readContent();

  default Mono<String> readContentAsync() {
    return Mono.fromSupplier(() -> readContent());
  }
}
