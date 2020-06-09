package org.raisercostin.jedio.fs.stream;

import java.io.InputStream;
import java.nio.charset.Charset;

import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;

public class InputStreamLocation implements ReadableFileLocation, ReadableFileLocationLike<InputStreamLocation> {
  private InputStream stream;

  public InputStreamLocation(InputStream stream) {
    this.stream = stream;
  }

  @Override
  public InputStream unsafeInputStream() {
    return stream;
  }

  @Override
  public String readContentSync(Charset charset) {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
