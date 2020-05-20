package org.raisercostin.jedio;

import java.io.InputStream;

import io.vavr.collection.Map;
import io.vavr.control.Option;
import org.jedio.ExceptionUtils;
import org.jedio.NodeUtils;
import org.jedio.deprecated;
import org.raisercostin.jedio.MetaInfo.StreamAndMeta;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.nodes.Nodes;
import org.raisercostin.util.functions.JedioFunction;
import org.raisercostin.util.functions.JedioProcedure;
import org.raisercostin.util.sugar;
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

  @Deprecated
  @deprecated("If the content is too big String might be a bad container")
  String readContent();

  @Deprecated
  @deprecated("If the content is too big String might be a bad container")
  default String readMetaContent() {
    return meta().readContent();
  }

  public static class Meta {
    boolean isSuccess;
    Map<String, Object> payload;

    @SuppressWarnings("unchecked")
    public Option<String> field(String pointSelector) {
      String[] keys = pointSelector.split("[.]");
      return Option.of(NodeUtils.nullableString(this, keys));
    }
  }

  default Meta readMeta() {
    return Nodes.json.toObject(readMetaContent(), Meta.class);
  }

  default ReadableFileLocation<SELF> meta() {
    return withExtension(ext -> ext + "-meta-json");
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
