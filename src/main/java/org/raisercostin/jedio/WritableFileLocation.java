package org.raisercostin.jedio;

import java.io.InputStream;
import java.nio.charset.Charset;

import org.raisercostin.jedio.op.CopyOptions;

public interface WritableFileLocation extends BasicFileLocation {
  WritableFileLocation write(String content, Charset charset);

  default WritableFileLocation write(String content, String charset) {
    return write(content, Charset.forName(charset));
  }

  WritableFileLocation copyFrom(InputStream inputStream);

  WritableFileLocation copyFrom(InputStream inputStream, CopyOptions options);

  WritableFileLocation copyFrom(ReadableFileLocation source, CopyOptions options);

  WritableFileLocation write(String content);
}
