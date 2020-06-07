package org.raisercostin.jedio;

import lombok.Data;
import org.raisercostin.jedio.impl.LocationLike;

@Data
public class RelativeLocation implements LocationLike<RelativeLocation>, Location {
  private final String location;

  public static RelativeLocation create(String path) {
    // TODO Normalize file name String fileName =
    // org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
    while (path.startsWith("/")) {
      path = path.substring(1);
    }
    while (path.startsWith("\\")) {
      path = path.substring(1);
    }
    return new RelativeLocation(path);
  }
}
