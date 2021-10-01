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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jedio.RichThrowable;
import org.jedio.struct.RichIterable;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.ReadableDirLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.impl.ExistingLocationLike;
import org.raisercostin.jedio.impl.ReadableDirLocationLike;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;
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
@Slf4j
public class ClasspathLocation
    implements ReadableDirLocation, ReadableFileLocation, ReadableDirLocationLike<@NonNull ClasspathLocation>,
    ExistingLocationLike<@NonNull ClasspathLocation>, ReadableFileLocationLike<@NonNull ClasspathLocation> {

  public static ClasspathLocation classpath(String path) {
    return new ClasspathLocation(path);
  }

  public static ClasspathLocation classpath(Class<?> clazz, String relative) {
    return new ClasspathLocation(clazz.getPackage().getName().replace('.', '/') + "/" + relative);
  }

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
    this.resourcePath = fixPath(path);
    this.resourceUrl = toUrl(this.resourcePath);
  }

  private ClasspathLocation(String path, URL resourcePath) {
    this.resourcePath = fixPath(path);
    this.resourceUrl = toUrl(this.resourcePath);
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
    URL url = this.resourceUrl;
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

  public PathLocation toPathLocation() {
    return new PathLocation(toPath());
  }

  private Path toPath() {
    return RichThrowable.tryWithSuppressed(() -> {
      URL resource = ClasspathLocation.class.getClassLoader().getResource(this.resourcePath);
      System.out.println("resource=" + resource);
      Preconditions.checkNotNull(resource);
      URI uri = resource.toURI();
      initFileSystem(uri);
      System.out.println("uri=" + uri);
      Preconditions.checkNotNull(uri);
      return Paths.get(uri);
    }, "When trying to read resource [%s]", this.resourcePath);
  }

  private FileSystem initFileSystem(URI uri) throws IOException {
    try {
      return FileSystems.getFileSystem(uri);
    } catch (FileSystemNotFoundException e) {
      log.debug("ignored exception", e);
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
    return Locations.classpath(fixPath(this.resourcePath + "/" + child.relativePath()));
  }
  //
  // private InputStream toStream() {
  // return ClasspathLocation.class.getClassLoader().getResourceAsStream(resourcePath);
  // }

  @Override
  public InputStream unsafeInputStream() {
    final InputStream res = specialClassLoader.getResourceAsStream(this.resourcePath);
    return Preconditions.checkNotNull(res);
  }

  @Override
  public String readContentSync(Charset charset) {
    try (BufferedInputStream b = new BufferedInputStream(unsafeInputStream())) {
      return IOUtils.toString(b, charset);
    } catch (IOException e) {
      throw new RuntimeException("Can't read resource [" + this.resourcePath + "]", e);
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
  public Flux<@NonNull ClasspathLocation> findFilesAndDirs(boolean recursive) {
    //return toPathLocation().findFilesAndDirs(recursive);
    throw new RuntimeException("Not implemented yet!!!");
  }
}
