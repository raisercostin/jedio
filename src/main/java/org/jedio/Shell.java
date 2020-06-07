package org.jedio;

import java.nio.file.Path;

import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;

public interface Shell {
  ReferenceLocation child(RelativeLocation path);

  ReferenceLocation child(String path);

  DirLocation pwd();

  String absolute(String path);

  DirLocation cd(RelativeLocation path);

  @sugar
  DirLocation pushd(String path);

  DirLocation pushd(DirLocation url);

  DirLocation pushd(RelativeLocation path);

  DirLocation popd();

  void mkdir(RelativeLocation path);

  void mkdir(String path);

  DirLocation mkdirAndPushd(String path);

  void deleteIfExists(String path);

  void addEnv(String name, String value);

  void execute(String command);

  void execute(String command, String... params);

  ProcessResult executeWithResult(String command);

  void blur(String sensibleRegex);

  default Path pwdPath() {
    return pwd().asPathLocation().toPath();
  }
}
