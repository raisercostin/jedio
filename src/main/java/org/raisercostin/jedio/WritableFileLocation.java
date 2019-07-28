package org.raisercostin.jedio;

import java.io.InputStream;

import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.util.sugar;

/** Writable by me or others? */
public interface WritableFileLocation extends FileLocation {
  WritableFileLocation write(String content, String encoding);

  @sugar
  default WritableFileLocation copyFrom(InputStream inputStream) {
    return copyFrom(inputStream, CopyOptions.copyDefault());
  }
  @sugar
  default WritableFileLocation copyFrom(InputStream inputStream, CopyOptions options) {
    return copyFrom(Locations.stream(inputStream),options);
  }

  WritableFileLocation copyFrom(ReadableFileLocation source, CopyOptions options);

  @sugar
  default WritableFileLocation write(String content) {
    return write(content, "UTF-8");
  }
}
