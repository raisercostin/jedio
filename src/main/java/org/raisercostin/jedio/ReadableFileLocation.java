package org.raisercostin.jedio;

import java.io.InputStream;

import io.vavr.control.Option;
import org.jedio.ExceptionUtils;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.util.functions.JedioFunction;
import org.raisercostin.util.functions.JedioProcedure;
import org.raisercostin.util.sugar;
import reactor.core.publisher.Mono;

public interface ReadableFileLocation extends FileLocation {
  long length();

  Option<String> readIfExists();

  // TODO @Deprecated(message="replace with functional equivalent that also
  // closes the stream: safeInputStream")
  @Deprecated
  InputStream unsafeInputStream();

  @sugar
  default void usingInputStreamReturnVoid(JedioProcedure<InputStream> inputStreamConsumer) {
    usingInputStream(is -> {
      inputStreamConsumer.apply(is);
      return null;
    });
  }

  default <R> R usingInputStream(JedioFunction<InputStream, R> inputStreamConsumer) {
    try (InputStream in = unsafeInputStream()) {
      return inputStreamConsumer.apply(in);
    } catch (Throwable e) {
      throw ExceptionUtils.nowrap(e);
    }
  }

  @Deprecated
  String readContent();

  default Mono<String> readContentAsync() {
    return Mono.fromSupplier(() -> readContent());
  }

  @sugar
  default WritableFileLocation copyTo(WritableFileLocation destination) {
    return copyTo(destination, CopyOptions.copyDefault());
  }

  @sugar
  default WritableFileLocation copyTo(WritableFileLocation destination, CopyOptions options) {
    return destination.copyFrom(this, options);
  }
}
