package org.raisercostin.jedio.fs.stream;

import java.io.InputStream;

import org.raisercostin.jedio.ReadableFileLocation;

public class InputStreamLocation extends AbstractLocation implements ReadableFileLocation {
  private InputStream stream;

  public InputStreamLocation(InputStream stream) {
    this.stream = stream;
  }

  @Override
  public InputStream unsafeInputStream() {
    return stream;
  }
}
