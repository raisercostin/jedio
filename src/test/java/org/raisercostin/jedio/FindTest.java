package org.raisercostin.jedio;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class FindTest {
  @Test
  void testFindFilesAndFolders() {
    List<ExistingLocation> all = Locations.current().childFolder("src/test/resources").findFilesAndFolders()
        .collectList().block();
    assertEquals(8, all.size());
  }

  @Test
  void testFindFiles() {
    List<FileLocation> all = Locations.current().childFolder("src/test/resources").findFiles().collectList().block();
    System.out.println(all);
    assertEquals(6, all.size());
  }
}
