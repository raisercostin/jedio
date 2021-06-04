package org.raisercostin.jedio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;

import org.jedio.Audit.AuditException;
import org.jedio.struct.RichIterable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.path.PathLocation;

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

  @Test
  void locations0Relative() {
    assertThat(Locations.pathFromExternalForm("file:raiser/file1.txt").absoluteAndNormalized())
      .isEqualTo(Locations.current().absoluteAndNormalized() + "/raiser/file1.txt");
  }

  @Test
  void locations0RelativeUserHomeOnWindows() {
    //C:/ acts as an absolute path
    assertThat(Locations.pathFromExternalForm("file:C:/Users/raiser/file1.txt").absoluteAndNormalized())
      .isEqualTo("C:/Users/raiser/file1.txt");
  }

  @Test
  void locations0RelativeUserHomeOnLinux() {
    assertThat(Locations.pathFromExternalForm("file:Users/raiser/file1.txt").absoluteAndNormalized())
      .isEqualTo(Locations.current().absoluteAndNormalized() + "/Users/raiser/file1.txt");
  }

  @Test
  void locations1() {
    assertThat(Locations.pathFromExternalForm("file:/C:/Users/raiser/file1.txt").absoluteAndNormalized())
      .isEqualTo("C:/Users/raiser/file1.txt");
  }

  @SuppressWarnings("null")
  @Test
  void locations2HostnameC() {
    assertThatThrownBy(() -> Locations.pathFromExternalForm("file://C:/Users/raiser/file1.txt").absoluteAndNormalized())
      .hasMessageContaining("Maybe you wanted to pass [file://localhost/C:/Users/raiser/file1.txt]")
      .hasMessageContaining("The hostname [C:]");
  }

  @SuppressWarnings("null")
  @Test
  void locations2HostnameCRelative() {
    assertThatThrownBy(() -> Locations.pathFromExternalForm("file://raiser/file1.txt").absoluteAndNormalized())
      .hasMessageContaining("Maybe you wanted to pass [file://localhost/raiser/file1.txt]")
      .hasMessageContaining("The hostname [raiser]");
  }

  @Test
  void locations3Absolute() {
    assertThat(Locations.pathFromExternalForm("file:///C:/Users/raiser/file1.txt").absoluteAndNormalized())
      .isEqualTo("C:/Users/raiser/file1.txt");
  }

  @Test
  void locations3AbsoluteWithLocalhost() {
    assertThat(Locations.pathFromExternalForm("file://localhost/C:/Users/raiser/file1.txt").absoluteAndNormalized())
      .isEqualTo("C:/Users/raiser/file1.txt");
  }

  @Test
  void locations3AbsoluteLinux() {
    assertThat(Locations.pathFromExternalForm("file:////home/raiser/file1.txt").absoluteAndNormalized())
      .isEqualTo(PathLocation.rootDir() + "home/raiser/file1.txt");
  }

  @Test
  void locations3AbsoluteLinuxWithLocalhost() {
    assertThat(Locations.pathFromExternalForm("file://localhost//home/raiser/file1.txt").absoluteAndNormalized())
      .isEqualTo(PathLocation.rootDir() + "home/raiser/file1.txt");
  }

  @Test
  void locations3AbsoluteButRelativeToRootDir() {
    assertThat(Locations.pathFromExternalForm("file:///raiser/file1.txt").absoluteAndNormalized())
      .isEqualTo(PathLocation.rootDir() + "raiser/file1.txt");
  }

  @Test
  void locations3AbsoluteButRelativeToRootDirWithLocalhost() {
    assertThat(Locations.pathFromExternalForm("file://localhost/raiser/file1.txt").absoluteAndNormalized())
      .isEqualTo(PathLocation.rootDir() + "raiser/file1.txt");
  }

  @Test
  void locations4() {
    assertThat(Locations.pathFromExternalForm("file:////raiser/file1.txt").absoluteAndNormalized())
      .isEqualTo(PathLocation.rootDir() + "raiser/file1.txt");
  }

  @Test
  void locations4WithLocalhost() {
    assertThat(Locations.pathFromExternalForm("file://localhost//raiser/file1.txt").absoluteAndNormalized())
      .isEqualTo(PathLocation.rootDir() + "raiser/file1.txt");
  }

  static Iterable<Arguments> configsNoArgs() {
    return RichIterable
      .of(Arguments.of("file:/C:/Users/raiser/file1.txt", "file://localhost/C:/Users/raiser/file1.txt"),
        Arguments.of("file:///C:/Users/raiser/file1.txt", "file://localhost/C:/Users/raiser/file1.txt"),
        Arguments.of("file://localhost/C:/Users/raiser/file1.txt", "file://localhost/C:/Users/raiser/file1.txt"))
      .iterable();
  }

  @ParameterizedTest
  @MethodSource("configsNoArgs")
  void locationsFromUrl(String file, String expected) throws MalformedURLException, URISyntaxException {
    Location loc = Locations.location(file);
    assertThat(loc).isNotNull();
    assertThat(loc.toExternalUri())
      .isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("configsNoArgs")
  @Disabled
  //Useful to understand how the jvm works
  void locations3(String file) throws MalformedURLException, URISyntaxException {
    assertThat(
      Paths.get(new URI(file))
        .toUri()
        .toURL()
        .toExternalForm())
          .isEqualTo(file);
  }

  @Test
  void locationsFromUrl2() {
    Location loc = Locations.location("file:/C:\\Users\\raiser/.revobet/lsports-cache");
    assertThat(loc).isNotNull();
  }

  @Test
  void locationsBug1OnLinuxCannotParseIt() {
    Location loc = Locations.location("file://localhost//root/.revobet/lsports-cache");
    assertThat(loc).isNotNull();
  }

}
