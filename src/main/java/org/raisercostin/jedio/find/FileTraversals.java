package org.raisercostin.jedio.find;

import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.directoryFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import com.google.common.graph.Traverser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import reactor.core.publisher.Flux;

public class FileTraversals {
  public static FileTraversal traverseUsingWalk() {
    return new WalkTraversal();
  }

  public static FileTraversal traverseUsingGuava() {
    return new GuavaTraversal();
  }

  public static FileTraversal traverseUsingGuavaAndDirectoryStream() {
    return new GuavaAndDirectoryStreamTraversal();
  }

  public static FileTraversal traverseUsingCommonsIo(String gitIgnores) {
    return new CommonsIoTraversal(gitIgnores);
  }

  public interface SimpleFileTraversal extends FileTraversal {
    @Override
    Flux<Path> traverse(Path start, boolean ignoreCase);

    @Override
    default Flux<Path> traverse(Path start, String regex, boolean ignoreCase) {
      PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + regex);
      return traverse(start, ignoreCase).filter(path -> matcher.matches(path));
    }
  }

  public static class WalkTraversal implements SimpleFileTraversal {
    @Override
    public Flux<Path> traverse(Path start, boolean ignoreCase) {
      try {
        return Flux.fromStream(Files.walk(start));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static class CommonsIoTraversal implements FileTraversal {
    private final IOFileFilter gitFilterCaseSensible;
    private final IOFileFilter gitFilterCaseInSensible;

    public CommonsIoTraversal(String gitIgnores) {
      this.gitFilterCaseSensible = notFileFilter(and(directoryFileFilter(), createFilter(gitIgnores, true)));
      this.gitFilterCaseInSensible = notFileFilter(and(directoryFileFilter(), createFilter(gitIgnores, false)));
    }

    private OrFileFilter createFilter(String gitIgnores, boolean ignoreCase) {
      Stream<IOFileFilter> or = Streams.stream(Splitter.on("\n").omitEmptyStrings().trimResults().split(gitIgnores))
        .filter(line -> !line.startsWith("#"))
        .map(dir -> nameFileFilter(dir, ignoreCase ? IOCase.INSENSITIVE : IOCase.SENSITIVE));
      List<IOFileFilter> all = or.collect(Collectors.toList());
      return new OrFileFilter(all);
    }

    @Override
    public Flux<Path> traverse(Path start, String regex, boolean ignoreCase) {
      Iterable<File> a = () -> FileUtils.iterateFilesAndDirs(start.toFile(), TrueFileFilter.INSTANCE,
        getFilter(ignoreCase));
      // lesAndDirs(start.toFile(), null, null);
      return Flux.fromStream(StreamSupport.stream(a.spliterator(), false).map(File::toPath));
    }

    private IOFileFilter getFilter(boolean ignoreCase) {
      if (ignoreCase) {
        return this.gitFilterCaseInSensible;
      } else {
        return this.gitFilterCaseSensible;
      }
    }
  }

  /**
   * Sample gitIgnores:
   *
   * <pre>
   *     # for now only dirs
   *     target
   *     .git
   *     .mvn
   * </pre>
   */
  protected static OrFileFilter createGitFilter(String gitIgnores, boolean ignoreCase) {
    Stream<IOFileFilter> or = Streams.stream(Splitter.on("\n").omitEmptyStrings().trimResults().split(gitIgnores))
      .filter(line -> !line.startsWith("#"))
      .map(dir -> nameFileFilter(dir, ignoreCase ? IOCase.INSENSITIVE : IOCase.SENSITIVE));
    List<IOFileFilter> all = or.collect(Collectors.toList());
    return new OrFileFilter(all);
  }

  public static class FileFilterPathMatcherAdapter implements PathMatcher {
    private final FileFilter filter;

    public FileFilterPathMatcherAdapter(FileFilter filter) {
      this.filter = filter;
    }

    @Override
    public boolean matches(Path path) {
      if (path.getClass().getName().equals("com.sun.nio.zipfs.ZipPath")) {
        return true;
      }
      return this.filter.accept(path.toFile());
    }
  }

  // public static class WalkFileTreeTraversal implements FileTraversal {
  // public Stream<Path> traverse(Path start) {
  // try {
  // Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
  // });
  // //return stream;
  // } catch (IOException e) {
  // throw new RuntimeException(e);
  // }
  // }
  // }
  //
  public static class GuavaTraversal implements SimpleFileTraversal {
    @Override
    public Flux<Path> traverse(Path start, boolean ignoreCase) {
      Iterable<File> iterable = com.google.common.io.Files.fileTraverser().depthFirstPreOrder(start.toFile());
      Stream<File> stream = StreamSupport.stream(iterable.spliterator(), false);
      return Flux.fromStream(stream.map(File::toPath));
    }
  }

  public static class GuavaAndDirectoryStreamTraversal implements FileTraversal {
    @Override
    public Flux<Path> traverse(Path start, String regex, boolean ignoreCase) {
      try {
        Iterable<Path> iterable = fileTraverser(createFilter(start, regex)).depthFirstPreOrder(start);
        return Flux.fromIterable(iterable);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private static Traverser<Path> fileTraverser(Optional<Filter<Path>> filter) {
      return Traverser.forTree(file -> fileTreeChildren(file, filter));
    }

    private static LinkOption[] options = { LinkOption.NOFOLLOW_LINKS };

    private static Iterable<Path> fileTreeChildren(Path file, Optional<Filter<Path>> filter) {
      // check isDirectory() just because it may be faster than listFiles() on a
      // non-directory
      if (Files.isDirectory(file, options)) {
        try {
          DirectoryStream<Path> files = null;
          if (filter.isPresent()) {
            files = Files.newDirectoryStream(file, filter.get());
          } else {
            files = Files.newDirectoryStream(file);
          }
          if (files != null) {
            return files;
            // return Collections.unmodifiableList(Arrays.asList(files));
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      return Collections.emptyList();
    }
  }

  // copied from newDirectoryStream(file,regex)
  public static Optional<Filter<Path>> createFilter(Path dir, String glob) throws IOException {
    // avoid creating a matcher if all entries are required.
    if (glob.equals("*")) {
      return Optional.empty();
    }
    // create a matcher and return a filter that uses it.
    FileSystem fs = dir.getFileSystem();
    final PathMatcher matcher = fs.getPathMatcher("glob:" + glob);
    DirectoryStream.Filter<Path> filter = path -> matcher.matches(path);
    return Optional.of(filter);
  }
}
