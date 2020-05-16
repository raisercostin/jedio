package org.raisercostin.jedio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import io.vavr.collection.List;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.path.PathLocation;

class FindTest {
  @Test
  void testFindFilesAndDirs() {
    List<PathLocation> all = Locations.current().childDir("src/test/resources").ls(true).toList();
    assertEquals(8, all.size());
  }

  @Test
  void testFindFiles() {
    List<PathLocation> all = Locations.current().childDir("src/test/resources").findFiles(true).toList();
    System.out.println(all);
    assertEquals(6, all.size());
  }

  @Test
  void testFindFilesGradually() {
    assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
      DirLocation all = Locations.existingDir("e:/work/electrica")
        .findDirs(true)
        .doOnNext(x -> System.out.println(x))
        .blockFirst(Duration.ofSeconds(1));
      // .blockLast();
      System.out.println(all);
    });
  }
}
