package org.raisercostin.jedio;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class FindTest {
  @Test
  void testFindFilesAndDirs() {
    List<ExistingLocation> all = Locations.current().childDir("src/test/resources").findFilesAndDirs()
        .collectList().block();
    assertEquals(8, all.size());
  }

  @Test
  void testFindFiles() {
    List<FileLocation> all = Locations.current().childDir("src/test/resources").findFiles().collectList().block();
    System.out.println(all);
    assertEquals(6, all.size());
  }
}
