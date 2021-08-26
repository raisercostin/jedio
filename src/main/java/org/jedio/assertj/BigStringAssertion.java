package org.jedio.assertj;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.AssertDelegateTarget;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.path.PathLocation;

public class BigStringAssertion implements AssertDelegateTarget {
  public static BigStringAssertion assertThatBigString(String actual) {
    return new BigStringAssertion(actual);
  }

  private String actual;

  public BigStringAssertion(String actual) {
    this.actual = actual;
  }

  @SuppressWarnings("null")
  public void isEqualTo(String expected) {
    int maxLength = 20_000;
    int commonPrefixLength = maxLength / 10;
    if (actual.length() < maxLength && expected.length() < maxLength) {
      assertThat(actual).isEqualTo(expected);
    } else {
      String prefix = Strings.commonPrefix(actual, expected);
      if (prefix.length() < maxLength) {
        //if the strings are different before maxLength compare prefix as well
        prefix = "";
      }
      int start = prefix.length() - commonPrefixLength;
      String actualTruncated = StringUtils.abbreviate(actual, "\n...truncated(" + start + ")...\n", start, maxLength);
      String expectedTruncated = StringUtils.abbreviate(expected, "\n...truncated(" + start + ")...\n", start,
        maxLength);
      if (prefix.length() != actual.length() || prefix.length() != expected.length()) {
        PathLocation place = Locations.tempFile("test", "");
        PathLocation actualFile = place.withName(x -> x + "-actual").write(actual);
        PathLocation expectedFile = place.withName(x -> x + "-expected").write(expected);
        place.withName(x -> x + "-actualTruncated").write(actualTruncated);
        place.withName(x -> x + "-expectedTruncated").write(expectedTruncated);
        assertThat(actualTruncated).describedAs("Actual file [%s] expected [%s] and prefix of length %s",
          actualFile.absoluteAndNormalized(),
          expectedFile.absoluteAndNormalized(),
          prefix.length())
          .isEqualTo(expectedTruncated);
      }
    }
  }
}
