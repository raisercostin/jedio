package org.raisercostin.jedio;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.vavr.control.Option;
import org.jedio.ExceptionUtils;
import org.jedio.deprecated;
import org.jedio.functions.JedioFunction;
import org.jedio.functions.JedioProcedure;
import org.jedio.sugar;
import org.raisercostin.jedio.MetaInfo.StreamAndMeta;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.nodes.Nodes;
import reactor.core.publisher.Mono;

public interface ReadableFileLocation<SELF extends ReadableFileLocation<SELF>> extends FileLocation<SELF> {
  default Option<String> readIfExists() {
    throw new RuntimeException("Not implemented yet!!!");
  }

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

  default <R> R usingInputStreamAndMeta(boolean returnExceptionsAsMeta,
      JedioFunction<StreamAndMeta, R> inputStreamConsumer) {
    boolean exceptionInConsumer = false;
    try (StreamAndMeta in = unsafeInputStreamAndMeta()) {
      exceptionInConsumer = true;
      return inputStreamConsumer.apply(in);
    } catch (Throwable e) {
      if (returnExceptionsAsMeta && !exceptionInConsumer) {
        try {
          return inputStreamConsumer.apply(StreamAndMeta.fromThrowable(e));
        } catch (Throwable e1) {
          throw ExceptionUtils.nowrap(e1);
        }
      } else {
        throw ExceptionUtils.nowrap(e);
      }
    }
  }

  @Deprecated
  @deprecated("Use usingInputStreamAndMeta. This is needed by implementors.")
  default StreamAndMeta unsafeInputStreamAndMeta() {
    return new StreamAndMeta(null, unsafeInputStream());
  }

  Charset charset1 = StandardCharsets.UTF_8;
  Charset charset2 = StandardCharsets.ISO_8859_1;

  @Deprecated
  @deprecated("If the content is too big String might be a bad container")
  default String readContent() {
    try {
      return readContent(charset1);
    } catch (Exception e) {
      try {
        return readContent(charset2);
      } catch (Exception e2) {
        throw ExceptionUtils.nowrap(e, "While reading %s with charsets %s and %s. Others could exist %s", this,
            charset1, charset2, Charset.availableCharsets().keySet());
      }
    }
  }

  @Deprecated
  @deprecated("If the content is too big String might be a bad container")
  String readContent(Charset charset);

  @Deprecated
  @deprecated("If the content is too big String might be a bad container")
  default String readMetaContent() {
    return meta().readContent();
  }

  default MetaInfo readMeta() {
    try {
      return Nodes.json.toObject(readMetaContent(), MetaInfo.class);
    } catch (Exception e) {
      throw ExceptionUtils.nowrap(e, "While reading metadata of %s", this);
    }
  }

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
