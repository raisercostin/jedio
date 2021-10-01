package org.raisercostin.jedio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.fs.stream.InputStreamLocation;
import org.raisercostin.jedio.path.PathLocation;
import org.raisercostin.jedio.url.WebClientLocation;
import org.springframework.core.io.ClassPathResource;

public class SpringClassPathResourceTest {

  @Test
  void testClasspathFromSpringClasspathResouceAsStreamLocation() {
    InputStreamLocation classpathFile = Locations
      .stream(new ClassPathResource("classpath-resource1.md", ClasspathLocationTest.class));
    assertThat(classpathFile.readContentSync()).isEqualTo("resource content");
  }

  @Test
  void testClasspathFromSpringClasspathResouceAsPathLocation() {
    PathLocation classpathFile = Locations
      .path(new ClassPathResource("classpath-resource1.md", ClasspathLocationTest.class));
    assertThat(classpathFile.readContentSync()).isEqualTo("resource content");
  }

  @Test
  void testClasspathFromSpringClasspathResouceAsUrlLocation() {
    WebClientLocation classpathFile = Locations
      .url(new ClassPathResource("classpath-resource1.md", ClasspathLocationTest.class));
    assertThat(classpathFile.readContentSync()).isEqualTo("resource content");
  }
}
