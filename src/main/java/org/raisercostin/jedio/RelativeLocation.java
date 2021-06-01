package org.raisercostin.jedio;

import java.nio.file.Path;

import org.jedio.sugar;
import org.raisercostin.jedio.impl.SimpleRelativeLocation;

public interface RelativeLocation extends Location {

  @sugar
  static RelativeLocation relative(Path path) {
    return create(path.normalize().toString());
  }

  static RelativeLocation relative(String path) {
    return create(path);
  }

  static SimpleRelativeLocation create(String path) {
    // TODO Normalize file name String fileName =
    // org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
    while (path.startsWith("/")) {
      path = path.substring(1);
    }
    while (path.startsWith("\\")) {
      path = path.substring(1);
    }
    return new SimpleRelativeLocation(path);
  }

  String relativePath();

  @Override
  default String toExternalUri() {
    return "./" + relativePath();
  }
}
