package org.raisercostin.jedio.find;

import static org.raisercostin.jedio.find.Filters.NO_PRUNING_PATH_MATCHER;
import static org.raisercostin.jedio.find.Filters.createAny;
import static org.raisercostin.jedio.find.Filters.createGlob;
import static org.raisercostin.jedio.find.Filters.createTotalCommanderExpression;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

public class FindFilters {
  private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FindFilters.class);
  private static final MimetypesFileTypeMap allMimeTypes = new javax.activation.MimetypesFileTypeMap();

  public static TraversalFilter createTotalCommanderFilter(String totalCommanderFilter, boolean recursive) {
    List<String> list = Splitter.on("|").splitToList(totalCommanderFilter);
    String first = list.get(0);
    String second = "";
    if (list.size() == 2)
      second = list.get(1);
    if (list.size() > 2)
      throw new IllegalArgumentException("Strange split for " + totalCommanderFilter);
    return filter(createTotalCommanderExpression(first), createTotalCommanderExpression(second), true, false, recursive);
  }

  public static TraversalFilter globFilter(String matcher, String prunningMatcher, boolean ignoreCase,
      boolean dirsFirstInRecursive, boolean recursive) {
    return filter(createGlob(matcher), createGlob(prunningMatcher), ignoreCase, dirsFirstInRecursive, recursive);
  }

  public static TraversalFilter anyFilter(String matcher, String prunningMatcher, boolean ignoreCase,
      boolean dirsFirstInRecursive, boolean recursive) {
    return filter(createAny(matcher), createAny(prunningMatcher), ignoreCase, dirsFirstInRecursive, recursive);
  }

  public static TraversalFilter anyFilterNoPruning(String matcher, boolean ignoreCase, boolean dirsFirstInRecursive, boolean recursive) {
    return filter(createAny(matcher), NO_PRUNING_PATH_MATCHER, ignoreCase, dirsFirstInRecursive, recursive);
  }

  public static TraversalFilter filter2(PathMatcher matcher, PathMatcher pruningMatcher, boolean ignoreCase, boolean recursive) {
    return filter(matcher, pruningMatcher, ignoreCase, false, recursive);
  }

  public static TraversalFilter filter(PathMatcher matcher, PathMatcher pruningMatcher, boolean ignoreCase,
      boolean dirsFirstInRecursive, boolean recursive) {
    return new TraversalFilter() {
      @Override
      public PathMatcher matcher() {
        return matcher;
      }

      @Override
      public PathMatcher pruningMatcher() {
        return pruningMatcher;
      }

      @Override
      public boolean ignoreCase() {
        return ignoreCase;
      }

      @Override
      public boolean dirsFirstInRecursive() {
        return dirsFirstInRecursive;
      }

      @Override
      public boolean recursive() {
        return recursive;
      }
    };
  }

  public static TraversalFilter createFindFilter(String filter, String gitIgnore, boolean dirsFirst, boolean recursive) {
    PathMatcher matcher;
    if (filter.startsWith("glob:"))
      matcher = Filters.createAny(filter);
    else if (filter.startsWith("default:"))
      matcher = Filters.createGlob("**/*" + filter.replace("default:", "") + "*");
    else if (filter.startsWith("content:"))
      matcher = createContentFilter(StringUtils.unwrap(StringUtils.removeStart(filter, "content:"), "\""));
    else
      matcher = Filters.createGlob("**/*" + filter + "*");
    boolean ignoreCase = true;
    final TraversalFilter filter2 = FindFilters.filter(matcher, Filters.createGitFilter(gitIgnore, ignoreCase),
        ignoreCase, dirsFirst, recursive);
    return filter2;
  }

  private static PathMatcher createContentFilter(String filter) {
    return new PathMatcher() {
      @Override
      public boolean matches(Path path) {
        return checkIfContentOrPathContains(path, filter, false);
      }
    };
  }

  private static boolean checkIfContentOrPathContains(Path path, String filter, boolean ignoreCase) {
    try {
      if (Files.isRegularFile(path)) {
        if (Strings.isNullOrEmpty(filter))
          return true;
        else {
          if (Files.size(path) < 10000 && !isBinaryFile(path)) {
            logger.debug("searchInContent  [{}] in {}", filter, path);
            String content = new String(Files.readAllBytes(path), Charset.forName("UTF-8"));
            return contains(content, filter, ignoreCase);
          }
          return false;
        }
      } else {
        return false;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isBinaryFile(Path p) {
    String mimeType = allMimeTypes.getContentType(p.toFile());
    if (mimeType.equals("application/octet-stream") || mimeType.equals("application/pdf"))
      return true;
    return false;
    // return
    // OptionConverters.toJava(Locations.file(p).mimeTypeFromName()).map(x ->
    // x.isBinary()).orElse(false);
    // String type = Files.probeContentType(p);
    // System.out.println("probed ["+type+"]");
    // if (type == null) {
    // // type couldn't be determined, assume binary
    // return true;
    // } else if (type.startsWith("text")) {
    // return false;
    // } else {
    // // type isn't text
    // return true;
    // }
  }

  private static boolean contains(String string, String substring, boolean ignoreCase) {
    if (Strings.isNullOrEmpty(substring))
      return true;
    return containsIgnoreCase(string, substring, ignoreCase);
  }

  public static boolean containsIgnoreCase(String str, String searchStr, boolean ignoreCase) {
    if (str == null || searchStr == null)
      return false;

    final int length = searchStr.length();
    if (length == 0)
      return true;

    for (int i = str.length() - length; i >= 0; i--) {
      if (str.regionMatches(ignoreCase, i, searchStr, 0, length))
        return true;
    }
    return false;
  }
}
