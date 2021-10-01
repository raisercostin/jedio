package org.raisercostin.jedio.url;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vavr.Function1;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jedio.feature.BooleanFeature;
import org.jedio.feature.GenericFeature;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.nodes.Nodes;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.retry.Retry;

public class WebClientLocation extends BaseHttpLocationLike<@NonNull WebClientLocation>
    implements ReadableFileLocation, Closeable {
  public static final WebClientFactory defaultClient = new WebClientFactory();

  public static WebClientLocation post(String url, MediaType applicationJson, Object params) {
    return defaultClient.post(url, applicationJson, params);
  }

  public static WebClientLocation get(String url) {
    return defaultClient.get(url);
  }

  @JsonIgnore
  public final WebClientFactory client;
  private final RequestHeadersSpec<?> request;

  private WebClientLocation(URL url, boolean escaped, WebClientFactory client) throws URISyntaxException {
    this(url, escaped, client.currentClient().get().uri(url.toURI()), client);
  }

  private WebClientLocation(URL url, boolean escaped, RequestHeadersSpec<?> request, WebClientFactory client) {
    super(url, escaped);
    this.client = client;
    this.request = request;
  }

  @Override
  @SneakyThrows
  protected WebClientLocation create(URL url, boolean escaped) {
    return new WebClientLocation(url, escaped, client);
  }

  public WebClientLocation reconfigure(Function1<RequestHeadersSpec<?>, RequestHeadersSpec<?>> requestSupplier) {
    return new WebClientLocation(url, true, requestSupplier.apply(request), client);
  }

  @Override
  @SneakyThrows
  public InputStream unsafeInputStream() {
    return pipeToInputStream(request.exchange().flatMap(x -> x.bodyToMono(DataBuffer.class)));
  }

  private InputStream pipeToInputStream(Mono<DataBuffer> data) throws IOException {
    PipedOutputStream osPipe = new PipedOutputStream();
    PipedInputStream isPipe = new PipedInputStream(osPipe);
    DataBufferUtils.write(data, osPipe)
      .subscribeOn(client.scheduler)
      .doOnComplete(() -> {
        try {
          osPipe.close();
        } catch (IOException ex) {
          log.debug("close with exception for {} unsafeInputStream", url, ex);
        }
      })
      .subscribe(DataBufferUtils.releaseConsumer());
    return isPipe;
  }

  @Override
  @SneakyThrows
  public boolean exists() {
    return request.exchange().map(x -> x.statusCode() != HttpStatus.NOT_FOUND).block(client.existsTimeout);
  }

  @Override
  public Mono<String> readContentAsync() {
    //return request.exchange().flatMap(x -> x.bodyToMono(String.class));
    return request.exchange().flatMap(clientResponse -> {
      if (clientResponse.statusCode().isError()) {
        return Mono.error(new ResponseStatusException(clientResponse.statusCode(), "Error"));
      } else {
        return Mono.just(clientResponse);
      }
    })
      .retryWhen(
        Retry.anyOf(ResponseStatusException.class)
          .randomBackoff(Duration.ofSeconds(1), Duration.ofSeconds(1000))
          .retryMax(3))
      .flatMap(x -> x.bodyToMono(String.class));
  }

  @Override
  public String readContentSync(Charset charset) {
    return readContentAsync().block(client.readContentSyncTimeout);
  }

  @Override
  public void close() throws IOException {
  }

  public static class WebClientFactory implements HttpClientLocationFactory {
    private WebClient client;
    private WebClient clientWithTap;
    public Builder builder;
    public Scheduler scheduler = Schedulers.elastic();
    public Duration existsTimeout = Duration.ofSeconds(30);
    public Duration readContentSyncTimeout = Duration.ofSeconds(30);
    public BooleanFeature webclientWireTap = GenericFeature.booleanFeature(
      "webclientWireTap", false, "feature.webclient.wireTap", true);
    public GenericFeature<Integer> webclientMaxConnections = GenericFeature.create(
      "webclientMaxConnections", 500, "feature.webclient.maxConnections", true);

    public WebClientFactory() {
      this.builder = createWebClient(false, webclientMaxConnections);
      this.client = builder.build();
      this.clientWithTap = createWebClient(true, webclientMaxConnections).build();
    }

    @Override
    @SneakyThrows
    public WebClientLocation get(String url) {
      URL url2 = new URL(url);
      return new WebClientLocation(url2, false, currentClient().get().uri(url2.toURI()), this);
    }

    @SneakyThrows
    public WebClientLocation post(String url, MediaType applicationJson, Object params) {
      log.info("post {}: {}", url, params);
      RequestHeadersSpec<?> req = currentClient()
        .post()
        .uri(url)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(params)
      //.retrieve()
      //.onStatus(statusPredicate, exceptionFunction)
      //      .onStatus(HttpStatus.NOT_FOUND::equals,
      //        clientResponse -> Mono.error(new ResponseStatusException(clientResponse.statusCode(), "")))
      //      .onStatus(HttpStatus::isError,
      //        clientResponse -> clientResponse.bodyToMono(ServiceException.class)
      //          .flatMap(serviceException -> Mono.error(serviceException)))
      //end retrieve
      ;
      return new WebClientLocation(new URL(url), false, req, this);
      //return Locations.urlPost(url, Nodes.json.toString(params)).readContentSync();
      //return HttpUtils.getUrl(url).getBody();
    }

    public final WebClient currentClient() {
      return webclientWireTap.isEnabled() ? clientWithTap : client;
    }

    @SuppressWarnings("deprecation")
    public static Builder createWebClient(boolean withTap, GenericFeature<Integer> webclientMaxConnections) {
      ConnectionProvider provider = ConnectionProvider
        .builder("revobet-webclient")
        .metrics(true)
        .maxConnections(webclientMaxConnections.value())
        //.pendingAcquireMaxCount(10)
        .build();
      //HttpClient.from(TcpClient.create(provider))
      //logging - https://www.baeldung.com/spring-log-webclient-calls
      HttpClient httpClient = HttpClient
        .create(provider)
        //.responseTimeout(Duration.ofSeconds(10))
        .compress(true)
        .wiretap(withTap)
      //      .tcpConfiguration(tcpClient -> tcpClient
      //        .bootstrap(b -> BootstrapHandlers.updateLogSupport(b,
      //          new LoggingHandler(HttpClient.class, LogLevel.INFO, ByteBufFormat.SIMPLE))))
      ;
      //.wiretap("reactor.netty.http.client.HttpClient",
      //  LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);

      //see RevobetFeatureService.debugLogNettyClient
      //      ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("reactor.netty.http.client"))
      //        .setLevel(Level.DEBUG);

      Builder client = WebClient.builder()
        .exchangeStrategies(ExchangeStrategies.builder()
          .codecs(configurer -> {
            configurer
              .defaultCodecs()
              .maxInMemorySize(256 * 1024 * 1024);
            configurer
              .defaultCodecs()
              .jackson2JsonEncoder(new Jackson2JsonEncoder(Nodes.json.mapper, MediaType.APPLICATION_JSON));
          })
          .build())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        //.baseUrl(url)
        .defaultCookie("cookieKey", "cookieValue")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultUriVariables(Collections.singletonMap("url", "http://localhost:8080"));
      return client;
    }
  }
}
