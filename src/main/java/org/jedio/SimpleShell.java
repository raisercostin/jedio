package org.jedio;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.op.DeleteOptions;

// An instance must be created when is needed as is not thread safe.
@NotThreadSafe
public class SimpleShell implements Shell {
  private static final Pattern SPLIT_PARAMS_PATTERN = Pattern.compile("\"([^\"]*)\"|(\\S+)");
  private Stack<DirLocation> dirs = new Stack<>();
  private DirLocation current;
  private final Map<String, String> env;
  private Pattern sensibleRegex;
  private final DeleteOptions deleteOptions;

  @sugar
  public SimpleShell(String path) {
    this(Locations.path(path).mkdirIfNeeded(), DeleteOptions.deleteByRenameOption());
  }

  @sugar
  public SimpleShell(Path path) {
    this(Locations.path(path).mkdirIfNeeded(), DeleteOptions.deleteByRenameOption());
  }

  @sugar
  public SimpleShell() {
    this(Locations.current(), DeleteOptions.deleteByRenameOption());
  }

  @sugar
  public SimpleShell(DeleteOptions deleteOptions) {
    this(Locations.current(), deleteOptions);
  }

  @sugar
  public SimpleShell(DirLocation path) {
    this(path, DeleteOptions.deleteByRenameOption());
  }

  private SimpleShell(DirLocation path, DeleteOptions deleteOptions) {
    this.deleteOptions = deleteOptions;
    this.env = Maps.newHashMap();
    this.current = path;
  }

  @Override
  public void execute(String command) {
    executeInternal(this.current, split(command)).valid();
  }

  @Override
  public void execute(String command, String... params) {
    executeInternal(this.current, Lists.asList(command, params)).valid();
  }

  @Override
  public ProcessResult executeWithResult(String command) {
    return executeInternal(this.current, split(command));
  }

  // See
  // -
  // https://zeroturnaround.com/rebellabs/why-we-created-yaplj-yet-another-process-library-for-java/
  // -
  // https://stackoverflow.com/questions/193166/good-java-process-control-library
  private ProcessResult executeInternal(DirLocation path, List<String> commandAndParams) {
    try {
      // File input = File.createTempFile("restfs", ".input");
      // TODO splitting command should work for "aaa bbbb" as argument
      ProcessBuilder builder = new ProcessBuilder(commandAndParams).redirectOutput(Redirect.PIPE)
        .redirectError(Redirect.PIPE)
        // .redirectInput(input)
        .directory(path.asPathLocation().toFile());
      // .inheritIO();
      Map<String, String> currentEnvironment = builder.environment();
      currentEnvironment.putAll(this.env);
      Process proc = builder.start();
      return new ProcessResult(this.current, commandAndParams, this.sensibleRegex, proc);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private List<String> split(String command) {
    return splitterOnTokens(SPLIT_PARAMS_PATTERN, command);
  }

  private List<String> splitterOnTokens(Pattern pattern, String command) {
    Matcher m = pattern.matcher(command);
    ArrayList<String> result = new ArrayList<>();
    while (m.find()) {
      if (m.group(1) != null) {
        result.add(m.group(1));
      } else {
        result.add(m.group(2));
      }
    }
    return result;
  }

  @Override
  public DirLocation pwd() {
    return this.current;
  }

  @Override
  public DirLocation cd(RelativeLocation path) {
    return internalCd(child(path).existingRef().get().asDir());
  }

  @Override
  public DirLocation pushd(DirLocation url) {
    this.dirs.push(this.current);
    return internalCd(url);
  }

  @Override
  public DirLocation pushd(RelativeLocation path) {
    this.dirs.push(this.current);
    return internalCd(child(path).existingRef().get().asDir());
  }

  @Override
  public DirLocation popd() {
    return internalCd(this.dirs.pop());
  }

  private DirLocation internalCd(DirLocation dir) {
    this.current = dir;
    dir.mkdirIfNeeded();
    return dir;
  }

  @Override
  public void mkdir(RelativeLocation path) {
    child(path).existingOrElse(NonExistingLocation::mkdir);
  }

  @Override
  public ReferenceLocation child(RelativeLocation path) {
    return pwd().child(path);
  }

  @Override
  @sugar
  public ReferenceLocation child(String path) {
    return child(Locations.relative(path));
  }

  @Override
  @sugar
  public DirLocation pushd(String path) {
    return pushd(Locations.relative(path));
  }

  @Override
  public void mkdir(String path) {
    mkdir(Locations.relative(path));
  }

  @Override
  public String absolute(String path) {
    return child(path).absoluteAndNormalized();
  }

  @Override
  @sugar
  public DirLocation mkdirAndPushd(String path) {
    mkdir(path);
    return pushd(path);
  }

  @Override
  public void deleteIfExists(String path) {
    child(path).nonExistingOrElse(x -> x.delete(this.deleteOptions));
  }

  @Override
  public void addEnv(String name, String value) {
    this.env.put(name, value);
  }

  @Override
  public void blur(String sensibleRegex) {
    this.sensibleRegex = Pattern.compile(sensibleRegex);
  }
}
