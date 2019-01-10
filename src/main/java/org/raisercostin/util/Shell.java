package org.raisercostin.util;

import java.nio.file.Path;

import org.raisercostin.jedio.FolderLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;

public interface Shell {
  ReferenceLocation child(RelativeLocation path);

  ReferenceLocation child(String path);

  FolderLocation pwd();

  String absolute(String path);

  FolderLocation cd(RelativeLocation path);

  @sugar
  FolderLocation pushd(String path);

  FolderLocation pushd(FolderLocation url);

  FolderLocation pushd(RelativeLocation path);

  FolderLocation popd();

  void mkdir(RelativeLocation path);

  void mkdir(String path);

  FolderLocation mkdirAndPushd(String path);

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
