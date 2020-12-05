package org.jedio.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.vavr.Tuple;
import io.vavr.Tuple3;
import io.vavr.collection.Stream;
import io.vavr.collection.Traversable;
import io.vavr.match.annotation.Unapply;
import lombok.SneakyThrows;

public class RegexExtension {
  public static final LoadingCache<String, Pattern> COMPILED_PATTERNS = CacheBuilder.newBuilder()
    .maximumSize(100)
    .build(new CacheLoader<String, Pattern>()
      {
        @Override
        public Pattern load(String regexp) throws Exception {
          return Pattern.compile(regexp);
        }
      });

  @Unapply
  @SneakyThrows
  /**
   * Matches the given regex and returns the matched string (group0) and the group1 and group2.
   * The regex is cached.
   */
  public static Tuple3<String, String, String> regexp2(String regex, String value) {
    Matcher matcher = regexpMatcher(regex, value, 2);
    return Tuple.of(matcher.group(0), matcher.group(1), matcher.group(2));
  }

  @Unapply
  @SneakyThrows
  public static Traversable<String> regexpTraversable(String regex, String value, int matchedGroups) {
    Matcher matcher = regexpMatcher(regex, value, matchedGroups);
    return Stream.range(0, matchedGroups + 1).map(x -> matcher.group(x));
  }

  @Unapply
  @SneakyThrows
  public static Matcher regexpMatcher(String regex, String value, int matchedGroups) {
    Pattern pattern = COMPILED_PATTERNS.get(regex);
    Matcher matcher = pattern.matcher(value);
    if (matcher.matches()) {
      Preconditions.checkArgument(matchedGroups == matcher.groupCount());
      return matcher;
    }
    throw new RuntimeException("Regex [" + regex + "] didn't matched [" + value + "]");
  }
}
