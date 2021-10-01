package org.raisercostin.jedio.fs.stream;

import java.io.InputStream;

import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;
import org.springframework.core.io.ClassPathResource;

public class InputStreamLocation
    implements ReadableFileLocation, ReadableFileLocationLike<@NonNull InputStreamLocation> {
  public static InputStreamLocation stream(InputStream inputStream) {
    return new InputStreamLocation(inputStream);
  }

  @SneakyThrows
  public static InputStreamLocation stream(ClassPathResource classPathResource) {
    return stream(classPathResource.getInputStream());
  }

  private InputStream stream;

  public InputStreamLocation(InputStream stream) {
    this.stream = stream;
  }

  @Override
  public InputStream unsafeInputStream() {
    return this.stream;
  }
}
