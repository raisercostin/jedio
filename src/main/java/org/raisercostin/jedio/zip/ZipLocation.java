package org.raisercostin.jedio.zip;

import static io.vavr.API.unchecked;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import io.vavr.Lazy;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.impl.ReadableDirLocationLike;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;
import org.raisercostin.jedio.op.DeleteOptions;
import org.raisercostin.jedio.path.PathLocation;
import reactor.core.publisher.Flux;

public class ZipLocation
    implements ReadableDirLocationLike<@NonNull ZipLocation>, ReadableFileLocationLike<@NonNull ZipLocation> {
  //public static class ZipInputLocationImpl(zip: ReadableFileLocation, entry: Option<java.util.zip.ZipEntry>) extends ZipInputLocation {
  //override def toString = s"ZipInputLocation($zip,$entry)"
  //  override protected lazy val rootzip = new java.util.zip.ZipFile(Try { toFile }.getOrElse(Locations.temp.randomChild(name).copyFrom(zip).toFile))
  //}

  private ReadableFileLocation path;
  private Lazy<ZipFile> rootzip = Lazy.of(unchecked(() -> new java.util.zip.ZipFile(path.asPathLocation().toFile())));
  private ZipLocation root;
  private ZipEntry entry;
  private Lazy<byte[]> entryContent = Lazy.of(() -> readBytes());

  public ZipLocation(ReadableFileLocation path) {
    this.path = path;
  }

  public ZipLocation(ZipLocation root, ZipEntry entry) {
    this.path = root.path;
    this.root = root;
    this.entry = entry;
  }

  private ZipLocation zipChild(ZipEntry entry) {
    return new ZipLocation(this, entry);
  }

  @Override
  public ZipLocation child(String name) {
    return entry == null ? new ZipLocation(this, new ZipEntry(name))
        : new ZipLocation(this, rootzip.get().getEntry(entry.getName() + name));
  }

  public PathLocation unzip() {
    throw new RuntimeException("Not implemented yet!!!");
  }
  //override def list: Iterable[self.type] = Option(existing).map(_ => entries).getOrElse(Iterable()).map(entry => toRepr(childFromEntry(entry))).asInstanceOf[Iterable[self.type]]

  @Override
  public String toExternalUri() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    return this;
  }

  @Override
  public Flux<@NonNull ZipLocation> findFilesAndDirs(boolean recursive) {
    //.getOrElse(Locations.temp.randomChild(name).copyFrom(zip).toFile))
    return Flux.fromIterable(() -> Iterators.forEnumeration(rootzip.get().entries()))
      .map(x -> zipChild(x));
  }

  @Override
  public String toString() {
    return "ZipLocation(%s ! %s)".formatted(zipRoot().path.absoluteAndNormalized(), entry);
  }

  private ZipLocation zipRoot() {
    return this.root != null && this.root.root != null ? this.root.zipRoot() : this.root;
  }

  @Override
  public String absoluteAndNormalized() {
    return "%s!%s".formatted(zipRoot().path.absoluteAndNormalized(), entry);
  }

  public ZipEntry entry() {
    return entry;
  }

  @Override
  @SneakyThrows
  public boolean exists() {
    return path.exists() && (entry == null || rootzip.get().getInputStream(entry) != null);
  }

  @Override
  @SneakyThrows
  public InputStream unsafeInputStream() {
    if (entry != null) {
      InputStream inputStream = rootzip.get().getInputStream(entry);
      Preconditions.checkNotNull(inputStream, "InputStream for %s should be not null", this);
      return inputStream;
    } else {
      throw new RuntimeException("Can't read stream from " + this);
    }
  }

  @Override
  public NonExistingLocation deleteFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation deleteFile(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public void rename(WritableFileLocation writableFileLocation) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public long length() {
    long length = entry.getSize();
    //    try {
    if (length == -1) {
      return entryContent.get().length;
    }
    //        FileSystem zipFs = FileSystems.newFileSystem(path.asPathLocation().toPath());
    //        long length2 = java.nio.file.Files.size(zipFs.getPath("!" + entry.getName()));
    //        return length2;
    //      }
    //    } catch (IOException e) {
    //      throw org.jedio.RichThrowable.nowrap(e);
    //    }
    return length;
  }

  public long lengthIfPossible() {
    return entry.getSize();
  }

  @SneakyThrows
  private byte[] readBytes() {
    try (InputStream stream = unsafeInputStream()) {
      return IOUtils.toByteArray(stream);
    }
  }
}
