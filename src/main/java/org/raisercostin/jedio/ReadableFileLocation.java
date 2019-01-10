package org.raisercostin.jedio;

import io.vavr.control.Option;
import java.io.InputStream;

public interface ReadableFileLocation extends FileLocation {
  long length();

  Option<String> read();
  // TODO @Deprecated(message="replace with functional equivalent that also closes the stream:
  // safeInputStream")
  InputStream unsafeInputStream();

  String readContent();
}
