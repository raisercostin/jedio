package org.raisercostin.jedio;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.vavr.control.Option;
import org.jedio.functions.JedioFunction;
import org.jedio.functions.JedioProcedure;
import org.raisercostin.jedio.MetaInfo.StreamAndMeta;
import org.raisercostin.jedio.op.CopyOptions;
import reactor.core.publisher.Mono;

public interface ReadableFileLocation extends BasicFileLocation {
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

  Charset charset1 = StandardCharsets.UTF_8;
  Charset charset2 = StandardCharsets.ISO_8859_1;

  String readContent();

  String readContent(Charset charset);

  String readMetaContent();

  MetaInfo readMeta();

  Mono<String> readContentAsync();

  WritableFileLocation copyTo(WritableFileLocation destination);

  WritableFileLocation copyTo(WritableFileLocation destination, CopyOptions options);
}
