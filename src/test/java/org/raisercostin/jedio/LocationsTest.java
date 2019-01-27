package org.raisercostin.jedio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class LocationsTest {
  @Test
  void test() {
    assertEquals("PathLocation(path="+Paths.get(".").toAbsolutePath().normalize()+")",Locations.current().toString());
  }
}
