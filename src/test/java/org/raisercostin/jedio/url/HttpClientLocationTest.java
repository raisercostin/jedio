package org.raisercostin.jedio.url;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.url.JedioHttpClients.JedioHttpClient;

class HttpClientLocationTest {
  JedioHttpClient defaultClient = JedioHttpClients.createHighPerfHttpClient();

  @Test
  void test() {
    assertThat(HttpClientLocation.url("http://jedio.org", "contact", defaultClient).toExternalForm())
      .isEqualTo("http://jedio.org/contact");
  }

  @Test
  void test2() {
    assertThat(HttpClientLocation.url("http://jedio.org", "/", defaultClient).toExternalForm())
      .isEqualTo("http://jedio.org/");
  }

  @Test
  void test3() {
    assertThat(HttpClientLocation.url("http://jedio.org", "", defaultClient).toExternalForm())
      .isEqualTo("http://jedio.org");
  }
}
