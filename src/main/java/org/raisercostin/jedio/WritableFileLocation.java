package org.raisercostin.jedio;

import java.io.InputStream;

import org.jedio.sugar;
import org.raisercostin.jedio.op.CopyOptions;

/** Writable by me or others? */
public interface WritableFileLocation<SELF extends WritableFileLocation<SELF>> extends FileLocation<SELF> {
  SELF write(String content, String encoding);

  @sugar
  default SELF copyFrom(InputStream inputStream) {
    return copyFrom(inputStream, CopyOptions.copyDefault());
  }

  @sugar
  default SELF copyFrom(InputStream inputStream, CopyOptions options) {
    return copyFrom(Locations.stream(inputStream), options);
  }

  SELF copyFrom(ReadableFileLocation<?> source, CopyOptions options);

  @sugar
  default SELF write(String content) {
    return write(content, "UTF-8");
  }
}
