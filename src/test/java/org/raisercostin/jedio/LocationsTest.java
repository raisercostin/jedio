package org.raisercostin.jedio;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;

class LocationsTest {
  @Test
  void test() {
    Flux<FileAltered> all = Locations.existingFolder("target/watched").asChangableLocation().watch();
    all.log("rec").map(x->x.location()).log("2").blockLast(Duration.ofSeconds(200));
  }
}
