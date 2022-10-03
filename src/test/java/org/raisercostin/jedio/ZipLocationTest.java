package org.raisercostin.jedio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jedio.struct.RichIterable;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.zip.ZipLocation;

class ZipLocationTest {

  @Test
  void test() {
    RichIterable<@NonNull ZipLocation> ls = Locations.path("src/test/resources/location.zip")
      .unzip()
      .ls()
      .memoizeJava();
    ls.forEach(x -> System.out.println(x));
    assertThat(ls.map(x -> x.entry().getName()).mkString(","))
      .isEqualTo("c/,c/d.txt,c/e/,c/e/f.txt,a.txt,b.txt,c/subzip.zip");
    assertThat(ls.length()).isEqualTo(7);
  }
}
