package org.jedio;

import java.nio.file.Path;

import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.impl.DirLocationLike;
import org.raisercostin.jedio.impl.ReferenceLocationLike;

public interface Shell {
  ReferenceLocationLike<?> child(RelativeLocation path);

  ReferenceLocationLike<?> child(String path);

  DirLocationLike<?> pwd();

  String absolute(String path);

  DirLocationLike<?> cd(RelativeLocation path);

  @sugar
  DirLocationLike<?> pushd(String path);

  DirLocationLike<?> pushd(DirLocationLike<?> url);

  DirLocationLike<?> pushd(RelativeLocation path);

  DirLocationLike<?> popd();

  void mkdir(RelativeLocation path);

  void mkdir(String path);

  DirLocationLike<?> mkdirAndPushd(String path);

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
