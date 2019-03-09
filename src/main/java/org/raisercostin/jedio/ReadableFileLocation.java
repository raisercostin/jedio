package org.raisercostin.jedio;

import java.io.InputStream;

import io.vavr.control.Option;

public interface ReadableFileLocation extends FileLocation {
  long length();

  Option<String> readIfExists();

  // TODO @Deprecated(message="replace with functional equivalent that also
  // closes the stream: safeInputStream")
  InputStream unsafeInputStream();

  String readContent();
}
