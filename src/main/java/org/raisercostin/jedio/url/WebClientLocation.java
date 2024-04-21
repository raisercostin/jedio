package org.raisercostin.jedio.url;

import static io.vavr.API.Seq;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.netty.handler.logging.LogLevel;
import io.vavr.Function1;
import io.vavr.Tuple;
import lombok.SneakyThrows;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jedio.sugar;
import org.jedio.feature.BooleanFeature;
import org.jedio.feature.EnumFeature;
import org.jedio.feature.GenericFeature;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.op.OperationOptions.ReadOptions;
import org.raisercostin.jedio.url.impl.ModifiedURI;
import org.raisercostin.nodes.Nodes;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;
import reactor.util.retry.RetryBackoffSpec;

public class WebClientLocation extends BaseHttpLocationLike<@NonNull WebClientLocation>
    implements ReadableFileLocation {
  public static final WebClientFactory defaultClient = new WebClientFactory();

  @sugar
  public static WebClientLocation url(String sourceHyperlink, String relativeOrAbsoluteHyperlink) {
    return httpGet(SimpleUrl.resolve(sourceHyperlink, relativeOrAbsoluteHyperlink).toExternalForm());
  }

  @sugar
  public static WebClientLocation url(SimpleUrl url) {
    return httpGet(url.toExternalForm());
  }

  @sugar
  public static WebClientLocation url(URI uri) {
    return httpGet(uri.toString());
  }

  public static WebClientLocation post(String url, MediaType applicationJson, Object bodyValue) {
    return defaultClient.post(url, applicationJson, bodyValue);
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  public static <S extends RequestHeadersUriSpec<S>> WebClientLocation httpGet(String url,
      Function1<RequestHeadersUriSpec<S>, RequestHeadersUriSpec<S>> requestCreator) {
    URL url2 = new URL(url);
    return new WebClientLocation(url2, false,
      requestCreator.apply((S) defaultClient.currentClient().get()).uri(url2.toURI()),
      defaultClient);
  }

  @SneakyThrows
  public static WebClientLocation httpPost(String url,
      Function1<RequestBodyUriSpec, RequestBodyUriSpec> requestCreator) {
    URL url2 = new URL(url);
    return new WebClientLocation(url2, false,
      requestCreator.apply(defaultClient.currentClient().post()).uri(url2.toURI()),
      defaultClient);
  }

  @SneakyThrows
  public static WebClientLocation httpPut(String url,
      Function1<RequestBodyUriSpec, RequestBodyUriSpec> requestCreator) {
    URL url2 = new URL(url);
    return new WebClientLocation(url2, false,
      requestCreator.apply(defaultClient.currentClient().put()).uri(url2.toURI()),
      defaultClient);
  }

  @SneakyThrows
  public static WebClientLocation httpMethod(String url, HttpMethod method,
      Function1<RequestBodyUriSpec, RequestBodyUriSpec> requestCreator) {
    URL url2 = new URL(url);
    return new WebClientLocation(url2, false,
      requestCreator.apply(defaultClient.currentClient().method(method)).uri(url2.toURI()),
      defaultClient);
  }

  public static WebClientLocation httpGet(String url) {
    return defaultClient.get(url);
  }

  public static WebClientLocation httpGet(ModifiedURI uri) {
    return defaultClient.get(uri);
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
    if (client.retryOnAnyError.isEnabled()) {
      return readContentAsyncWithRetry();
    }
    //return request.exchange().flatMap(x -> x.bodyToMono(String.class));
    return request.exchange().flatMap(clientResponse -> {
      if (clientResponse.statusCode().isError()) {
        return Mono.error(new RequestError(this, clientResponse, "Error"));
      } else {
        return Mono.just(clientResponse);
      }
    })
      //      .retryWhen(
      //        Retry.anyOf(ResponseStatusException.class)
      //          .randomBackoff(Duration.ofSeconds(1), Duration.ofSeconds(1000))
      //          .retryMax(3))
      .flatMap(x -> x.bodyToMono(String.class));
  }

  @ToString
  public static class RequestError extends ResponseStatusException {
    private ClientResponse clientResponse;
    private WebClientLocation webClientLocation;

    public RequestError(WebClientLocation webClientLocation, ClientResponse clientResponse, String reason) {
      super(clientResponse.statusCode(), reason);
      this.webClientLocation = webClientLocation;
      this.clientResponse = clientResponse;
    }

    @ToString.Include
    public String getDetails() {
      //HttpStatus code = clientResponse.statusCode();
      //String msg = Nodes.yml.toString(Tuple.of(this.get);//code + getReason();
      return Nodes.yml.toString(Seq(
        Tuple.of("request", webClientLocation.url),
        Tuple.of("statusCode", clientResponse.statusCode()),
        Tuple.of("rawStatusCode", clientResponse.rawStatusCode()),
        Tuple.of("headers", clientResponse.headers().asHttpHeaders()),
        Tuple.of("body", clientResponse.bodyToMono(String.class).block(Duration.ofSeconds(1)))
      //
      ));
    }

    @Override
    public HttpHeaders getResponseHeaders() {
      return clientResponse.headers().asHttpHeaders();
    }

    @Override
    public String getMessage() {
      return NestedExceptionUtils.buildMessage(getDetails(), getCause());
    }
  }

  public Mono<String> readContentAsyncWithRetry() {
    return readContentAsyncWithRetry(Duration.ofSeconds(1), Duration.ofSeconds(1000), 3);
  }

  public Mono<String> readContentAsyncWithRetry(Duration firstBackoff, Duration maxBackoff, int retryMax) {
    RetryBackoffSpec retry = reactor.util.retry.Retry.backoff(retryMax, firstBackoff)
      .maxBackoff(maxBackoff)
      .filter(throwable -> throwable instanceof RequestError)
      .jitter(0.5);
    //return request.exchange().flatMap(x -> x.bodyToMono(String.class));
    return request.exchange().flatMap(clientResponse -> {
      if (clientResponse.statusCode().isError()) {
        return Mono.error(new RequestError(this, clientResponse, "Error"));
      } else {
        return Mono.just(clientResponse);
      }
    })
      .retryWhen(retry)
      .flatMap(x -> x.bodyToMono(String.class));
  }

  @Override
  public String readContentSync(ReadOptions options) {
    if (!options.blockingReadDuration.equals(client.readContentSyncTimeout)) {
      log.info("While reading {} readoptions {} is ignored and client config {} is used.", this.toExternalForm(),
        options.blockingReadDuration, client.readContentSyncTimeout);
    }
    return readContentAsync(options).block(client.readContentSyncTimeout);
  }

  public static class WebClientFactory implements HttpClientLocationFactory {
    private WebClient client;
    private WebClient clientWithTapSimple;
    private WebClient clientWithTapDump;
    public Builder builder;
    public Scheduler scheduler = Schedulers.boundedElastic();
    public Duration existsTimeout = Duration.ofSeconds(30);
    public Duration readContentSyncTimeout = Duration.ofSeconds(30);
    public final EnumFeature<AdvancedByteBufFormat> webclientWireTapType = new EnumFeature<>(
      "webclientWireTapType2", "One of the values SIMPLE, TEXTUAL, HEX_DUMP", AdvancedByteBufFormat.SIMPLE,
      "feature.webclient.wireTap.type",
      true, false);
    public final BooleanFeature webclientWireTap = GenericFeature.booleanFeature(
      "webclientWireTap", "", false, "feature.webclient.wireTap", true);
    public final GenericFeature<Integer> webclientMaxConnections = GenericFeature.create(
      "webclientMaxConnections", "", 500, "feature.webclient.maxConnections", true);
    public final BooleanFeature retryOnAnyError = GenericFeature.booleanFeature(
      "webclientRetryOnAnyError", "", false, "feature.webclient.retryOnAnyError", true);
    public final BooleanFeature webclientMicrometerMetrics = GenericFeature.booleanFeature(
      "webclientMicrometerMetrics", "", false, "feature.webclient.micrometerMetrics", true);

    public WebClientFactory() {
      this.builder = createWebClient(null);
      this.client = builder.build();
      this.clientWithTapSimple = createWebClient(AdvancedByteBufFormat.SIMPLE).build();
      this.clientWithTapDump = createWebClient(AdvancedByteBufFormat.HEX_DUMP).build();
    }

    public WebClientLocation get(ModifiedURI uri) {
      URL url2 = uri.toURL();
      return new WebClientLocation(url2, false, currentClient().get().uri(uri.toURI()), this);
    }

    @Override
    @SneakyThrows
    public WebClientLocation get(String url) {
      URL url2 = new URL(url);
      RequestHeadersSpec<?> req = currentClient().get().uri(url2.toURI());
      return new WebClientLocation(url2, false, req, this);
    }

    @SneakyThrows
    public WebClientLocation post(String url, MediaType mediaType, Object bodyValue) {
      log.info("post {}: {}", url, bodyValue);
      RequestHeadersSpec<?> req = currentClient()
        .post()
        .uri(url)
        .contentType(mediaType)
        .bodyValue(bodyValue)
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
      return webclientWireTap.isEnabled()
          ? (webclientWireTapType.value() == AdvancedByteBufFormat.SIMPLE ? clientWithTapSimple : clientWithTapDump)
          : client;
    }

    public Builder createWebClient(AdvancedByteBufFormat format) {
      ConnectionProvider provider = ConnectionProvider
        .builder("namek-webclient")
        .metrics(webclientMicrometerMetrics.value())
        .maxConnections(webclientMaxConnections.value())
        //.pendingAcquireMaxCount(10)
        .build();
      //HttpClient.from(TcpClient.create(provider))
      //logging - https://www.baeldung.com/spring-log-webclient-calls
      HttpClient httpClient = HttpClient
        .create(provider)
        .protocol(HttpProtocol.H2, HttpProtocol.HTTP11)
        //.responseTimeout(Duration.ofSeconds(10))
        .compress(true)
      //.wiretap("jedio.http", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL)
      //.attr(null, null)
      //.tcpConfiguration(tcpClient -> tcpClient
      //        .bootstrap(b -> BootstrapHandlers.updateLogSupport(b,
      //          new LoggingHandler(HttpClient.class, LogLevel.INFO, ByteBufFormat.SIMPLE))))
      ;
      if (format != null) {
        httpClient = httpClient.wiretap("jedio.http", LogLevel.INFO, format);
      }
      //.wiretap("reactor.netty.http.client.HttpClient",
      //  LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);

      //see NamekFeatureService.debugLogNettyClient
      //      ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("reactor.netty.http.client"))
      //        .setLevel(Level.DEBUG);

      //      DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
      //      factory.setDefaultUriVariables(Collections.singletonMap("url", "http://localhost:8080"));
      //      factory.setEncodingMode(EncodingMode.URI_COMPONENT);

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
        .filter(encodePlusSignFilter())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        //.baseUrl(url)
        //.defaultCookie("cookieKey", "cookieValue")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.USER_AGENT, "curl/8.2.1")
        //> GET /json/program/liveEvents?oneSectionResult=true HTTP/2
        //> Host: sports.tipico.com
        //> User-Agent: curl/8.2.1
        //> Accept: */*
        .defaultUriVariables(Collections.singletonMap("url", "http://localhost:8080"))
      //        .uriBuilderFactory(factory)
      //        .filter(ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
      //          // Log Headers
      //          HttpHeaders headers = clientRequest.headers();
      //          HttpMethod method = clientRequest.method();
      //          System.out.println("Request Headers: " + headers);
      //
      //          // Continue processing
      //          return Mono.just(clientRequest);
      //        }))
      //
      ;
      return client;
    }

    /** Web Client is not encoding the '+' sign, so in an already constructed URL, this must be manually encoded.
     *  Another way would be to pass the query parameters name and values, but this would require some changes in the implementation of WebClientLocation2 and in it's usages.
     *  See <a>https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/util/UriBuilder.html#queryParam(java.lang.String,java.lang.Object...)</a>*/
    private static ExchangeFilterFunction encodePlusSignFilter() {
      return (request, next) -> {
        String encodedUrl = request.url().toString().replace("+", "%2B");
        URI modifiedUri;
        try {
          modifiedUri = new URI(encodedUrl);
        } catch (URISyntaxException e) {
          throw new RuntimeException("Failed to create modified URI.", e);
        }
        return next.exchange(ClientRequest.from(request)
          .url(modifiedUri)
          .build());
      };
    }
  }
}
