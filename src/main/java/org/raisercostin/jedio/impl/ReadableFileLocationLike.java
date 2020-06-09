package org.raisercostin.jedio.impl;

import java.io.InputStream;

import io.vavr.control.Option;
import org.jedio.ExceptionUtils;
import org.jedio.deprecated;
import org.jedio.functions.JedioFunction;
import org.jedio.functions.JedioProcedure;
import org.jedio.sugar;
import org.raisercostin.jedio.MetaInfo;
import org.raisercostin.jedio.MetaInfo.StreamAndMeta;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.nodes.Nodes;

public interface ReadableFileLocationLike<SELF extends ReadableFileLocationLike<SELF>>
    extends ReadableFileLocation, BasicFileLocationLike<SELF> {

  @Override
  default ReadableFileLocation asReadableFile() {
    return ReadableFileLocation.super.asReadableFile();
  }

  @Override
  default Option<String> readIfExists() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  @sugar
  default void usingInputStreamReturnVoid(JedioProcedure<InputStream> inputStreamConsumer) {
    usingInputStream(is -> {
      inputStreamConsumer.apply(is);
      return null;
    });
  }

  @Override
  default <R> R usingInputStream(JedioFunction<InputStream, R> inputStreamConsumer) {
    try (InputStream in = unsafeInputStream()) {
      return inputStreamConsumer.apply(in);
    } catch (Throwable e) {
      throw ExceptionUtils.nowrap(e);
    }
  }

  @Override
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

  @Override
  @Deprecated
  @deprecated("Use usingInputStreamAndMeta. This is needed by implementors.")
  default StreamAndMeta unsafeInputStreamAndMeta() {
    return new StreamAndMeta(null, unsafeInputStream());
  }

  @Override
  @Deprecated
  @deprecated("If the content is too big String might be a bad container")
  default String readMetaContent() {
    return meta().readContent();
  }

  @Override
  default MetaInfo readMeta() {
    try {
      return Nodes.json.toObject(readMetaContent(), MetaInfo.class);
    } catch (Exception e) {
      throw ExceptionUtils.nowrap(e, "While reading metadata of %s", this);
    }
  }

  @Override
  @sugar
  default WritableFileLocation copyTo(WritableFileLocation destination) {
    return copyTo(destination, CopyOptions.copyDefault());
  }

  @Override
  @sugar
  default WritableFileLocation copyTo(WritableFileLocation destination, CopyOptions options) {
    return destination.copyFrom(this, options);
  }
}
