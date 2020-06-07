package org.raisercostin.jedio.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URL;

import lombok.SneakyThrows;
import org.apache.xerces.util.URI;
import org.junit.jupiter.api.Test;

class HttpClientLocationTest {
  @Test
  void test() {
    assertThat(UrlLocation.of("http://jedio.org").child("contact").toExternalForm())
        .isEqualTo("http://jedio.org/contact");
    assertThat(UrlLocation.of("http://jedio.org").child("").toExternalForm()).isEqualTo("http://jedio.org");
  }

  @Test
  void test2() {
    assertThat(UrlLocation.of("http://jedio.org").child("/").toExternalForm()).isEqualTo("http://jedio.org/");
  }

  @Test
  void test3() {
    assertThat(UrlLocation.of("http://www.revomatico.com/benefits/clients/DFPRADM")
        .child("http://www.revomatico.com/benefits/clients/DFPRADM/").toExternalForm())
            .isEqualTo("http://www.revomatico.com/benefits/clients/DFPRADM/");
  }

  @Test
  void test4() {
    assertThat(UrlLocation.of("http://www.revomatico.com/camel/")
        .child("portfolio/products/bean:cxfEndpoint|//someAddress").toExternalForm())
            .isEqualTo("http://www.revomatico.com/camel/portfolio/products/bean:cxfEndpoint%7C//someAddress");
  }

  @Test
  void testReEscapeShouldNotHappen() {
    assertThat(UrlLocation.of("http://www.revomatico.com/|").toExternalForm())
        .isEqualTo("http://www.revomatico.com/%7C");
    assertThat(UrlLocation.of("http://www.revomatico.com/%7C").toExternalForm())
        .isEqualTo("http://www.revomatico.com/%257C");
    assertThat(UrlLocation.of("http://www.revomatico.com/%7C", true).toExternalForm())
        .isEqualTo("http://www.revomatico.com/%7C");
  }

  @Test
  void testReEscapeShouldNotHappen2() {
    assertThat(UrlLocation.of("http://www.revomatico.com/b/|/").child("a").toExternalForm())
        .isEqualTo("http://www.revomatico.com/b/%7C/a");
    assertThat(UrlLocation.of("http://www.revomatico.com/b/|").child("a").toExternalForm())
        .describedAs("[a] should replace [/]").isEqualTo("http://www.revomatico.com/b/a");
    assertThat(UrlLocation.of("http://www.revomatico.com/b/|/").child("./d").toExternalForm())
        .isEqualTo("http://www.revomatico.com/b/%7C/d");
    assertThat(UrlLocation.of("http://www.revomatico.com/b/|").child("./d").toExternalForm())
        .isEqualTo("http://www.revomatico.com/b/d");
    assertThat(UrlLocation.of("http://www.revomatico.com/|").child("/d").toExternalForm())
        .isEqualTo("http://www.revomatico.com/d");
  }

  @Test
  void test5() {
    assertThat(UrlLocation.of("http://www.revomatico.com/camel/portfolio/products/bean:cxfEndpoint|//someAddress")
        .child("").toExternalForm())
            .isEqualTo("http://www.revomatico.com/camel/portfolio/products/bean:cxfEndpoint%7C//someAddress");
  }

  @Test
  @SneakyThrows
  void test6() {
    String address = "http://www.revomatico.com/camel/portfolio/products/bean:cxfEndpoint|//someAddress";
    new URL(address);
    assertThatThrownBy(() -> new URI(address)).hasMessageContaining("Path contains invalid character: |");
    assertThat(UrlLocation.of(address, false).toUri().toString())
        .isEqualTo("http://www.revomatico.com/camel/portfolio/products/bean:cxfEndpoint%7C//someAddress");
    assertThat(UrlLocation.of(address, false).toUrl().toExternalForm())
        .isEqualTo("http://www.revomatico.com/camel/portfolio/products/bean:cxfEndpoint%7C//someAddress");
  }

  @Test
  void test7() {
    assertThatThrownBy(() -> UrlLocation.of("//www.revomatico.com/").child("").toExternalForm())
        .hasMessageContaining("no protocol: //www.revomatico.com/");
  }

  @Test
  void test8() {
    assertThat(
        UrlLocation.of("http://www.revomatico.com/benefits/clients/DFPRADM").child("/portofolio").toExternalForm())
            .isEqualTo("http://www.revomatico.com/portofolio");
  }
}
