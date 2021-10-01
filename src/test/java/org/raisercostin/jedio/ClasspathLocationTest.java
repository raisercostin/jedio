package org.raisercostin.jedio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.classpath.ClasspathLocation;
import org.raisercostin.jedio.fs.stream.InputStreamLocation;
import org.raisercostin.jedio.path.PathLocation;
import org.springframework.core.io.ClassPathResource;

class ClasspathLocationTest {
  @Test
  void testClasspathLs() {
    // Locations.existingDir("..").ls().filter(x -> x.getName().contains("java")).forEach(System.out::println);
    assertThat(Locations.classpath("folder\\..").ls().map(x -> x.getName()).mkString(","))
      .isEqualTo("a b.jpg,fileWithBom.txt,fileWithBom2.txt,folder,location.zip,logback-test.xml,org");
  }

  @Test
  void testClasspathNonExisting() {
    // Locations.existingDir("..").ls().filter(x -> x.getName().contains("java")).forEach(System.out::println);
    assertThat(Locations.classpath("none").ls().map(x -> x.getName()).mkString(","))
      .isEqualTo("a b.jpg,fileWithBom.txt,fileWithBom2.txt,folder,location.zip,logback-test.xml,org");
  }

  @Test
  void testClasspathInJarLs() {
    ClasspathLocation classpathDir = Locations.classpath("org/apache/commons/io/FileUtils.class/..");
    System.out.println("abs=" + classpathDir.absoluteAndNormalized());
    classpathDir.ls().forEach(System.out::println);
    assertThat(Locations.classpath("spring-core.kotlin_module\\..").ls().map(x -> x.getName()).mkString(","))
      .isEqualTo("a b.jpg,fileWithBom.txt,fileWithBom2.txt,folder,location.zip,logback-test.xml,org");
  }

  @Test
  void testClasspathResourceRelativeToClass() {
    ClasspathLocation classpathFile = Locations.classpath(ClasspathLocationTest.class, "classpath-resource1.md");
    assertThat(classpathFile.readContentSync()).isEqualTo("resource content");
  }
}
