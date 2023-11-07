package org.raisercostin.jedio.impl;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jedio.sugar;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.op.CopyOptions;

/** Writable by me or others? */
public interface WritableFileLocationLike<SELF extends WritableFileLocationLike<SELF>>
    extends WritableFileLocation, BasicFileLocationLike<SELF> {
  @Override
  default SELF write(String content, String charset) {
    return write(content, Charset.forName(charset));
  }

  @Override
  SELF write(String content, Charset charset);

  @Override
  @sugar
  default SELF copyFrom(InputStream inputStream) {
    return copyFrom(inputStream, CopyOptions.copyDefault());
  }

  @Override
  @sugar
  default SELF copyFrom(InputStream inputStream, CopyOptions options) {
    return copyFrom(Locations.stream(null, inputStream), options);
  }

  @Override
  SELF copyFrom(ReadableFileLocation source, CopyOptions options);

  @Override
  @sugar
  default SELF write(String content) {
    return write(content, StandardCharsets.UTF_8);
  }

  default SELF writeContentIfNotExists(String content) {
    if (exists()) {
      return (SELF) this;
    }
    return write(content, StandardCharsets.UTF_8);
  }
}
