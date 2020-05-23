package org.raisercostin.jedio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    assertEquals(346622,
      Locations.classpath("a b.jpg").copyTo(Locations.writableFile("target/ab-copied.jpg")).length());
  }

  @Test
  void testCopyToDontOverwrite() {
    final ReadableFileLocation src = Locations.classpath("a b.jpg");
    final WritableFileLocation dest = Locations.writableFile("target/ab-copied.jpg");
    dest.deleteFile();
    assertEquals(346622, src.copyTo(dest).length());
    assertThrows(FileAlreadyExistsException.class, () -> {
      src.copyTo(dest, CopyOptions.copyDoNotOverwrite());
    });
  }
}
