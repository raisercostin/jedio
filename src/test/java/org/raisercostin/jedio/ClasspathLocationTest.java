package org.raisercostin.jedio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClasspathLocationTest {
  @Test
  void testClasspathLs() {
    // Locations.existingDir("..").ls().filter(x -> x.getName().contains("java")).forEach(System.out::println);
    assertThat(Locations.classpathDir("folder\\..").ls().map(x -> x.getName()).mkString(","))
      .isEqualTo("a b.jpg,fileWithBom.txt,fileWithBom2.txt,folder,location.zip,logback-test.xml,org");
  }

  @Test
  void testClasspathNonExisting() {
    // Locations.existingDir("..").ls().filter(x -> x.getName().contains("java")).forEach(System.out::println);
    assertThat(Locations.classpathDir("none").ls().map(x -> x.getName()).mkString(","))
      .isEqualTo("a b.jpg,fileWithBom.txt,fileWithBom2.txt,folder,location.zip,logback-test.xml,org");
  }

  @Test
  void testClasspathInJarLs() {
    ReadableDirLocation classpathDir = Locations.classpathDir("org/apache/commons/io/FileUtils.class/..");
    System.out.println("abs=" + classpathDir.absoluteAndNormalized());
    classpathDir.ls().forEach(System.out::println);
    assertThat(Locations.classpathDir("spring-core.kotlin_module\\..").ls().map(x -> x.getName()).mkString(","))
      .isEqualTo("a b.jpg,fileWithBom.txt,fileWithBom2.txt,folder,location.zip,logback-test.xml,org");
  }
}
