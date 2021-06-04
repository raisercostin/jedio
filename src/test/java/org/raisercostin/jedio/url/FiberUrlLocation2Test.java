package org.raisercostin.jedio.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.url.FiberUrlLocation2.FiberUrlLocationFactory;
import reactor.core.publisher.Flux;

@Slf4j
@Tag("slow")
@Tag("integration")
@Disabled
class FiberUrlLocation2Test {
  final FiberUrlLocationFactory factory = FiberUrlLocation2.defaultFactory;
  // final String betsApiBaseUrl = "https://api.betsapi.com";
  final String betsApiBaseUrl = "https://revobet-green-feed1.revomatico.com";

  @Test
  void testNginx() {
    int calls = 500;
    String baseUrl = "http://10.3.67.222/";
    Tuple2<Integer, Integer> res = assertTimeoutPreemptively(Duration.ofSeconds(2),
      () -> testFiberUrlLocationPerformance(baseUrl, calls));
    String result = message(1, res, calls);
    assertThat(result).isEqualTo(message(1, Tuple.of(calls, 0), calls));
  }

  @Test
  void testNginxWithWarmUp() {
    String baseUrl = "http://10.3.67.222/";
    // 8 seconds 10*1100 calls
    assertTimeoutPreemptively(Duration.ofSeconds(8), () -> testWithWarmup(baseUrl));
  }

  @Test
  void testBetsapi() {
    int calls = 500;
    String baseUrl = String.format("%s/v1/bet365/result?token=16670-qW18MIAejtyQQG&event_id=83218362",
      betsApiBaseUrl);
    Tuple2<Integer, Integer> res = assertTimeoutPreemptively(Duration.ofSeconds(10),
      () -> testFiberUrlLocationPerformance(baseUrl, calls));
    String result = message(1, res, calls);
    assertThat(result).isEqualTo(message(1, Tuple.of(calls, 0), calls));
  }

  @Test
  void testBetsapiWithWarmUp() {
    String baseUrl = String.format("%s/v1/bet365/result?token=16670-qW18MIAejtyQQG&event_id=83218362",
      betsApiBaseUrl);
    testWithWarmup(baseUrl);
    // 19 seconds 10*1100 calls erors:13%-21%
    // assertTimeoutPreemptively(Duration.ofSeconds(23), () -> testWithWarmup(baseUrl));

    // Timeout connecting to [api.betsapi.com/104.27.189.54:443]
    // =>? https://github.com/elastic/elasticsearch/issues/24069
  }

  private void testWithWarmup(String baseUrl) {
    int calls = 500;
    // warmup
    testFiberUrlLocationPerformance(baseUrl, calls);
    int repeats = 10;
    Tuple2<Integer, Integer> res = Stream.range(0, repeats)
      .map(x -> assertTimeoutPreemptively(Duration.ofSeconds(5),
        () -> testFiberUrlLocationPerformance(x + "", baseUrl, false, calls)))
      .reduce((x, y) -> Tuple.of(x._1 + y._1, x._2 + y._2));
    int total = res._1 + res._2;
    String result = message(repeats, res, total);
    log.info(result);
    assertThat(result).isEqualTo(message(repeats, Tuple.of(total, 0), total));
  }

  private String message(int repeats, Tuple2<Integer, Integer> res, int total) {
    String result = String.format("repeats=%s success=%s=%s%% errors=%s=%s%% total=%s", repeats, res._1,
      res._1 * 100 / total, res._2, res._2 * 100 / total, total);
    return result;
  }

  private Tuple2<Integer, Integer> testFiberUrlLocationPerformance(String baseUrl, int calls) {
    return testFiberUrlLocationPerformance("warmup", baseUrl, true, calls);
  }

  @SneakyThrows
  private Tuple2<Integer, Integer> testFiberUrlLocationPerformance(String repeat, String baseUrl, boolean warmup,
      int calls) {
    int parallelism = 8;// 4 threads for fibers
    // Scheduler scheduler = Schedulers.newParallel("bet365-pre", 1);
    // String a = Flux.range(0, calls).parallel(calls).runOn(scheduler).map(x -> {
    // log.info("counter=" + x);
    // return x;
    // }).map(x -> one.readContent()).map(x -> {
    // log.info(x);
    // return x;
    // }).sequential().take(calls).onErrorContinue((error, item) -> RevobetExceptionLogger.handleWarn(error,
    // "error2")).blockLast();
    FiberForkJoinScheduler fiberForkJoinScheduler = new FiberForkJoinScheduler("fiber-url-location", parallelism);
    AtomicInteger counter = new AtomicInteger(0);

    List<Fiber> all = Stream.range(0, calls).map(j -> new Fiber(fiberForkJoinScheduler, (SuspendableRunnable) () -> {
      String url = baseUrl + "?" + j;
      log.info(repeat + " " + j + "> start        " + url);
      final String content = factory.url(url).readContent();
      // if(warmup)
      // Fiber.sleep(1);
      log.info(repeat + " " + j + "> end   " + counter.incrementAndGet() + "th " + url + " ==> "
          + content.substring(0, 100).replaceAll("\n", "\\n"));
    }).start()).toList();

    io.vavr.collection.List<Try<Strand>> results = all.map(strand -> Try.ofCallable(() -> {
      strand.join();
      return strand;
    }));
    Tuple2<io.vavr.collection.List<Try<Strand>>, io.vavr.collection.List<Try<Strand>>> res = results
      .partition(x -> x.isSuccess());
    log.info("repeat={} success={} errors={} total={}", repeat, res._1.size(), res._2.size(), results.size());
    return Tuple.of(res._1.size(), res._2.size());
  }

  @Test
  @Suspendable
  @Disabled
  void testFiberFromReactor() {
    String baseUrl = "http://10.3.67.222/";
    Flux.interval(Duration.ofMillis(10))
      .onBackpressureLatest()
      .flatMap(x -> FiberUrlLocation2.defaultFactory.url(baseUrl).readContentAsync().map(y -> Tuple.of(x, y)))
      .doOnNext(x -> {
        log.info("onEach" + x._1);
      })
      .doOnError(x -> log.error("aaa", x))
      .take(1000)
      .blockLast(Duration.ofSeconds(10));
  }
}
