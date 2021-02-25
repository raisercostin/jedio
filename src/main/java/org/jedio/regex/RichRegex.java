package org.jedio.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.collection.Stream;
import io.vavr.collection.Traversable;
import io.vavr.control.Either;
import io.vavr.match.annotation.Unapply;
import lombok.SneakyThrows;

//TODO https://stackoverflow.com/questions/5767627/how-to-add-features-missing-from-the-java-regex-implementation/5771326#5771326
//TODO http://pleac.sourceforge.net/pleac_groovy/patternmatching.html
public class RichRegex {
  public static final LoadingCache<String, Pattern> patterns = CacheBuilder.newBuilder()
    .maximumSize(100)
    .build(new CacheLoader<String, Pattern>()
      {
        @Override
        public Pattern load(String regexp) throws Exception {
          return Pattern.compile(regexp);
        }
      });

  /**Compiles the regex and caches it. The cache has a 100 values limit and can be accessed via `patterns`.*/
  @SneakyThrows
  public static Pattern compiled(String regex) {
    return patterns.get(regex);
  }

  @Unapply
  @SneakyThrows
  /**
   * Matches the given regex and returns the matched string (group0) and the group1 and group2.
   * The regex is cached.
   */
  public static Either<String, Tuple2<Matcher, String>> regexp1(String regex, String value) {
    Either<String, Matcher> matcher = regexpMatcher(regex, value, 1);
    return matcher.map(m -> Tuple.of(m, m.group(1)));
  }

  @Unapply
  @SneakyThrows
  /**
   * Matches the given regex and returns the matched string (group0) and the group1 and group2.
   * The regex is cached.
   */
  public static Either<String, Tuple3<Matcher, String, String>> regexp2(String regex, String value) {
    Either<String, Matcher> matcher = regexpMatcher(regex, value, 2);
    return matcher.map(m -> Tuple.of(m, m.group(1), m.group(2)));
  }

  @Unapply
  @SneakyThrows
  public static Either<String, Traversable<String>> regexpTraversable(String regex, String value, int matchedGroups) {
    Either<String, Matcher> matcher = regexpMatcher(regex, value, matchedGroups);
    return matcher.map(m -> Stream.range(0, matchedGroups + 1).map(x -> m.group(x)));
  }

  @Unapply
  @SneakyThrows
  public static Either<String, Matcher> regexpMatcher(String regex, String value, int requiredGroups) {
    Pattern pattern = patterns.get(regex);
    Matcher matcher = pattern.matcher(value);
    if (matcher.find()) {
      int matchedGroups = matcher.groupCount();
      if (matchedGroups < requiredGroups) {
        return Either.left(
          String.format("Matched %s groups but must be at least %s matched on [%s] with [%s]", matchedGroups,
            requiredGroups, value, regex));
      }
      return Either.right(matcher);
    }
    return Either.left(String.format("No matching on [%s] with [%s]", value, regex));
  }
}
