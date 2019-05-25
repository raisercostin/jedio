package org.raisercostin.jedio.find;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

public interface TraversalFilter {
  PathMatcher matcher();

  PathMatcher pruningMatcher();

  @Deprecated // toremove
  boolean ignoreCase();

  default boolean shouldPrune(Path path) {
    return pruningMatcher().matches(path);
  }

  default boolean matches(Path path) {
    return matcher().matches(path);
  }

  //If dirs are first the traversal needs to first traverse everything. 
  boolean dirsFirst();
  boolean recursive();
}
