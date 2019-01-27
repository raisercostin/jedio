package org.raisercostin.jedio;

import java.io.InputStream;

import org.raisercostin.util.sugar;

/** Writable by me or others? */
public interface WritableFileLocation extends FileLocation {
  WritableFileLocation write(String content, String encoding);

  WritableFileLocation copyFrom(InputStream inputStream);

  @sugar
  default WritableFileLocation write(String content) {
    return write(content, "UTF-8");
  }
}
