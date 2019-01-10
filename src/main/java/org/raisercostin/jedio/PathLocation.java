package org.raisercostin.jedio;

import com.google.common.base.Preconditions;
import io.vavr.control.Option;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.raisercostin.util.SimpleShell;

/**
 * What is Absolute, Relative and Canonical Path
 * <li>https://javarevisited.blogspot.com/2014/08/difference-between-getpath-getabsolutepath-getcanonicalpath-java.html
 * <li>https://stackoverflow.com/questions/30920623/difference-between-getcanonicalpath-and-torealpath
 *
 * @author raiser
 */
@Data
public class PathLocation
    implements FolderLocation,
        NonExistingLocation,
        ReferenceLocation,
        ReadableFileLocation,
        WritableFileLocation,
        LinkLocation {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(PathLocation.class);

  private final Path path;

  public PathLocation(Path path) {
    // org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
    this.path = fixPath(path);
  }

  @Override
  public ReferenceLocation child(RelativeLocation child) {
    return Locations.folder(fixPath(path.resolve(child.getLocation())));
  }

  private Path fixPath(Path path) {
    return path.toAbsolutePath().normalize();
  }

  @Override
  public ReferenceLocation child(String path) {
    return FolderLocation.super.child(path);
  }

  public File toFile() {
    return path.toFile();
  }

  public Path toPath() {
    return path;
  }

  @Override
  public FolderLocation mkdir() {
    try {
      FileUtils.forceMkdir(toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  @Override
  /** Not all absolute paths are canonical. Some contain `..` and `.` */
  public String absolute() {
    return path.toAbsolutePath().toString();
  }

  @Override
  /**
   * Returns a path that is this path with redundant name elements eliminated. @see
   * java.nio.file.Path.normalize()
   */
  public String normalized() {
    return toPath().normalize().toString();
  }

  @Override
  /**
   * A canonical pathname is both absolute and unique. The precise definition of canonical form is
   * system-dependent.
   */
  public String canonical() {
    return toPath().normalize().toString();
  }

  @Override
  public String absoluteAndNormalized() {
    return toPath().toAbsolutePath().normalize().toString();
  }

  @Override
  public String real() {
    try {
      return toPath().toRealPath().toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public NonExistingLocation deleteFolder(DeleteOptions options) {
    if (options.deleteByRename) deleteFolderByRename();
    else deleteFolderPermanently();
    return this;
  }

  private void deleteFolderPermanently() {
    try {
      FileUtils.deleteDirectory(toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void deleteFolderByRename() {
    File dest = duplicate(toFile());
    try {
      FileUtils.moveDirectory(toFile(), dest);
      // FileUtils.deleteDirectory(toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public NonExistingLocation deleteFile(DeleteOptions options) {
    if (options.deleteByRename) deleteFileByRename();
    else deleteFilePermanently();
    return this;
  }

  private void deleteFilePermanently() {
    try {
      FileUtils.forceDelete(toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void deleteFileByRename() {
    File dest = duplicate(toFile());
    try {
      FileUtils.moveFile(toFile(), dest);
      // FileUtils.delete(toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public NonExistingLocation delete(DeleteOptions options) {
    if (isFolder()) deleteFolder(options);
    else deleteFile(options);
    return this;
  }

  private File duplicate(File original) {
    File dest;
    int index = 0;
    do {
      dest = new File(original.getAbsolutePath() + "-" + index + ".bak");
      index++;
    } while (dest.exists());
    return dest;
  }

  @Override
  public Option<FolderLocation> existing() {
    if (Files.exists(path)) return Option.of(this);
    else return Option.none();
  }

  @Override
  public Option<NonExistingLocation> nonExisting() {
    if (!Files.exists(path)) return Option.of(this);
    else return Option.none();
  }

  @Override
  public NonExistingLocation nonExistingOrElse(Function<FolderLocation, NonExistingLocation> fn) {
    if (exists()) return fn.apply(this);
    else return this;
  }

  public boolean exists() {
    return Files.exists(path);
  }

  @Override
  public FolderLocation existingOrElse(Function<NonExistingLocation, FolderLocation> fn) {
    if (!exists()) return fn.apply(this);
    else return this;
  }

  @Override
  public WritableFileLocation asWritableFile() {
    // TODO check folder exists and file doesn't
    return this;
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    // TODO check file exists
    return this;
  }

  @Override
  public Option<String> read() {
    try {
      File file = toFile();
      if (file.exists()) return Option.of(FileUtils.readFileToString(file, "UTF-8"));
      else return Option.none();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long length() {
    return toFile().length();
  }

  @Override
  public String getName() {
    return toFile().getName();
  }

  @Override
  public InputStream unsafeInputStream() {
    try {
      return new BufferedInputStream(Files.newInputStream(toPath()), 1024 * 1024 * 10);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public WritableFileLocation write(String content, String charset) {
    makeDirOnParentIfNeeded();
    try {
      FileUtils.write(toFile(), content, charset);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  @Override
  public WritableFileLocation copyFrom(InputStream inputStream) {
    storeFile(inputStream);
    return this;
  }

  private void storeFile(InputStream inputStream) {
    makeDirOnParentIfNeeded();
    try {
      Files.copy(inputStream, toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isFolder() {
    return Files.isDirectory(toPath());
  }

  @Override
  public boolean isFile() {
    return Files.isRegularFile(toPath());
  }

  @Override
  public void rename(FileLocation destLocation) {
    try {
      Path src = toPath();
      Path dest = destLocation.asPathLocation().toPath();
      if (isFolder()) {
        logger.info("rename folder " + src + " to " + destLocation);
        makeDirOnParentIfNeeded();
        Files.move(src, dest);
      } else {
        logger.info("rename file " + src + " to " + destLocation);
        makeDirOnParentIfNeeded();
        Files.move(src, dest);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public PathLocation makeDirOnParentIfNeeded() {
    parent().forEach(x -> x.createDirectories());
    return this;
  }

  private void createDirectories() {
    try {
      FileUtils.forceMkdir(toFile());
      // Files.createDirectories(toPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  // @tailrec
  public Option<ReferenceLocation> findAncestor(Function<ReferenceLocation, Boolean> fn) {
    if (fn.apply(this)) {
      return Option.of(this);
    } else {
      return parent().flatMap(x -> x.findAncestor(fn));
    }
  }

  @Override
  public Option<PathLocation> parent() {
    return Option.of(path.getParent()).map(x -> create(x));
  }

  public PathLocation create(Path x) {
    return new PathLocation(x);
  }

  public Option<RelativeLocation> relativize(FolderLocation ancestor) {
    return stripAncestor(ancestor);
  }

  @Override
  public Option<RelativeLocation> stripAncestor(FolderLocation ancestor) {
    String pathAncestor = ancestor.absoluteAndNormalized();
    String pathChild = absoluteAndNormalized();
    if (pathChild.startsWith(pathAncestor))
      return Option.of(Locations.relative(pathChild.substring(pathAncestor.length())));
    else return Option.none();
  }

  @Override
  public void junctionTo(ReferenceLocation target) {
    createJunction(toPath(), target.asPathLocation().toPath());
  }

  @Override
  public void symlinkTo(ReferenceLocation target) {
    createSymlink(toPath(), target.asPathLocation().toPath());
  }

  private void createJunction(Path symlink, Path target) {
    try {
      if (SystemUtils.IS_OS_WINDOWS) createWindowsJunction(symlink, symlink, target);
      else
        createLinuxSymlink(
            symlink, symlink.toFile().getAbsolutePath(), target.toFile().getAbsolutePath());
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void createWindowsJunction(Path place, Path symlink, Path target) {
    new SimpleShell(place.getParent())
        .execute("cmd /C mklink /J \"" + symlink + "\" \"" + target.toFile().getName() + "\"");
  }

  private void createSymlink(Path symlink, Path target) {
    try {
      if (SystemUtils.IS_OS_WINDOWS)
        createWindowsSymlink(
            symlink, symlink.toFile().getAbsolutePath(), target.toFile().getName());
      else
        createLinuxSymlink(
            symlink, symlink.toFile().getAbsolutePath(), target.toFile().getAbsolutePath());
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void createWindowsSymlink(Path place, String symlink, String targetName)
      throws InterruptedException, IOException {
    new SimpleShell(place.getParent())
        .execute("cmd /C sudo cmd /C mklink /D \"" + symlink + "\" \"" + targetName + "\"");
  }

  private void createLinuxSymlink(Path place, String symlink, String targetPath)
      throws InterruptedException, IOException {
    new SimpleShell(place.getParent()).execute("ln -sr " + targetPath + " " + symlink);
  }

  @Override
  public Option<LinkLocation> asSymlink() {
    if (isSymlink()) return Option.of(this);
    else return Option.none();
  }

  @Override
  public boolean isSymlink() {
    return Files.isSymbolicLink(path) || isJunctionInWindows();
  }

  private boolean isJunctionInWindows() {
    return !Files.isRegularFile(path);
  }

  @Override
  public ReferenceLocation getTarget() {
    Preconditions.checkState(isSymlink());
    try {
      if (isJunctionInWindows()) return create(path.toRealPath(LinkOption.NOFOLLOW_LINKS));
      return create(Files.readSymbolicLink(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String readContent() {
    return read().get();
  }

  @Override
  public PathLocation asPathLocation() {
    return this;
  }
}
