package org.raisercostin.jedio.path;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import io.vavr.API;
import io.vavr.collection.Iterator;
import io.vavr.control.Option;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.SystemUtils;
import org.jedio.Audit;
import org.jedio.SimpleShell;
import org.jedio.sugar;
import org.raisercostin.jedio.BasicDirLocation;
import org.raisercostin.jedio.ChangeableLocation;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.ExistingLocation;
import org.raisercostin.jedio.FileAltered;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.MetaInfo;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.FindFilters;
import org.raisercostin.jedio.find.GuavaAndDirectoryStreamTraversalWithVirtualDirs;
import org.raisercostin.jedio.find.PathWithAttributes;
import org.raisercostin.jedio.find.TraversalFilter;
import org.raisercostin.jedio.impl.ChangeableLocationLike;
import org.raisercostin.jedio.impl.LinkLocationLike;
import org.raisercostin.jedio.impl.NonExistingLocationLike;
import org.raisercostin.jedio.impl.ReadableDirLocationLike;
import org.raisercostin.jedio.impl.ReadableFileLocationLike;
import org.raisercostin.jedio.impl.ReferenceLocationLike;
import org.raisercostin.jedio.impl.WritableDirLocationLike;
import org.raisercostin.jedio.impl.WritableFileLocationLike;
import org.raisercostin.jedio.op.CopyEvent;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.op.DeleteOptions;
import org.raisercostin.nodes.ExceptionUtils;
import org.raisercostin.nodes.Nodes;
import org.raisercostin.nodes.impl.JsonUtils2;
import reactor.core.publisher.Flux;

/**
 * What is Absolute, Relative and Canonical Path
 * <li>https://javarevisited.blogspot.com/2014/08/difference-between-getpath-getabsolutepath-getcanonicalpath-java.html
 * <li>https://stackoverflow.com/questions/30920623/difference-between-getcanonicalpath-and-torealpath
 *
 * @author raiser
 */
@Data
public class PathLocation implements FileLocation, ChangeableLocation, NonExistingLocation,
    ReadableDirLocationLike<PathLocation>, WritableDirLocationLike<PathLocation>, NonExistingLocationLike<PathLocation>,
    ReadableFileLocationLike<PathLocation>, WritableFileLocationLike<PathLocation>,
    ChangeableLocationLike<PathLocation>, LinkLocationLike<PathLocation> {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PathLocation.class);

  public static PathLocation efficientExistingDir(Path path) {
    return new PathLocation(path, false);
  }

  public static PathLocation efficientExistingFile(Path path) {
    return new PathLocation(path, false);
  }

  protected static Path fixPath(Path path) {
    return path.toAbsolutePath().normalize();
  }

  private final Path path;

  public PathLocation(Path path) {
    this(path, true);
  }

  public PathLocation(Path path, boolean normalize) {
    // org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
    this.path = normalize ? fixPath(path) : path;
  }

  @Override
  public PathLocation child(RelativeLocation child) {
    return Locations.path(fixPath(this.path.resolve(child.relativePath())));
  }

  @Override
  public PathLocation child(String path) {
    return ChangeableLocationLike.super.child(path);
  }

  public File toFile() {
    return this.path.toFile();
  }

  public Path toPath() {
    return this.path;
  }

  @Override
  public PathLocation mkdir() {
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
    return this.path.toAbsolutePath().toString();
  }

  @Override
  /**
   * Returns a path that is this path with redundant name elements eliminated. @see java.nio.file.Path.normalize()
   */
  public String normalized() {
    return toPath().normalize().toString();
  }

  @Override
  /**
   * A canonical pathname is both absolute and unique. The precise definition of canonical form is system-dependent.
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
  public NonExistingLocation deleteDir(DeleteOptions options) {
    if (options.deleteByRename()) {
      deleteDirByRename();
    } else {
      deleteDirPermanently();
    }
    return this;
  }

  private void deleteDirPermanently() {
    try {
      FileUtils.deleteDirectory(toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void deleteDirByRename() {
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
    if (options.deleteByRename()) {
      deleteFileByRename();
    } else {
      deleteFilePermanently();
    }
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
    if (isDir()) {
      deleteDir(options);
    } else {
      deleteFile(options);
    }
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
  public Option<PathLocation> existing() {
    if (Files.exists(this.path)) {
      return Option.of(this);
    } else {
      return Option.none();
    }
  }

  @Override
  public Option<NonExistingLocationLike<?>> nonExisting() {
    if (!Files.exists(this.path)) {
      return Option.of(this);
    } else {
      return Option.none();
    }
  }

  @Override
  public NonExistingLocation nonExistingOrElse(Function<DirLocation, NonExistingLocation> fn) {
    if (exists()) {
      return fn.apply(this);
    } else {
      return this;
    }
  }

  @Override
  public boolean exists() {
    return Files.exists(this.path);
  }

  @Override
  public PathLocation existingOrElse(Function<NonExistingLocation, DirLocation> fn) {
    if (!exists()) {
      return (PathLocation) fn.apply(this);
    } else {
      return this;
    }
  }

  @Override
  public WritableFileLocation asWritableFile() {
    // TODO check dir exists and file doesn't
    return this;
  }

  @Override
  public ReadableFileLocationLike<?> asReadableFile() {
    return this;
  }

  @Override
  public String readContentSync(Charset charset) {
    try (BufferedReader b = reader(toPath(), charset)) {
      return IOUtils.toString(b);
    } catch (IOException e) {
      throw ExceptionUtils.wrap(e, "While reading %s with charset %s. Others could exist %s", this, charset,
        Charset.availableCharsets().keySet());
    }
  }

  @SneakyThrows
  private BufferedReader reader(Path path, Charset charset) {
    CharsetDecoder decoder = charset.newDecoder();
    Reader reader = new InputStreamReader(new BOMInputStream(Files.newInputStream(path), ByteOrderMark.UTF_8,
      ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE), decoder);
    return new BufferedReader(reader);
  }

  @Override
  public Option<String> readIfExists() {
    try {
      File file = toFile();
      if (file.exists()) {
        return Option.of(FileUtils.readFileToString(file, "UTF-8"));
      } else {
        return Option.none();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long length() {
    try {
      return Files.size(this.path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
  public PathLocation write(String content, String charset) {
    mkdirOnParentIfNeeded();
    try {
      FileUtils.write(toFile(), content, charset);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  @Override
  public PathLocation copyFrom(ReadableFileLocation source, CopyOptions copyOptions) {
    copyOptions.reportOperationEvent(CopyEvent.CopyFileTriggered, source, this);
    if (copyOptions.makeDirIfNeeded()) {
      mkdirOnParentIfNeeded();
    }
    PathLocation metaHttp = this.meta("http", "json");
    try {
      boolean sourceExists = source.exists();
      boolean metaShouldBeCreated = copyOptions.copyMeta() && !metaHttp.exists();
      boolean destAndHttpMetaExists = metaHttp.exists() && exists() && !metaShouldBeCreated;
      if (metaShouldBeCreated && metaHttp.exists()) {
        reportOperationEvent(copyOptions, true, CopyEvent.CopyIgnoreDestinationMetaExists, source, metaHttp);
      } else if (destAndHttpMetaExists && !copyOptions.replaceExisting()) {
        reportOperationEvent(copyOptions, true, CopyEvent.CopyIgnoreDestinationExists, source, this);
      } else {
        // source is analysed for existance only if will actually try to copy
        if (!sourceExists) {
          reportOperationEvent(copyOptions, true, CopyEvent.CopyIgnoreSourceDoesNotExists, source, this);
        } else {
          reportOperationEvent(copyOptions, false, CopyEvent.CopyFileStarted, source, this);
          source.usingInputStreamAndMeta(true, streamAndMeta -> {
            if (streamAndMeta.meta.isSuccess) {
              PathLocation actualDest = copyOptions.amend(this, streamAndMeta);
              if (copyOptions.acceptStreamAndMeta(streamAndMeta)) {
                if (copyOptions.replaceExisting()) {
                  reportOperationEvent(copyOptions, false, CopyEvent.CopyReplacing, source, this);
                  Files.copy(streamAndMeta.is, actualDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                  Files.copy(streamAndMeta.is, actualDest.toPath());
                }
              } else {
                reportOperationEvent(copyOptions, true, CopyEvent.CopyIgnoreContentType, source, this, metaHttp);
              }
              // write meta
              if (copyOptions.copyMeta()) {
                JsonUtils2 mapper = Nodes.json;
                // mapper.mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);
                String str = mapper.excluding("parent", "otherParents", "impl").toString(streamAndMeta.meta);
                metaHttp.write(str);
                reportOperationEvent(copyOptions, false, CopyEvent.CopyMeta, source, this, metaHttp);
              }
              return this;
            } else {
              metaHttp.write(Nodes.json.toString(streamAndMeta.meta));
              copyOptions.reportOperationEvent(CopyEvent.CopyFailed, streamAndMeta.meta.error, source, this);
              return this;
            }
          });
          reportOperationEvent(copyOptions, false, CopyEvent.CopyFileFinished, source, this);
        }
      }
    } catch (Throwable e) {
      metaHttp.write(Nodes.json.toString(MetaInfo.error(e)));
      copyOptions.reportOperationEvent(CopyEvent.CopyFailed, e, source, this);
      if (copyOptions.throwOnError()) {
        throw e;
      }
    }
    return this;
  }

  private void reportOperationEvent(CopyOptions copyOptions, boolean operationIgnored, CopyEvent event,
      ExistingLocation src, ReferenceLocation dst, Object... args) {
    copyOptions.reportOperationEvent(event, src, dst, args);
    if (operationIgnored && copyOptions.throwOnError()) {
      throw new Audit.AuditException("CopyOperation result: %s (src=%s, dst=%s) %s", event, src, dst, API.List(args));
    }
  }

  @Override
  public boolean isDir() {
    return Files.isDirectory(toPath());
  }

  @Override
  public boolean isFile() {
    return Files.isRegularFile(toPath());
  }

  @Override
  public void rename(WritableFileLocation destLocation) {
    try {
      Path src = toPath();
      Path dest = destLocation.asPathLocation().toPath();
      if (isDir()) {
        logger.info("rename dir " + src + " to " + destLocation);
        mkdirOnParentIfNeeded();
        Files.move(src, dest);
      } else {
        logger.info("rename file " + src + " to " + destLocation);
        mkdirOnParentIfNeeded();
        Files.move(src, dest);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public PathLocation mkdirOnParentIfNeeded() {
    parent().forEach(x -> x.createDirectories());
    return this;
  }

  void createDirectories() {
    try {
      FileUtils.forceMkdir(toFile());
      // Files.createDirectories(toPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  // @tailrec
  public Option<PathLocation> findAncestor(Function<ReferenceLocationLike<?>, Boolean> fn) {
    if (fn.apply(this)) {
      return Option.of(this);
    } else {
      return parent().flatMap(x -> x.findAncestor(fn));
    }
  }

  @Override
  public Option<PathLocation> parent() {
    return Option.of(this.path.getParent()).map(x -> create(x));
  }

  @Override
  public Option<RelativeLocation> stripAncestor(BasicDirLocation ancestor) {
    String pathAncestor = ancestor.absoluteAndNormalized();
    String pathChild = absoluteAndNormalized();
    if (pathChild.startsWith(pathAncestor)) {
      return Option.of(Locations.relative(pathChild.substring(pathAncestor.length())));
    } else {
      return Option.none();
    }
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
    if (SystemUtils.IS_OS_WINDOWS) {
      createWindowsJunction(symlink, symlink, target);
    } else {
      createLinuxSymlink(symlink, symlink.toFile().getAbsolutePath(), target.toFile().getAbsolutePath());
    }
  }

  private void createWindowsJunction(Path place, Path symlink, Path target) {
    new SimpleShell(place.getParent())
      .execute("cmd /C mklink /J \"" + symlink + "\" \"" + target.toFile().getName() + "\"");
  }

  private void createSymlink(Path symlink, Path target) {
    if (SystemUtils.IS_OS_WINDOWS) {
      createWindowsSymlink(symlink, symlink.toFile().getAbsolutePath(), target.toFile().getName());
    } else {
      createLinuxSymlink(symlink, symlink.toFile().getAbsolutePath(), target.toFile().getAbsolutePath());
    }
  }

  private void createWindowsSymlink(Path place, String symlink, String targetName) {
    new SimpleShell(place.getParent())
      .execute("cmd /C sudo cmd /C mklink /D \"" + symlink + "\" \"" + targetName + "\"");
  }

  private void createLinuxSymlink(Path place, String symlink, String targetPath) {
    new SimpleShell(place.getParent()).execute("ln -sr " + targetPath + " " + symlink);
  }

  @Override
  public Option<LinkLocationLike> asSymlink() {
    if (isSymlink()) {
      return Option.of(this);
    } else {
      return Option.none();
    }
  }

  @Override
  public boolean isSymlink() {
    return Files.isSymbolicLink(this.path) || isJunctionInWindows();
  }

  private boolean isJunctionInWindows() {
    return !Files.isRegularFile(this.path);
  }

  @Override
  public PathLocation getTarget() {
    Preconditions.checkState(isSymlink());
    try {
      if (isJunctionInWindows()) {
        return create(this.path.toRealPath(LinkOption.NOFOLLOW_LINKS));
      }
      return create(Files.readSymbolicLink(this.path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public PathLocation asPathLocation() {
    return this;
  }

  @Override
  public ChangeableLocationLike<?> asChangableLocation() {
    return this;
  }

  @Override
  public Flux<FileAltered> watch() {
    /*
     * Implementation inspired from - http://blog2.vorburger.ch/2015/04/java-7-watchservice-based.html -
     * https://docs.oracle.com/javase/tutorial/essential/io/notification.html -
     * http://commons.apache.org/proper/commons-io/javadocs/api-release/index.
     * html?org/apache/commons/io/monitor/package -summary.html
     */
    logger.info("watch " + this);
    // WatchService watcher = FileSystems.getDefault().newWatchService();
    // val key = path.register(watcher, ENTRY_CREATE, ENTRY_DELETE,
    // ENTRY_MODIFY, OVERFLOW);
    // //key.pollEvents();
    // watcher.poll()
    // Flux.in
    // new DirectoryWatcherBuilder().dir(dir)
    // .listener((directory, changeKind) ->
    // System.out.println(changeKind.toString() + " " + directory.toString()));
    // val observer = new FileAlterationObserver(toFile());
    // val monitor = new FileAlterationMonitor(pollingIntervalInMillis);
    return PathObservables.watchNonRecursive(this.path);
    // .map(x->{System.out.println(x.kind().type()+" "+x.kind().name()+"
    // "+x.context()+" "+x.count()+"
    // "+((Path)x.context()).toAbsolutePath());return x;})
    // .map(x->new FileAltered());
    // throw new RuntimeException("Not implemented yet!!!");
  }
  // @Override
  // public Flux<FileAltered> watch2() {
  // val observer = new FileAlterationObserver(toFile());
  //
  // return Flux.interval(Duration.ofSeconds(1)).map{x->{
  // });
  // }
  // }
  // def watch(pollingIntervalInMillis: Long = 1000): Observable[FileAltered] =
  // {
  // Observable.apply { obs =>
  // val observer = new FileAlterationObserver(toFile);
  // val monitor = new FileAlterationMonitor(pollingIntervalInMillis);
  // val fileListener = new FileAlterationListenerAdaptor() {
  // // override def onFileCreate(file: File) = {
  // // val location = Locations.file(file)
  // // try {
  // // obs.onNext(FileCreated(file))
  // // } catch {
  // // case NonFatal(e) =>
  // // obs.onError(new RuntimeException(s"Processing of
  // [${Locations.file(file)}] failed.", e))
  // // }
  // // }
  // /**File system observer started checking event.*/
  // //override def onStart(file:FileAlterationObserver) =
  // obs.onNext(FileChanged(file))
  // override def onDirectoryCreate(file: File) =
  // obs.onNext(DirectoryCreated(file))
  // override def onDirectoryChange(file: File) =
  // obs.onNext(DirectoryChanged(file))
  // override def onDirectoryDelete(file: File) =
  // obs.onNext(DirectoryDeleted(file))
  // override def onFileCreate(file: File) = obs.onNext(FileCreated(file))
  // override def onFileChange(file: File) = obs.onNext(FileChanged(file))
  // override def onFileDelete(file: File) = obs.onNext(FileDeleted(file))
  // /**File system observer finished checking event.*/
  // //override def onStop(file:FileAlterationObserver) =
  // obs.onNext(FileChanged(file))
  // }
  // observer.addListener(fileListener)
  // monitor.addObserver(observer)
  // monitor.start()
  // Subscription { monitor.stop() }
  // }
  // }

  // /**
  // * Register the rootDirectory, and all its sub-directories.
  // */
  // private void registerAll(final Path rootDirectory, final WatchService
  // watcher) throws IOException {
  // Files.walkFileTree(rootDirectory, new SimpleFileVisitor<Path>() {
  // @Override
  // public FileVisitResult preVisitDirectory(final Path dir, final
  // BasicFileAttributes attrs) throws IOException {
  // register(dir, watcher);
  // return FileVisitResult.CONTINUE;
  // }
  // });
  // }
  //
  // private void register(final Path dir, final WatchService watcher) throws
  // IOException {
  // final WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE,
  // ENTRY_MODIFY);
  // directoriesByKey.put(key, dir);
  // }
  /// **
  // * Copyright (C) 2015 by Michael Vorburger
  // */
  // package ch.vorburger.hotea.watchdir;
  //
  // import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
  // import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
  // import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
  //
  // import java.io.IOException;
  // import java.nio.file.ClosedWatchServiceException;
  // import java.nio.file.FileSystems;
  // import java.nio.file.FileVisitResult;
  // import java.nio.file.Files;
  // import java.nio.file.Path;
  // import java.nio.file.SimpleFileVisitor;
  // import java.nio.file.StandardWatchEventKinds;
  // import java.nio.file.WatchEvent;
  // import java.nio.file.WatchEvent.Kind;
  // import java.nio.file.WatchKey;
  // import java.nio.file.WatchService;
  // import java.nio.file.attribute.BasicFileAttributes;
  //
  // import org.slf4j.Logger;
  // import org.slf4j.LoggerFactory;
  //
  /// **
  // * DirectoryWatcher based on java.nio.file.WatchService.
  // *
  // * @author Michael Vorburger
  // */
  //// intentionally package local, for now
  // public class DirectoryWatcherImpl implements DirectoryWatcher {
  // private final static Logger log =
  /// LoggerFactory.getLogger(DirectoryWatcherImpl.class);
  //
  // protected final WatchService watcher =
  /// FileSystems.getDefault().newWatchService(); // better final, as it will be
  /// accessed by both threads (normally OK either way, but still)
  // protected final Thread thread;
  //
  // /** Clients should use DirectoryWatcherBuilder */
  // protected DirectoryWatcherImpl(boolean watchSubDirectories, final Path
  /// watchBasePath, final Listener listener, ExceptionHandler exceptionHandler)
  /// throws IOException {
  // if (!watchBasePath.toFile().isDirectory())
  // throw new IllegalArgumentException("Not a directory: " +
  /// watchBasePath.toString());
  //
  // register(watchSubDirectories, watchBasePath);
  // Runnable r = () -> {
  // for (;;) {
  // WatchKey key;
  // try {
  // key = watcher.take();
  // } catch (ClosedWatchServiceException e) {
  // log.debug("WatchService take() interrupted by ClosedWatchServiceException,
  /// terminating Thread (as planned).");
  // return;
  // } catch (InterruptedException e) {
  // log.debug("Thread InterruptedException, terminating (as planned, if caused
  // by
  /// close()).");
  // return;
  // }
  // Path watchKeyWatchablePath = (Path) key.watchable();
  // // We have a polled event, now we traverse it and receive all the states
  // from
  /// it
  // for (WatchEvent<?> event : key.pollEvents()) {
  //
  // Kind<?> kind = event.kind();
  // if (kind == StandardWatchEventKinds.OVERFLOW) {
  // // TODO Not sure how to correctly "handle" an Overflow.. ?
  // log.error("Received {} (TODO how to handle?)", kind.name());
  // continue;
  // }
  //
  // Path relativePath = (Path) event.context();
  // if (relativePath == null) {
  // log.error("Received {} but event.context() == null: {}", kind.name(),
  /// event.toString());
  // continue;
  // }
  // Path absolutePath = watchKeyWatchablePath.resolve(relativePath);
  // if (log.isTraceEnabled())
  // log.trace("Received {} for: {}", kind.name(), absolutePath);
  //
  // if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
  // if (Files.isDirectory(absolutePath)) { // don't NOFOLLOW_LINKS
  // try {
  // register(watchSubDirectories, watchBasePath);
  // } catch (IOException e) {
  // exceptionHandler.onException(e);
  // }
  // }
  // }
  //
  // if (kind == StandardWatchEventKinds.ENTRY_MODIFY || kind ==
  /// StandardWatchEventKinds.ENTRY_DELETE) {
  // // To reduce notifications, only call the Listener on Modify and Delete but
  /// not Create,
  // // because (on Linux at least..) every ENTRY_CREATE from new file
  // // is followed by an ENTRY_MODIFY anyway.
  // try {
  // ChangeKind ourKind = kind == StandardWatchEventKinds.ENTRY_MODIFY ?
  /// ChangeKind.MODIFIED : ChangeKind.DELETED;
  // listener.onChange(absolutePath, ourKind);
  // } catch (Throwable e) {
  // exceptionHandler.onException(e);
  // }
  // }
  // }
  // key.reset();
  // }
  // };

  static GuavaAndDirectoryStreamTraversalWithVirtualDirs traversal = new GuavaAndDirectoryStreamTraversalWithVirtualDirs(
    true, x -> false);

  private TraversalFilter createFilter(boolean recursive) {
    return FindFilters.createFindFilter("", "", false, recursive);
  }

  @Override
  public Flux<PathLocation> findFilesAndDirs(boolean recursive) {
    return findFilesAndDirs(createFilter(recursive));
  }

  public Flux<PathLocation> findFilesAndDirs(TraversalFilter filter) {
    return find(filter).map(x -> {
      if (x.isDirectory()) {
        return PathLocation.efficientExistingDir(x.path);
      } else {
        return PathLocation.efficientExistingFile(x.path);
      }
    });
  }

  @Override
  public Flux<PathLocation> findFilesAsFlux(boolean recursive) {
    return find(createFilter(recursive)).flatMap(x -> {
      if (x.isDirectory()) {
        return Flux.empty();
      } else {
        return Flux.just(PathLocation.efficientExistingFile(x.path));
      }
    });
  }

  @Override
  public Flux<DirLocation> findDirs(boolean recursive) {
    return find(createFilter(recursive)).flatMap(x -> {
      if (!x.isDirectory()) {
        return Flux.empty();
      } else {
        return Flux.just(PathLocation.efficientExistingDir(x.path));
      }
    });
  }

  @Override
  public Flux<PathWithAttributes> find(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore,
      boolean dirsFirstInRecursive) {
    return find(traversal, FindFilters.createFindFilter(filter, gitIgnore, dirsFirstInRecursive, recursive));
  }

  private Flux<PathWithAttributes> find(final TraversalFilter filter) {
    return find(traversal, filter);
  }

  private Flux<PathWithAttributes> find(FileTraversal2 traversal, final TraversalFilter filter) {
    logger.info("traverse " + this);
    return traversal.traverse2(toPath(), filter);
  }

  public PathLocation create(Path x) {
    return new PathLocation(x);
  }

  @Override
  public PathLocation create(String path) {
    return create(Paths.get(path));
  }

  @Override
  public PathLocation asDir() {
    return this;
  }

  public PathLocation childDir(String path) {
    return child(path).mkdirIfNeeded();
  }

  @Override
  @sugar
  public Iterator<PathLocation> ls(boolean recursive) {
    return Iterator.ofAll(findFilesAndDirs(recursive).toIterable());
  }

  @Override
  public URI toUri() {
    return this.path.toUri();
  }
}
