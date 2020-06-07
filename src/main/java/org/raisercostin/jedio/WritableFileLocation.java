package org.raisercostin.jedio;

import java.io.InputStream;

import org.raisercostin.jedio.impl.ReadableFileLocationLike;
import org.raisercostin.jedio.op.CopyOptions;

public interface WritableFileLocation extends BasicFileLocation {

  WritableFileLocation write(String content, String encoding);

  WritableFileLocation copyFrom(InputStream inputStream);

  WritableFileLocation copyFrom(InputStream inputStream, CopyOptions options);

  WritableFileLocation copyFrom(ReadableFileLocationLike<?> source, CopyOptions options);

  WritableFileLocation write(String content);
}