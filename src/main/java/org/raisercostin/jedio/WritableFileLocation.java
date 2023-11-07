package org.raisercostin.jedio;

import java.io.InputStream;
import java.nio.charset.Charset;

import org.raisercostin.jedio.op.CopyOptions;

public interface WritableFileLocation extends BasicFileLocation {
  WritableFileLocation write(String content, Charset charset);

  default WritableFileLocation write(String content, String charset) {
    return write(content, Charset.forName(charset));
  }

  <T extends WritableFileLocation> T copyFrom(InputStream inputStream);

  <T extends WritableFileLocation> T copyFrom(InputStream inputStream, CopyOptions options);

  <T extends WritableFileLocation> T copyFrom(ReadableFileLocation source, CopyOptions options);

  <T extends WritableFileLocation> T write(String content);
}
