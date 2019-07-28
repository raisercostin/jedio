package org.raisercostin.util;

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

import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.op.DeleteOptions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

// An instance must be created when is needed as is not thread safe.
@NotThreadSafe
public class SimpleShell implements Shell {
  private static final Pattern SPLIT_PARAMS_PATTERN = Pattern.compile("\"([^\"]*)\"|(\\S+)");
  private Stack<DirLocation> dirs = new Stack<DirLocation>();
  private DirLocation current;
  private final Map<String, String> env;
  private Pattern sensibleRegex;
  private final DeleteOptions deleteOptions;

  @sugar
  public SimpleShell(String path) {
    this(Locations.existingDir(path), DeleteOptions.deleteByRenameOption());
  }

  @sugar
  public SimpleShell(Path path) {
    this(Locations.existingDir(path), DeleteOptions.deleteByRenameOption());
  }

  @sugar
  public SimpleShell() {
    this(Locations.existingDir("."), DeleteOptions.deleteByRenameOption());
  }

  @sugar
  public SimpleShell(DeleteOptions deleteOptions) {
    this(Locations.existingDir("."), deleteOptions);
  }

  @sugar
  public SimpleShell(DirLocation path) {
    this(path, DeleteOptions.deleteByRenameOption());
  }

  private SimpleShell(DirLocation path, DeleteOptions deleteOptions) {
    this.deleteOptions = deleteOptions;
    env = Maps.newHashMap();
    current = path;
  }

  public void execute(String command) {
    executeInternal(current, split(command)).valid();
  }

  @Override
  public void execute(String command, String... params) {
    executeInternal(current, Lists.asList(command, params)).valid();
  }

  @Override
  public ProcessResult executeWithResult(String command) {
    return executeInternal(current, split(command));
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
      currentEnvironment.putAll(env);
      Process proc = builder.start();
      return new ProcessResult(current, commandAndParams, sensibleRegex, proc);
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

  public DirLocation pwd() {
    return current;
  }

  public DirLocation cd(RelativeLocation path) {
    return internalCd(child(path).existing().get());
  }

  public DirLocation pushd(DirLocation url) {
    dirs.push(current);
    return internalCd(url);
  }

  public DirLocation pushd(RelativeLocation path) {
    dirs.push(current);
    return internalCd(child(path).existing().get());
  }

  public DirLocation popd() {
    return internalCd(dirs.pop());
  }

  private DirLocation internalCd(DirLocation dir) {
    current = dir;
    dir.mkdirIfNecessary();
    return dir;
  }

  public void mkdir(RelativeLocation path) {
    child(path).existingOrElse(NonExistingLocation::mkdir);
  }

  public ReferenceLocation child(RelativeLocation path) {
    return pwd().child(path);
  }

  @sugar
  public ReferenceLocation child(String path) {
    return child(Locations.relative(path));
  }

  @sugar
  public DirLocation pushd(String path) {
    return pushd(Locations.relative(path));
  }

  public void mkdir(String path) {
    mkdir(Locations.relative(path));
  }

  public String absolute(String path) {
    return child(path).absoluteAndNormalized();
  }

  @sugar
  public DirLocation mkdirAndPushd(String path) {
    mkdir(path);
    return pushd(path);
  }

  public void deleteIfExists(String path) {
    child(path).nonExistingOrElse(x -> x.delete(deleteOptions));
  }

  @Override
  public void addEnv(String name, String value) {
    env.put(name, value);
  }

  @Override
  public void blur(String sensibleRegex) {
    this.sensibleRegex = Pattern.compile(sensibleRegex);
  }
}
