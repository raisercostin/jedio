package org.raisercostin.jedio.fs.mem;

import java.io.InputStream;
import java.nio.charset.Charset;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;

public class InMemoryLocation implements ReadableFileLocation, ReadableFileLocationLike<InMemoryLocation> {
  private String data;

  public InMemoryLocation(String data) {
    this.data = data;
  }

  @Override
  public InputStream unsafeInputStream() {
    return IOUtils.toInputStream(data, Charsets.UTF_8);
  }

  @Override
  public String readContent(Charset charset) {
    return data;
  }
}
