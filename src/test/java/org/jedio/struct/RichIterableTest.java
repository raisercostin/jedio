package org.jedio.struct;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RichIterableTest {
  @Test
  void testGroupByToMap() {
    RichIterable<String> all = RichIterable.of("a1", "b1", "a2", "b2");
    Map<Character, ArrayList<String>> grouped = all.groupByToMap(x -> x.charAt(0));
    assertThat(grouped.size()).isEqualTo(2);
    assertThat(grouped.get('a').toString()).isEqualTo("[a1, a2]");
    assertThat(grouped.get('b').toString()).isEqualTo("[b1, b2]");
  }

  @Test
  void testGroupByToMapOfRichIterables() {
    RichIterable<String> all = RichIterable.of("a1", "b1", "a2", "b2");
    Map<Character, RichIterable<String>> grouped = all.groupByToMapOfRichIterables(x -> x.charAt(0));
    assertThat(grouped.size()).isEqualTo(2);
    assertThat(grouped.get('a').mkString("-")).isEqualTo("a1-a2");
    assertThat(grouped.get('b').mkString("-")).isEqualTo("b1-b2");
  }
}
