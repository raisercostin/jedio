package org.raisercostin.jedio.classpath;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Preconditions;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.raisercostin.jedio.ChangeableLocation;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.ExistingLocation;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.ReadableDirLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.fs.stream.AbstractLocation;
import org.raisercostin.jedio.path.PathLocation;
import reactor.core.publisher.Flux;

/**
 * What is Absolute, Relative and Canonical Path
 * <li>https://javarevisited.blogspot.com/2014/08/difference-between-getpath-getabsolutepath-getcanonicalpath-java.html
 * <li>https://stackoverflow.com/questions/30920623/difference-between-getcanonicalpath-and-torealpath
 *
 * @author raiser
 */
@Data
public class ClasspathLocation extends AbstractLocation implements ReadableDirLocation, ExistingLocation, ReadableFileLocation {
  private static final ClassLoader specialClassLoader = Option.of(ClasspathLocation.class.getClassLoader())
      .getOrElse(ClassLoader.class.getClassLoader());

  private final String path;

  public ClasspathLocation(String path) {
    this.path = fixPath(path);
  }

  private String fixPath(String path) {
    return org.springframework.util.StringUtils.cleanPath(path);
  }

  @Override
  public Option<String> readIfExists() {
    return Try.ofSupplier(() -> readContent()).toOption();
  }

  @Override
  public String absoluteAndNormalized() {
    return Locations.existingFile(toPath()).absoluteAndNormalized();
  }

  private PathLocation toPathLocation() {
    return new PathLocation(toPath());
  }

  private Path toPath() {
    try {
      return Paths.get(ClasspathLocation.class.getClassLoader().getResource(this.path).toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean exists() {
    try (InputStream stream = ClasspathLocation.class.getClassLoader().getResourceAsStream(path)) {
      return stream != null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ReferenceLocation child(RelativeLocation child) {
    return Locations.classpath(fixPath(path + "/" + child.getLocation()));
  }

  @Override
  public InputStream unsafeInputStream() {
    final InputStream res = specialClassLoader.getResourceAsStream(path);
    Preconditions.checkNotNull(res);
    return res;
  }

  public String readContent() {
    try (BufferedInputStream b = new BufferedInputStream(ClasspathLocation.class.getClassLoader().getResourceAsStream(path))) {
      return IOUtils.toString(b, "UTF-8");
    } catch (IOException e) {
      throw new RuntimeException("Can't read resource [" + path + "]", e);
    }
  }

  @Override
  public ClasspathLocation create(String path) {
    return new ClasspathLocation(path);
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    return this;
  }

  @Override
  public ChangeableLocation asChangableLocation() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<ExistingLocation> findFilesAndDirs(boolean recursive) {
    return toPathLocation().findFilesAndDirs(recursive);
  }
}
