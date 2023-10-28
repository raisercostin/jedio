package org.raisercostin.jedio.fs.stream;

import java.io.InputStream;

import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;
import org.springframework.core.io.ClassPathResource;

public class InputStreamLocation
    implements ReadableFileLocation, ReadableFileLocationLike<@NonNull InputStreamLocation> {
  /**Name is useful for extension.*/
  public static InputStreamLocation stream(String name, InputStream inputStream) {
    return new InputStreamLocation(name, inputStream);
  }

  @SneakyThrows
  public static InputStreamLocation stream(ClassPathResource classPathResource) {
    return stream(classPathResource.getFilename(), classPathResource.getInputStream());
  }

  public final String name;
  public final InputStream stream;

  public InputStreamLocation(String name, InputStream stream) {
    this.name = name;
    this.stream = stream;
  }

  @Override
  public InputStream unsafeInputStream() {
    return this.stream;
  }

  @Override
  public boolean isFile() {
    return true;
  }

  @Override
  public boolean isDir() {
    return false;
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public String absoluteAndNormalized() {
    return "stream://" + name;
  }
}
