package org.raisercostin.jedio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.path.PathLocation;

public class ContentTest {

  @Test
  void writeContentIfNotExists() {
    double random = Math.random();
    String content = "randomContent:" + random;
    PathLocation a = Locations.tempDir("jedio").nonExistingChild("child1").writeContentIfNotExists(content);
    assertThat(a.readContentSync()).isEqualTo(content);
  }
}
