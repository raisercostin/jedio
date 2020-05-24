package org.raisercostin.jedio.classpath;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jedio.ExceptionUtils;
import org.raisercostin.jedio.ExistingLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.ReadableDirLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.RelativeLocation;
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
public class ClasspathLocation implements ReadableDirLocation<ClasspathLocation>, ExistingLocation<ClasspathLocation>,
    ReadableFileLocation<ClasspathLocation> {
  private static final ClassLoader specialClassLoader = Option.of(ClasspathLocation.class.getClassLoader())
      .getOrElse(ClassLoader.class.getClassLoader());

  private static URL toUrl(String resourcePath) {
    URL res = specialClassLoader.getResource(resourcePath);
    Preconditions.checkNotNull(res, "Couldn't get a stream for resourcePath=[%s]", resourcePath);
    return res;
  }

  private final String resourcePath;
  private final URL resourceUrl;

  public ClasspathLocation(String path) {
    resourcePath = fixPath(path);
    resourceUrl = toUrl(resourcePath);
  }

  private String fixPath(String path) {
    return org.springframework.util.StringUtils.cleanPath(path);
  }

  @Override
  public Option<String> readIfExists() {
    return Try.ofSupplier(() -> readContent()).toOption();
  }

  @Override
  @SneakyThrows
  public String absoluteAndNormalized() {
    URL url = resourceUrl;
    String x = url.toURI().getPath();
    if (x == null) {
      return url.toURI().toString();
    } else if (SystemUtils.IS_OS_WINDOWS) {
      return StringUtils.removeStart(x, "/");
    } else {
      return x;
      // return Locations.existingFile(toUrl()).absoluteAndNormalized();
    }
  }

  private PathLocation toPathLocation() {
    return new PathLocation(toPath());
  }

  private Path toPath() {
    return ExceptionUtils.tryWithSuppressed(() -> {
      URL resource = ClasspathLocation.class.getClassLoader().getResource(resourcePath);
      System.out.println("resource=" + resource);
      Preconditions.checkNotNull(resource);
      URI uri = resource.toURI();
      initFileSystem(uri);
      System.out.println("uri=" + uri);
      Preconditions.checkNotNull(uri);
      return Paths.get(uri);
    }, "When trying to read resource [%s]", resourcePath);
  }

  private FileSystem initFileSystem(URI uri) throws IOException {
    try {
      return FileSystems.getFileSystem(uri);
    } catch (FileSystemNotFoundException e) {
      Map<String, String> env = new HashMap<>();
      env.put("create", "true");
      return FileSystems.newFileSystem(uri, env);
    }
  }

  @Override
  public boolean exists() {
    try (InputStream stream = unsafeInputStream()) {
      return stream != null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ClasspathLocation child(RelativeLocation child) {
    return Locations.classpath(fixPath(resourcePath + "/" + child.getLocation()));
  }
  //
  // private InputStream toStream() {
  // return ClasspathLocation.class.getClassLoader().getResourceAsStream(resourcePath);
  // }

  @Override
  public InputStream unsafeInputStream() {
    final InputStream res = specialClassLoader.getResourceAsStream(resourcePath);
    Preconditions.checkNotNull(res);
    return res;
  }

  @Override
  public String readContent(Charset charset) {
    try (BufferedInputStream b = new BufferedInputStream(unsafeInputStream())) {
      return IOUtils.toString(b, charset);
    } catch (IOException e) {
      throw new RuntimeException("Can't read resource [" + resourcePath + "]", e);
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
  public Flux<ClasspathLocation> findFilesAndDirs(boolean recursive) {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
