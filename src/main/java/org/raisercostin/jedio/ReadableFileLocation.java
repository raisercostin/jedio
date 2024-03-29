package org.raisercostin.jedio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import io.vavr.control.Option;
import org.apache.commons.io.IOUtils;
import org.jedio.RichThrowable;
import org.jedio.deprecated;
import org.jedio.functions.JedioFunction;
import org.jedio.functions.JedioProcedure;
import org.jedio.sugar;
import org.raisercostin.jedio.MetaInfo.StreamAndMeta;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.path.PathLocation;
import reactor.core.publisher.Mono;

public interface ReadableFileLocation extends BasicFileLocation {
  org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReadableFileLocation.class);

  @Override
  default ReadableFileLocation asReadableFile() {
    return this;
  }

  Option<String> readIfExists();

  // TODO @Deprecated(message="replace with functional equivalent that also
  // closes the stream: safeInputStream")
  InputStream unsafeInputStream();

  void usingInputStreamReturnVoid(JedioProcedure<InputStream> inputStreamConsumer);

  <R> R usingInputStream(JedioFunction<InputStream, R> inputStreamConsumer);

  <R> R usingInputStreamAndMeta(boolean returnExceptionsAsMeta, JedioFunction<StreamAndMeta, R> inputStreamConsumer);

  StreamAndMeta unsafeInputStreamAndMeta();

  /**
   * Reading async. Uses both internal connection pool and internal thread pool.
   */
  // @deprecated(use = "no suggestion. not implemented yet.", message = "if the content is too big String might be a bad
  // container")
  // TOOD should implement a readContentAsync with a input stream
  default Mono<String> readContentAsync(Charset charset) {
    return Mono.fromSupplier(() -> readContentSync(charset));
  }

  default CompletableFuture<String> readContentAsyncCompletableFuture(Charset charset) {
    return readContentAsync(charset).toFuture();
    // CompletableFuture.
  }

  /**
   * Forces reading synchronously on current thread.
   */
  default String readContentSync(Charset charset) {
    try (BufferedInputStream b = new BufferedInputStream(unsafeInputStream())) {
      return IOUtils.toString(b, charset);
    } catch (IOException e) {
      throw new RuntimeException("Can't read resource [" + this + "]", e);
    }
  }

  String readMetaContent();

  MetaInfo readMeta();

  @sugar
  default ReadableFileLocation copyToFile(WritableFileLocation destination) {
    return copyToFile(destination, CopyOptions.copyDefault());
  }

  ReadableFileLocation copyToFile(WritableFileLocation destination, CopyOptions options);

  Charset charset1_UTF8 = StandardCharsets.UTF_8;
  Charset charset2_ISO_8859_1 = StandardCharsets.ISO_8859_1;

  @sugar("readContentAsync with UTF8 charset")
  default Mono<String> readContentAsync() {
    return readContentAsync(charset1_UTF8);
  }

  @sugar("readContentSync with UTF8 charset")
  default String readContentSync() {
    return readContentSync(charset1_UTF8);
  }

  @sugar("readContent with UTF8 charset")
  @deprecated("Use readConentAsync if possible.")
  @Deprecated
  default String readContent() {
    try {
      return readContent(charset1_UTF8);
    } catch (Exception e) {
      try {
        return readContent(charset2_ISO_8859_1);
      } catch (Exception e2) {
        e2.addSuppressed(e);
        log.debug("Other charsets: {}", Charset.availableCharsets().keySet());
        throw RichThrowable.wrap(e2,
          e.getMessage() + " - Error while reading %s with charsets %s and %s. Enable debug to see available charsets.",
          this,
          charset1_UTF8, charset2_ISO_8859_1);
      }
    }
  }

  /**
   * Reading async if possible and block on current thread. This will force the clients that use async consumers to use
   * the readContentAsync. Reading should happen in 30s. If more control is needed please use readContentAsync().
   */
  @sugar("readContentAsync() and ulterior handling should be used.")
  default String readContent(Charset charset) {
    return readContentAsync().block(Duration.ofSeconds(30));
  }

  default PathLocation toPathLocationCopyIfNotPossible() {
    if (this instanceof PathLocation) {
      return (PathLocation) this;
    }
    return Locations.tempDir("spot-").child(filename()).copyFrom(this, CopyOptions.copyDoNotOverwriteAndThrow());
  }
}
