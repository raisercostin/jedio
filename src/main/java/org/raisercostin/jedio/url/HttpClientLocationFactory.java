package org.raisercostin.jedio.url;

import java.time.Duration;

import io.vavr.Lazy;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.url.JedioHttpClient.JedioHttpConfig;
import org.raisercostin.jedio.url.WebClientLocation.WebClientFactory;

/**A factory for urls if the default settings are not enough.*/
public interface HttpClientLocationFactory {
  ReadableFileLocation get(String url);

  default ReadableFileLocation url(String url) {
    return get(url);
  }

  /**Lazyly created since the factory is resource heavy: thread pool, connection pool.*/
  //Lazy<HighPerfHttpClientFactory> highPerf = Lazy.of(() -> new HighPerfHttpClientFactory());
  /**Lazyly created since the factory is resource heavy: thread pool, connection pool.*/
  //Lazy<FiberHttpClientFactory> fiber = Lazy.of(() -> new FiberHttpClientFactory());

  //  static HighPerfHttpClientFactory highPerf() {
  //    return highPerf.get();
  //  }
  //  static FiberHttpClientFactory fiber() {
  //    return fiber.get();
  //  }
  //

  static WebClientFactory web() {
    return WebClientLocation.defaultClient;
  }

  public static class HighPerfHttpClientFactory implements HttpClientLocationFactory {
    JedioHttpClient client = JedioHttpConfig
      .create()
      .withName("revobet1")
      .withThreads(50)//300
      .withMaxTotal(100)//1000
      .withMaxPerRoute(100)//1000
      .withSoTimeout(Duration.ofSeconds(20))
      .withValidateAfterInactivity(Duration.ofSeconds(60))
      .createClient();

    @Override
    public ReadableFileLocation get(String url) {
      return new HttpClientLocation(url, false, client);
    }
  }

  public static class FiberHttpClientFactory implements HttpClientLocationFactory {
    //    CloseableHttpClient client = FiberUrlLocation2.defaultFactory.client;
    //
    //    @Override
    //    public ReadableFileLocation url(String url) {
    //      return new FiberUrlLocation2(url, false, false, client);
    //    }

    @Override
    public ReadableFileLocation get(String url) {
      throw new RuntimeException("Not implemented yet!!!");
    }
  }
}
