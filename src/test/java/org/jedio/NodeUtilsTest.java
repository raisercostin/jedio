package org.jedio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.TreeMap;

import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import com.typesafe.config.ConfigSyntax;
import io.vavr.API;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import org.junit.jupiter.api.Test;

class NodeUtilsTest {
  @Test
  void testExtractValuesFromVavrMap() {
    Map<String, String> container = API
      .Map("a", "valueA");
    assertThat(NodeUtils
      .object(container, "a"))
        .isEqualTo("valueA");
    assertThat(container
      .toString())
        .isEqualTo("HashMap((a, valueA))");
  }

  @Test
  void testExtractValuesFromMap() {
    TreeMap<Comparable, Object> container = Maps
      .newTreeMap();
    container
      .put("a", "valueA");
    assertThat(NodeUtils
      .object(container, "a"))
        .isEqualTo("valueA");
    assertThat(container
      .toString())
        .isEqualTo("{a=valueA}");
  }

  @Test
  void testExtractValuesFromVavrList() {
    List<String> container = API
      .List("valueA", "valueB");
    assertThat(NodeUtils
      .object(container, 0))
        .isEqualTo("valueA");
    assertThat(NodeUtils
      .object(container, 1))
        .isEqualTo("valueB");
    assertThat(container
      .toString())
        .isEqualTo("List(valueA, valueB)");
  }

  @Test
  void testExtractValuesFromList() {
    java.util.List<String> container = Arrays
      .asList("valueA", "valueB");
    assertThat(NodeUtils
      .object(container, 0))
        .isEqualTo("valueA");
    assertThat(NodeUtils
      .object(container, 1))
        .isEqualTo("valueB");
    assertThat(container
      .toString())
        .isEqualTo("[valueA, valueB]");
  }

  @Test
  void testAddValues() {
    Map<String, String> container = API
      .Map("a", "valueA");
    container = NodeUtils
      .withValueOf(container, "b", "valueB");
    assertThat(container
      .toString())
        .isEqualTo("HashMap((a, valueA), (b, valueB))");
  }

  @Test
  void testAddValuesTwoLevels() {
    Map<String, String> container = API
      .Map("a", "valueA");
    container = NodeUtils
      .withValueOf(container,
        "b",
        API
          .Map());
    container = NodeUtils
      .withValueOf(container, "b", "c", "valueC");
    assertThat(container
      .toString())
        .isEqualTo("HashMap((a, valueA), (b, HashMap((c, valueC))))");
    container = NodeUtils
      .withValueOf(container,
        "b",
        "d",
        API
          .List("e"));
    assertThat(container
      .toString())
        .isEqualTo("HashMap((a, valueA), (b, HashMap((c, valueC), (d, List(e)))))");
    container = NodeUtils
      .withValueOf(container, "b", "d", 1, "f");
    assertThat(container
      .toString())
        .isEqualTo("HashMap((a, valueA), (b, HashMap((c, valueC), (d, List(e, f)))))");
    container = NodeUtils
      .withValueOf(container, "b", "d", null, "g");
    assertThat(container
      .toString())
        .isEqualTo("HashMap((a, valueA), (b, HashMap((c, valueC), (d, List(e, f, g)))))");
    assertThat(NodeUtils
      .object(container, "b", "d", 0))
        .isEqualTo("e");
    assertThat(NodeUtils
      .withValueOf(container, "b", "d", 0)
      .toString())
        .isEqualTo("HashMap((a, valueA), (b, HashMap((c, valueC), (d, 0))))");
  }

  @Test
  void confSyntaxUsesSubstitutions() {
    //assertThrows(ConfigException.class, () -> {
    Config config = ConfigFactory
      .parseString("a=b,c,d\nfooA=${fooB}", ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF))
      .withFallback(ConfigFactory.systemProperties())
      .withFallback(ConfigFactory.systemEnvironment())
      .resolve(ConfigResolveOptions.noSystem().setUseSystemEnvironment(false).setAllowUnresolved(false));
    //});
    assertThat(config.getString("a")).isEqualTo("b,c,d");
    assertThat(config.getString("fooA")).isEqualTo("${fooB}");
  }
}
