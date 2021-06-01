package org.raisercostin.jedio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.op.CopyOptions;

class LocationsTest {
  @Test
  void test() {
    assertEquals("PathLocation(path=" + Paths.get(".").toAbsolutePath().normalize() + ")",
      Locations.current().toString());
  }

  @Test
  void testCopyTo() {
    assertEquals(346622, Locations.classpath("a b.jpg").copyToFile(Locations.path("target/ab-copied.jpg")).length());
  }

  @Test
  void testCopyToDontOverwrite() {
    final ReadableFileLocation src = Locations.classpath("a b.jpg");
    final WritableFileLocation dest = Locations.path("target/ab-copied.jpg").mkdirOnParentIfNeeded();
    dest.deleteFile();
    assertEquals(346622, src.copyToFile(dest).length());
    assertThrows(FileAlreadyExistsException.class, () -> {
      src.copyToFile(dest, CopyOptions.copyDoNotOverwriteAndThrow());
    });
  }

  private static final String FILE = "file:/C:/Users/raiser/file1.txt";

  @Test
  void locationsFromUrl() {
    Location loc = Locations.location(FILE);
    assertThat(loc).isNotNull();
  }

  @Test
  void locations3() throws MalformedURLException, URISyntaxException {
    assertThat(
      Paths.get(new URI(FILE))
        .toUri()
        .toURL()
        .toExternalForm())
          .isEqualTo(FILE);
  }
}
