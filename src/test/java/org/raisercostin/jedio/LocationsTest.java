package org.raisercostin.jedio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Paths;

import org.jedio.Audit.AuditException;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.op.CopyOptions;

class LocationsTest {
  @Test
  void test() {
    assertThat(Locations.current().toString()).contains(Paths.get(".").toAbsolutePath().normalize().toString());
  }

  @Test
  void testCopyTo() {
    assertThat(
      Locations.path("target/ab-copied.jpg")
        .copyFrom(Locations.classpath("a b.jpg"), CopyOptions.copyOverwrite())
        .length())
          .isEqualTo(346622);
  }

  @Test
  void testCopyToDontOverwrite() {
    final ReadableFileLocation src = Locations.classpath("a b.jpg");
    final WritableFileLocation dest = Locations.path("target/ab-copied.jpg").mkdirOnParentIfNeeded();
    dest.deleteFile();
    assertEquals(346622, dest.copyFrom(src, CopyOptions.copyDefault()).length());
    assertThrows(AuditException.class, () -> {
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

  @Test
  void locationsFromUrl2() {
    Location loc = Locations.location("file:/C:\\Users\\raiser/.revobet/lsports-cache");
    assertThat(loc).isNotNull();
  }
}
