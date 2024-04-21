package org.raisercostin.jedio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
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
import org.raisercostin.jedio.op.OperationOptions.ReadOptions;
import org.raisercostin.jedio.path.PathLocation;
import reactor.core.publisher.Mono;

public interface ReadableFileLocation extends BasicFileLocation {
  org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReadableFileLocation.class);

  ReadOptions defaultRead = new ReadOptions();

  static ReadableFileLocation existingFile(Path path) {
    return PathLocation.path(path).asReadableFile();
  }

  @sugar
  static ReadableFileLocation existingFile(File path) {
    return PathLocation.path(path).asReadableFile();
  }

  @sugar
  static ReadableFileLocation existingFile(String path) {
    return PathLocation.path(path).asReadableFile();
  }

  static ReadableFileLocation readableFile(String path) {
    return existingFile(path).asReadableFile();
  }

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

  String readMetaContent();

  MetaInfo readMeta();

  @sugar
  default ReadableFileLocation copyToFile(WritableFileLocation destination) {
    return copyToFile(destination, CopyOptions.copyDefault());
  }

  @sugar
  default WritableFileLocation copyToFileAndReturnIt(WritableFileLocation destination) {
    return copyToFileAndReturnIt(destination, CopyOptions.copyDefault());
  }

  ReadableFileLocation copyToFile(WritableFileLocation destination, CopyOptions options);

  WritableFileLocation copyToFileAndReturnIt(WritableFileLocation destination, CopyOptions options);

  @sugar("readContentAsync with UTF8 charset")
  default Mono<String> readContentAsync() {
    return readContentAsync(defaultRead);
  }

  @sugar("readContentSync with UTF8 charset")
  default String readContentSync() {
    return readContentSync(defaultRead);
  }

  @sugar("readContentSync with UTF8 charset")
  default String readContent() {
    return readContentSync();
  }

  @sugar("readContentSync")
  default String readContent(ReadOptions options) {
    return readContentSync(options);
  }

  @sugar("readContentSync")
  default String readContent(Charset charset) {
    return readContentSync(defaultRead.withDefaultCharset(charset));
  }

  /**
   * Reading async. Uses both internal connection pool and internal thread pool.
   */
  // @deprecated(use = "no suggestion. not implemented yet.", message = "if the content is too big String might be a bad
  // container")
  // TOOD should implement a readContentAsync with a input stream
  default Mono<String> readContentAsync(ReadOptions options) {
    return Mono.fromSupplier(() -> readContentSync(options));
  }

  default CompletableFuture<String> readContentAsyncCompletableFuture(ReadOptions options) {
    return readContentAsync(options).toFuture();
    // CompletableFuture.
  }

  @sugar("readContent with UTF8 charset")
  default String readContentSync(ReadOptions options) {
    try {
      return readContentSync(options, options.defaultCharset);
    } catch (Exception e) {
      try {
        return readContentSync(options, options.fallbackCharset);
      } catch (Exception e2) {
        log.debug("Couldn't read fallback as well", e2);
        log.debug("Other charsets: {}", Charset.availableCharsets().keySet());
        throw RichThrowable.wrap(e,
          "Couldn't read %s charset %s and %s.", this.absoluteAndNormalized(), options.defaultCharset,
          options.fallbackCharset);
        //        throw RichThrowable.wrap(e,
        //          e.getMessage() + " - Error while reading %s with charsets %s and %s. Enable debug to see available charsets.",
        //          this,
        //          options.defaultCharset, options.fallbackCharset);
      }
    }
  }

  /**
   * Forces reading synchronously on current thread.
   */
  default String readContentSync(ReadOptions options, Charset charset) {
    try (BufferedInputStream b = new BufferedInputStream(unsafeInputStream())) {
      return IOUtils.toString(b, charset);
    } catch (IOException e) {
      throw new RuntimeException("Can't read resource [" + this + "]", e);
    }
  }
  //return readContentAsync(defaultRead.withDefaultCharset(charset)).block(defaultRead.blockingReadDuration);

  default PathLocation toPathLocationCopyIfNotPossible() {
    if (this instanceof PathLocation) {
      return (PathLocation) this;
    }
    return Locations.tempDir("spot-").child(filename()).copyFrom(this, CopyOptions.copyDoNotOverwriteAndThrow());
  }
}
