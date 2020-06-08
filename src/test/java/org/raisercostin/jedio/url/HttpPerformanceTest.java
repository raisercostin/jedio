package org.raisercostin.jedio.url;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import io.vavr.collection.Vector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.url.JedioHttpClients.JedioHttpClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class HttpPerformanceTest {
  @Test
  void testMaxNumberOfConnections() {
    int totalCalls = 20000;
    //If sync number of threads influences the perf
    // 100 == 5000 (no diff)
    //1000 calls
    //1000 = 3.8s
    //500 = 3.48s
    //100 = 3.31s <----
    //50 = 3.44s
    //Max number of available connections is the number of threads
    int threads = 100; //Change createExecutor() from HttpClientLocation
    //10000 - 21s on 10.3.67.222
    //10000 - 12s on 10.3.67.222 - with reusable connections (not closed connections)
    //20000 - 22s - 100 routes
    //20000 - 21s - 50 routes & 100 threads
    //20000 - 25s - 1000 routes (available 91)
    //20000 - 23s - 1000 routes & 1000 threads (available 879)
    //20000 - 21s - 1000 routes & 2000 threads
    String testUrl = "http://10.3.67.222/";
    //"https://revobet-green-feed2-inplay.revomatico.com/inplay-soccer.json";

    JedioHttpClient client = JedioHttpClients.createHighPerfHttpClient();
    //    log.info("Using the following\nApacheHttpClient Settings:\n{}",
    //      Nodes.json.excluding("trustManager", "socketFactoryRegistry", "cookieSpecRegistry").toString(client));
    assertThatTime(Duration.ofSeconds(3), () -> {
      AtomicInteger finished = new AtomicInteger(0);
      Vector<Mono<Integer>> all = Vector.range(0, totalCalls)//.toJavaParallelStream().
        .map((Integer x) -> {
          log.info("x=" + x);
          return Locations.url(testUrl, client)
            .readContentAsync()
            .map(content -> {
              assertThat(content.length()).isGreaterThan(1);
              assertThat(content).startsWith("{\"updated\":\"");
              //assertThat(content).startsWith("<!DOCTYPE html>");
              int counter = finished.incrementAndGet();
              log.info("{} finished total={}, stats={}", x, counter, client.getStatus());
              return x;
            })
            .cache();
        });
      log.info("all started {}", all.size());

      //all.forEach(x -> x.block());
      //TODO to investigate
      //    Flux<Integer> flux = Flux.merge(all)
      //      .doOnNext(x -> log.info("{} mono finished", x));
      Flux<Integer> flux = Flux.fromIterable(all)
        .flatMap(x -> x)
        .doOnNext(x -> log.info("{} mono finished", x));
      //Disposable subscription = flux.subscribe();
      //subscription.
      flux.blockLast();
      //subscription.dispose();
      assertThat(finished.get()).isEqualTo(totalCalls);
    });
  }

  private void assertThatTime(Duration ofSeconds, Runnable runnable) {
    LocalDateTime start = LocalDateTime.now();
    runnable.run();
    LocalDateTime end = LocalDateTime.now();
    assertThat(Duration.between(start, end)).isLessThan(ofSeconds);
  }
}