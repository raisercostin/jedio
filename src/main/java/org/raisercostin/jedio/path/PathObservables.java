package org.raisercostin.jedio.path;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.raisercostin.jedio.FileAltered;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Adapted from https://github.com/helmbold/rxfilewatcher (based on RxJava2)
 * 
 * @author raisercostin
 */
public final class PathObservables {

  private PathObservables() {
  }

  /**
   * Creates an observable that watches the given directory and all its subdirectories. Directories that are created
   * after subscription are watched, too.
   * 
   * @param path
   *          Root directory to be watched
   * @return Observable that emits an event for each filesystem event.
   * @throws IOException
   */
  public static Flux<FileAltered> watchRecursive(final Path path) {
    return new FluxFactory(path, true).create();
  }

  /**
   * Creates an observable that watches the given path but not its subdirectories.
   * 
   * @param path
   *          Path to be watched
   * @return Observable that emits an event for each filesystem event.
   */
  public static Flux<FileAltered> watchNonRecursive(final Path path) {
    return new FluxFactory(path, false).create();
  }

  private static class FluxFactory {

    private final Map<WatchKey, Path> directoriesByKey = new HashMap<>();
    private final Path directory;
    private final boolean recursive;

    private FluxFactory(final Path path, final boolean recursive) {
      directory = path.toAbsolutePath();
      this.recursive = recursive;
    }

    private Flux<FileAltered> create() {
      return EmitterProcessor.create(subscriber -> {
        try (WatchService watcher = directory.getFileSystem().newWatchService()) {
          if (recursive) {
            registerAll(directory, watcher);
          } else {
            register(directory, watcher);
          }
          while (!subscriber.isCancelled()) {
            final WatchKey key = watcher.take();
            final Path dir = directoriesByKey.get(key);
            for (final WatchEvent<?> event : key.pollEvents()) {
              subscriber.next(new FileAltered(dir, event));
              registerNewDirectory(subscriber, dir, watcher, event);
            }
            // reset key and remove from set if directory is no longer
            // accessible
            boolean valid = key.reset();
            if (!valid) {
              directoriesByKey.remove(key);
              // nothing to be watched
              if (directoriesByKey.isEmpty()) {
                break;
              }
            }
          }
          subscriber.complete();
        } catch (IOException exception) {
          subscriber.error(exception);
        } catch (InterruptedException exception) {
          if (!subscriber.isCancelled()) {
            subscriber.error(exception);
          }
        }
      });
    }

    /**
     * Register the rootDirectory, and all its sub-directories.
     */
    // TODO use traversal from PathLocation
    private void registerAll(final Path rootDirectory, final WatchService watcher) throws IOException {
      Files.walkFileTree(rootDirectory, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
          register(dir, watcher);
          return FileVisitResult.CONTINUE;
        }
      });
    }

    private void register(final Path dir, final WatchService watcher) throws IOException {
      final WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
      directoriesByKey.put(key, dir);
    }

    // register newly created directory to watching in recursive mode
    private void registerNewDirectory(final FluxSink<FileAltered> subscriber, final Path dir,
        final WatchService watcher, final WatchEvent<?> event) {
      final Kind<?> kind = event.kind();
      if (recursive && kind.equals(ENTRY_CREATE)) {
        // Context for directory entry event is the file name of entry
        @SuppressWarnings("unchecked")
        final WatchEvent<Path> eventWithPath = (WatchEvent<Path>) event;
        final Path name = eventWithPath.context();
        final Path child = dir.resolve(name).toAbsolutePath();
        try {
          if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
            registerAll(child, watcher);
          }
        } catch (final IOException exception) {
          subscriber.error(exception);
        }
      }
    }
  }
}