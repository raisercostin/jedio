package org.raisercostin.jedio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.op.CopyOptions;
import org.raisercostin.jedio.path.PathLocation;

class CopyTest {
  @Test
  void testCopyWithDefaultsWorks() {
    PathLocation dest = Locations.path("target/test-ab.jpg");
    Locations.classpath("folder/a b.jpg").copyToFile(dest, CopyOptions.copyDefault().withReplaceExisting(true));
    assertThat(dest.exists()).isTrue();
    assertThat(dest.length()).isEqualTo(346622);
    assertThatThrownBy(() -> Locations.classpath("folder/a b.jpg").copyToFile(dest, CopyOptions.copyDefault()))
      .hasMessageContaining("CopyIgnoreDestinationExists");
    assertThat(dest.exists()).isTrue();
    assertThat(dest.length()).isEqualTo(346622);
  }
}
