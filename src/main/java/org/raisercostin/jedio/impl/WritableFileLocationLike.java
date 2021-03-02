package org.raisercostin.jedio.impl;

import java.io.InputStream;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jedio.sugar;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.op.CopyOptions;

/** Writable by me or others? */
public interface WritableFileLocationLike<SELF extends @NonNull WritableFileLocationLike<SELF>>
    extends WritableFileLocation, BasicFileLocationLike<SELF> {
  @Override
  SELF write(String content, String encoding);

  @Override
  @sugar
  default SELF copyFrom(InputStream inputStream) {
    return copyFrom(inputStream, CopyOptions.copyDefault());
  }

  @Override
  @sugar
  default SELF copyFrom(InputStream inputStream, CopyOptions options) {
    return copyFrom(Locations.stream(inputStream), options);
  }

  @Override
  SELF copyFrom(ReadableFileLocation source, CopyOptions options);

  @Override
  @sugar
  default SELF write(String content) {
    return write(content, "UTF-8");
  }
}
