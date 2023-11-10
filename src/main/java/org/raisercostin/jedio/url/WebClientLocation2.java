package org.raisercostin.jedio.url;

import static io.vavr.API.Seq;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import io.netty.handler.logging.LogLevel;
import io.vavr.Function1;
import io.vavr.Tuple;
import io.vavr.collection.Iterator;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jedio.feature.BooleanFeature;
import org.jedio.feature.EnumFeature;
import org.jedio.feature.GenericFeature;
import org.json.JSONObject;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse.EnrichedContent;
import org.raisercostin.jedio.url.impl.ModifiedURI;
import org.raisercostin.nodes.Nodes;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
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
import reactor.util.retry.Retry;

public class WebClientLocation2 extends BaseHttpLocationLike<@NonNull WebClientLocation2>
    implements ReadableFileLocation {
  public static final WebClientFactory defaultClient = new WebClientFactory();

  public static WebClientLocation2 post(String url, MediaType applicationJson, Object params) {
    return defaultClient.post(url, applicationJson, params, null);
  }

  @Deprecated
  public static WebClientLocation2 post(String url, MediaType applicationJson, Object params,
      MultiValueMap<String, String> headers) {
    return defaultClient.post(url, applicationJson, params, headers);
  }

  public static WebClientLocation2 post(String url) {
    return defaultClient.post(url);
  }

  public WebClientLocation2 header(String name, String value) {
    RequestBodySpec requestBody2 = request.header(name, value);
    return new WebClientLocation2(url, httpMethod, false, requestBody2, client);
  }

  public WebClientLocation2 contentType(MediaType contentType) {
    RequestBodySpec requestBody2 = request.contentType(contentType);
    return new WebClientLocation2(url, httpMethod, false, requestBody2, client);
  }

  public WebClientLocation2 body(Object body) {
    if (body instanceof JSONObject) {
      body = body.toString();
    }
    RequestHeadersSpec<?> request2 = request.bodyValue(body);
    return new WebClientLocation2(url, httpMethod, false, request2, client);
  }

  @Deprecated
  public static WebClientLocation2 put(String url, MediaType applicationJson, Object params,
      MultiValueMap<String, String> headers) {
    return defaultClient.put(url, applicationJson, params, headers);
  }

  public static WebClientLocation2 put(String url) {
    return defaultClient.put(url);
  }

  /**
   * Is not ideal to use get with body, but here it is
   * https://stackoverflow.com/questions/69904185/is-it-possible-to-include-a-request-body-in-a-get-request-using-spring-webclient
  */
  @Deprecated
  public static WebClientLocation2 get(String url, MediaType applicationJson, Object params,
      MultiValueMap<String, String> headers) {
    return defaultClient.get(url, applicationJson, params, headers);
  }

  public static WebClientLocation2 get(String url) {
    return defaultClient.get(url);
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  public static <S extends RequestHeadersUriSpec<S>> @NonNull WebClientLocation2 httpGet(String url,
      Function1<RequestHeadersUriSpec<S>, RequestHeadersUriSpec<S>> requestCreator) {
    URL url2 = new URL(url);
    return new WebClientLocation2(url2, HttpMethod.GET, false,
      requestCreator.apply((S) defaultClient.currentClient().get()).uri(url2.toURI()),
      defaultClient);
  }

  @SneakyThrows
  public static WebClientLocation2 httpPost(String url,
      Function1<RequestBodyUriSpec, RequestBodyUriSpec> requestCreator) {
    URL url2 = new URL(url);
    return new WebClientLocation2(url2, HttpMethod.POST, false,
      requestCreator.apply(defaultClient.currentClient().post()).uri(url2.toURI()),
      defaultClient);
  }

  @SneakyThrows
  public static WebClientLocation2 httpPut(String url,
      Function1<RequestBodyUriSpec, RequestBodyUriSpec> requestCreator) {
    URL url2 = new URL(url);
    return new WebClientLocation2(url2, HttpMethod.PUT, false,
      requestCreator.apply(defaultClient.currentClient().put()).uri(url2.toURI()),
      defaultClient);
  }

  @SneakyThrows
  public static WebClientLocation2 httpMethod(String url, HttpMethod method,
      Function1<RequestBodyUriSpec, RequestBodyUriSpec> requestCreator) {
    URL url2 = new URL(url);
    return new WebClientLocation2(url2, method, false,
      requestCreator.apply(defaultClient.currentClient().method(method)).uri(url2.toURI()),
      defaultClient);
  }

  public static WebClientLocation2 httpGet(String url) {
    return defaultClient.get(url);
  }

  public static WebClientLocation2 httpGet(ModifiedURI uri) {
    return defaultClient.get(uri);
  }

  @JsonIgnore
  public final WebClientFactory client;
  private final HttpMethod httpMethod;
  private final RequestBodySpec request;
  private final EnrichedContent enrich;
  private boolean escaped;

  private WebClientLocation2(URL url, boolean escaped, WebClientFactory client)
      throws URISyntaxException
  {
    this(url, HttpMethod.GET, escaped, client.currentClient().get().uri(url.toURI()), client);
  }

  private WebClientLocation2(URL url, HttpMethod httpMethod, boolean escaped, RequestHeadersSpec<?> request,
      WebClientFactory client)
  {
    this(url, httpMethod, escaped, (RequestBodySpec) request, client, EnrichedContent.JSON_AUGUMENTED_OR_RAW);
  }

  private WebClientLocation2(URL url, HttpMethod httpMethod, boolean escaped,
      RequestBodySpec requestBody, WebClientFactory client, EnrichedContent enrich)
  {
    super(url, escaped);
    this.escaped = escaped;
    this.client = client;
    this.httpMethod = httpMethod;
    this.request = requestBody;
    this.enrich = enrich == null ? EnrichedContent.JSON_AUGUMENTED_OR_RAW : enrich;
  }

  @Override
  @SneakyThrows
  protected WebClientLocation2 create(URL url, boolean escaped) {
    return new WebClientLocation2(url, escaped, client);
  }

  public WebClientLocation2 reconfigure(Function1<RequestHeadersSpec<?>, RequestHeadersSpec<?>> requestSupplier) {
    return new WebClientLocation2(url, httpMethod, true, requestSupplier.apply(request), client);
  }

  public WebClientLocation2 withEnrichedContent(EnrichedContent enrich) {
    return new WebClientLocation2(url, httpMethod, escaped, request, client, enrich);
  }

  @Override
  @SneakyThrows
  public InputStream unsafeInputStream() {
    //TODO remove exchange
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
    return client.block(request.exchange()
      .map(x -> x.statusCode() != HttpStatus.NOT_FOUND));
  }

  public static class RequestResponse {
    private WebClientLocation2 clientLocation;
    private ResponseEntity<String> responseEntity;

    public RequestResponse(WebClientLocation2 clientLocation, ResponseEntity<String> responseEntity) {
      this.clientLocation = clientLocation;
      this.responseEntity = responseEntity;
    }

    @Override
    public String toString() {
      return "RequestResponse %s %s".formatted(responseEntity.getStatusCode(), clientLocation.url);
    }

    public HttpStatusCode getStatusCode() {
      return responseEntity.getStatusCode();
    }

    public HttpHeaders getHeaders() {
      return responseEntity.getHeaders();
    }

    /**Sometimes you want to save with the content the full details available in the call at that moment.
     * Best non intrusive default value is JSON_AUGUMENTED_OR_RAW.
     */
    public enum EnrichedContent {
      RAW_BODY("No enrichment.", false),
      JSON_AUGUMENTED_OR_RAW("Enrichment if returned type is json object that can be enriched easily. Otherwise raw.",
          false),
      FRONTMATTER("First there is the metadata as json. Then after `---` the body.", true),
      LASTMATTER("First there is content. Then after `---` the metadata as json. Read from end till --- is found.",
          false),
      JSON_WRAPPPED(
          "If content is json it will be under a content field and have a metadata sibiling. Otherwise FRONTMATTER",
          true),
      JSON_AUGUMENTED("If content is json it will have a new metadata field. Otherwise FRONTMATTER", true);

      public String description;
      public boolean allowFrontmatter;

      EnrichedContent(String description, boolean allowFrontmatter) {
        this.description = description;
        this.allowFrontmatter = allowFrontmatter;
      }
    }

    public String getBody() {
      return getRawBody();
    }

    public String getBodyWrappedInMetadata() {
      return getBodyWithMetadata(EnrichedContent.JSON_WRAPPPED);
    }

    public String getBodyWithMetadataField() {
      return getBodyWithMetadata(EnrichedContent.JSON_AUGUMENTED);
    }

    public String getBodyWithMetadata(EnrichedContent enrich) {
      if (enrich == EnrichedContent.RAW_BODY) {
        return responseEntity.getBody();
      }
      if (enrich == EnrichedContent.LASTMATTER) {
        throw new RuntimeException("Not implemented yet!!! " + EnrichedContent.LASTMATTER);
      }
      String body = responseEntity.getBody();
      boolean isJson = responseEntity.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON);
      if (isJson && (enrich == EnrichedContent.JSON_AUGUMENTED_OR_RAW || enrich == EnrichedContent.JSON_AUGUMENTED
          || enrich == EnrichedContent.JSON_WRAPPPED)) {
        if (enrich == EnrichedContent.JSON_WRAPPPED) {
          return "{%s,\n\"content\":%s\n}".formatted(computeMetadata(), body);
        } else {
          body = body.trim();
          boolean bodyIsAnObjectBetweenCurlyBraces = body.startsWith("{") && body.endsWith("}");
          if (bodyIsAnObjectBetweenCurlyBraces) {
            String bodyWithoutParanthesis = removePrefixSuffix(body.trim(), "{", "}");
            return "{%s,\n%s\n}".formatted(computeMetadata(), bodyWithoutParanthesis);
          } else {
            if (enrich == EnrichedContent.JSON_AUGUMENTED_OR_RAW) {
              return body;
            } else {
              throw new RuntimeException(
                "Content was primary json data so cannot be %s. Try %s".formatted(enrich,
                  EnrichedContent.JSON_WRAPPPED));
            }
          }
        }
      } else {
        if (enrich == EnrichedContent.JSON_AUGUMENTED_OR_RAW) {
          return body;
        } else {
          Preconditions.checkArgument(enrich.allowFrontmatter, "%s doesn't allow frontmatter.", enrich);
          return "{%s}\n---\n%s".formatted(computeMetadata(), body);
        }
      }
    }

    private String computeMetadata() {
      String responseHeaders = Iterator.ofAll(responseEntity.getHeaders().entrySet())
        .map(entry -> "\"" + escapeJson(entry.getKey()) + "\": \"" + escapeJson(String.join(",", entry.getValue()))
            + "\"")
        .mkString("", ",\n    ", "");
      String requestHeaders = Iterator.ofAll(requestHeaders().entrySet())
        .map(entry -> "\"" + escapeJson(entry.getKey()) + "\": \"" + escapeJson(String.join(",", entry.getValue()))
            + "\"")
        .mkString("", ",\n    ", "");
      String metadata = """
          "metadata": {
            "URL": "%s",
            "Method": "%s",
            "StatusCode": "%s",
            "StatusCodeValue": %s,
            "ResponseHeaders": {
              %s
            },
            "RequestHeaders": {
              %s
            }
          }""".formatted(
        escapeJson(clientLocation.url.toExternalForm()),
        escapeJson(clientLocation.httpMethod.toString()),
        escapeJson(responseEntity.getStatusCode().toString()),
        responseEntity.getStatusCodeValue(),
        responseHeaders, requestHeaders);
      return metadata;
    }

    private HttpMethod requestMethod() {
      try {
        RequestHeadersSpec<?> request = clientLocation.request();
        HttpMethod method = (HttpMethod) FieldUtils.getField(request.getClass(), "httpMethod")
          .get(request);
        return method;
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw org.jedio.RichThrowable.nowrap(e);
      }
    }

    private HttpHeaders requestHeaders() {
      //      try {
      AtomicReference<HttpHeaders> headers = new AtomicReference<>();
      RequestHeadersSpec<?> request = clientLocation.request();
      request.headers(x -> headers.set(x));
      return headers.get();
      //        HttpHeaders headers = (HttpHeaders) FieldUtils.getField(request.getClass(), "headers")
      //          .get(request);
      //        return headers;
      //      } catch (IllegalArgumentException | IllegalAccessException e) {
      //        throw org.jedio.RichThrowable.nowrap(e);
      //      }
    }

    private static String escapeJson(String str) {
      return str.replace("\\", "\\\\")
        .replace("\"", "\\\"");
    }

    private static String removePrefixSuffix(String str, String prefix, String suffix) {
      if (str.startsWith(prefix) && str.endsWith(suffix)) {
        return str.substring(prefix.length(), str.length() - suffix.length());
      }
      return str;
    }

    public String getRawBody() {
      return responseEntity.getBody();
    }
  }

  @ToString
  @JsonAutoDetect(getterVisibility = Visibility.NONE)
  public static class RequestError extends ErrorResponseException {
    public final ResponseEntity responseEntity;
    public final WebClientLocation2 webClientLocation;

    public RequestError(WebClientLocation2 webClientLocation, ResponseEntity responseEntity, String reason) {
      super(responseEntity.getStatusCode(), ProblemDetail.forStatusAndDetail(responseEntity.getStatusCode(),
        (String) responseEntity.getBody()), null, reason, null);
      this.webClientLocation = webClientLocation;
      this.responseEntity = responseEntity;
    }

    @ToString.Include
    public String getDetails() {
      //HttpStatus code = clientResponse.statusCode();
      //String msg = Nodes.yml.toString(Tuple.of(this.get);//code + getReason();
      return Nodes.yml.toString(Seq(
        Tuple.of("request", webClientLocation.url),
        Tuple.of("statusCode", responseEntity.getStatusCode()),
        Tuple.of("rawStatusCode", responseEntity.getStatusCodeValue()),
        Tuple.of("headers", responseEntity.getHeaders()),
        Tuple.of("body", responseEntity.getBody())
      //
      ));
    }

    public HttpHeaders getResponseHeaders() {
      return responseEntity.getHeaders();
    }

    @Override
    public String getMessage() {
      return NestedExceptionUtils.buildMessage(getDetails(), getCause());
    }

    //    @Override
    //    public String getBody() {
    //      return super.getBody().getDetail();
    //    }
  }

  @Override
  public String readContentSync(Charset charset) {
    Mono<String> mono = readContentAsync();
    return client.block(mono);
  }

  public RequestHeadersSpec<?> request() {
    return request;
  }

  public RequestResponse readCompleteContentSync() {
    Mono<RequestResponse> mono = readAsync();
    return client.block(mono);
  }

  @Override
  public Mono<String> readContentAsync() {
    return readAsync().map(x -> x.getBodyWithMetadata(enrich));
  }

  public Mono<RequestResponse> readAsync() {
    if (client.retryOnAnyError.isEnabled()) {
      return readContentAsyncWithRetry();
    }
    return readContentAsyncWithRetry(null, null, 0);
  }

  public Mono<RequestResponse> readContentAsyncWithRetry() {
    return readContentAsyncWithRetry(Duration.ofSeconds(1), Duration.ofSeconds(1000), 3);
  }

  public Mono<RequestResponse> readContentAsyncWithRetry(Duration firstBackoff, Duration maxBackoff, int retryMax) {
    return readCompleteContentAsyncWithRetry(firstBackoff, maxBackoff, retryMax);
  }

  @SuppressWarnings("null")
  private Mono<RequestResponse> readCompleteContentAsyncWithRetry(Duration firstBackoff, Duration maxBackoff,
      int retryMax) {
    return readContentInfoWithRetry(firstBackoff, maxBackoff, retryMax)
      .flatMap(clientResponse -> {
        if (clientResponse.getStatusCode().isError()) {
          return Mono.error(new RequestError(this, clientResponse, "Error"));
        } else {
          return Mono.just(new RequestResponse(this, clientResponse));
        }
      })
      .doOnNext(content -> {
        log.info("get {} done. size {}", url, content.getBody() == null ? null : content.getBody().length());
      });
  }

  private Mono<ResponseEntity<String>> readContentInfoWithRetry(Duration firstBackoff, Duration maxBackoff,
      int retryMax) {
    Retry retry = firstBackoff == null || maxBackoff == null || retryMax == 0
        ? reactor.util.retry.Retry.max(0)
        : reactor.util.retry.Retry.backoff(retryMax, firstBackoff)
          .maxBackoff(maxBackoff)
          .filter(throwable -> throwable instanceof RequestError)
          .jitter(0.5);
    RequestHeadersSpec<?> request2 = request();
    //TODO remove dependency on webclient. too comvoluted
    return request2.exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
      .retryWhen(retry);
  }

  public static class WebClientFactory implements HttpClientLocationFactory {
    private WebClient client;
    private WebClient clientWithTapSimple;
    private WebClient clientWithTapDump;
    public Builder builder;
    //To allow blocking in a special thread pool
    public ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
    public Scheduler blockingScheduler = Schedulers.fromExecutor(executorService);
    public Scheduler scheduler = Schedulers.boundedElastic();

    public Duration existsTimeout = Duration.ofSeconds(30);
    public Duration readContentSyncTimeout = Duration.ofSeconds(90);
    public static final EnumFeature<AdvancedByteBufFormat> webclientWireTapType = new EnumFeature<>(
      "webclientWireTapType2", "One of the values SIMPLE, TEXTUAL, HEX_DUMP", AdvancedByteBufFormat.SIMPLE,
      "feature.webclient.wireTap.type",
      true, false);
    public static final BooleanFeature webclientWireTap = GenericFeature.booleanFeature(
      "webclientWireTap2", "", false, "feature.webclient.wireTap", true);
    public static final GenericFeature<Integer> webclientMaxConnections = GenericFeature.create(
      "webclientMaxConnections2", "", 500, "feature.webclient.maxConnections", true);
    public static final BooleanFeature retryOnAnyError = GenericFeature.booleanFeature(
      "webclientRetryOnAnyError2", "", false, "feature.webclient.retryOnAnyError", true);

    public WebClientFactory() {
      this.builder = createWebClient(null, webclientMaxConnections);
      this.client = builder.build();
      this.clientWithTapSimple = createWebClient(AdvancedByteBufFormat.SIMPLE, webclientMaxConnections).build();
      this.clientWithTapDump = createWebClient(AdvancedByteBufFormat.HEX_DUMP, webclientMaxConnections).build();
    }

    public WebClientLocation2 get(ModifiedURI uri) {
      URL url2 = uri.toURL();
      return new WebClientLocation2(url2, HttpMethod.GET, false, currentClient().get().uri(uri.toURI()), this);
    }

    //    @Override
    //    @SneakyThrows
    //    public WebClientLocation2 get(String url) {
    //      return get(url, null, null, null);
    //    }

    /**
     * Is not ideal to use get with body, but here it is
     * https://stackoverflow.com/questions/69904185/is-it-possible-to-include-a-request-body-in-a-get-request-using-spring-webclient
    */
    @Deprecated
    @SneakyThrows
    public WebClientLocation2 get(String url, MediaType mediaType, Object bodyValue,
        MultiValueMap<String, String> headers) {
      log.info("get {}: {}", url, bodyValue);
      URL url2 = new URL(url);
      RequestHeadersSpec<?> req = bodyValue == null ? currentClient()
        .method(HttpMethod.GET)
        .uri(url2.toURI())
          : currentClient()
            .method(HttpMethod.GET)
            .uri(url2.toURI())
            .contentType(mediaType)
            .bodyValue(bodyValue);

      if (headers != null) {
        req = req.headers(headears2 -> headears2.addAll(headers));
      }

      return new WebClientLocation2(url2, HttpMethod.GET, false, req, this);
    }

    @Override
    @SneakyThrows
    public WebClientLocation2 get(String url) {
      log.info("get {}", url);
      return new WebClientLocation2(new URL(url), HttpMethod.GET, false,
        currentClient().method(HttpMethod.GET).uri(url), this);
    }

    @SneakyThrows
    @Deprecated
    public WebClientLocation2 post(String url, MediaType mediaType, Object bodyValue,
        MultiValueMap<String, String> headers) {
      log.info("post {}: {}", url, bodyValue);
      return createWebClientLocation(currentClient().post(), url, HttpMethod.POST, mediaType, bodyValue, headers);
    }

    @SneakyThrows
    public WebClientLocation2 post(String url) {
      log.info("post {}", url);
      return new WebClientLocation2(new URL(url), HttpMethod.POST, false, currentClient().post().uri(url), this);
    }

    @SneakyThrows
    @Deprecated
    public WebClientLocation2 put(String url, MediaType mediaType, Object bodyValue,
        MultiValueMap<String, String> headers) {
      log.info("put {}: {}", url, bodyValue);
      return createWebClientLocation(currentClient().put(), url, HttpMethod.PUT, mediaType, bodyValue, headers);
    }

    @SneakyThrows
    public WebClientLocation2 put(String url) {
      log.info("put {}", url);
      return new WebClientLocation2(new URL(url), HttpMethod.PUT, false, currentClient().put().uri(url), this);
    }

    @SneakyThrows
    private WebClientLocation2 createWebClientLocation(RequestBodyUriSpec requestBodyUriSpec, String url,
        HttpMethod httpMethod, MediaType mediaType, Object bodyValue, MultiValueMap<String, String> headers) {
      RequestHeadersSpec<?> req = requestBodyUriSpec.uri(url)
        .contentType(mediaType)
        .bodyValue(bodyValue);

      if (headers != null) {
        req = req.headers(headears2 -> headears2.addAll(headers));
      }

      return new WebClientLocation2(new URL(url), httpMethod, false, req, this);
    }

    public final WebClient currentClient() {
      return webclientWireTap.isEnabled()
          ? (webclientWireTapType.value() == AdvancedByteBufFormat.SIMPLE ? clientWithTapSimple : clientWithTapDump)
          : client;
    }

    @SuppressWarnings("deprecation")
    public static Builder createWebClient(AdvancedByteBufFormat format,
        GenericFeature<Integer> webclientMaxConnections) {
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

      //see RevobetFeatureService.debugLogNettyClient
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

    /**try not to block current thread. If is really needed the current thread should be one similar to this blockingScheduler.*/
    @Deprecated
    public <T> T block(Mono<T> mono) {
      return mono.publishOn(blockingScheduler)
        .block(readContentSyncTimeout);
    }
  }
}
