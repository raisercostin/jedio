package org.raisercostin.jedio;

import org.junit.jupiter.api.Test;

class ClasspathLocationTest {
  @Test
  void test() {
    Locations.existingDir("d:\\home\\raiser\\work\\").ls().filter(x->x.getName().contains("java")).forEach(System.out::println);
    Locations.classpathDir("folder\\..").ls().forEach(System.out::println);
  }
}
