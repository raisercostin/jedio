package org.raisercostin.jedio.url;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.vavr.Function0;
import io.vavr.Lazy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.Value;
import lombok.With;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.pool.PoolStats;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import reactor.core.publisher.Mono;

@Data
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
/**
 * Default client configuration that takes care of all http call details:
 * - [x] connection pool: `PoolingHttpClientConnectionManager`
 * - [x] thread pool: `ExecutorService result = Executors.newFixedThreadPool`
 */
@Slf4j
public class JedioHttpClient {
  private static final String SOCKET_REMOTE_ADDRESS = "socket-remote-address";
  private static final String SOCKET_LOCAL_ADDRESS = "socket-local-address";
  private static final int MILLIS = 1000;
  private static final int timeoutBase = 20;

  @Value
  @With
  @Builder
  @NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
  @AllArgsConstructor
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
  public static class JedioHttpConfig {
    public static JedioHttpConfig create() {
      return new JedioHttpConfig();
    }

    String name;
    @Builder.Default
    /**Number of threads in the thread pool. Caps the available connections since the connection one creates one per thread.*/
    int threads = 100;
    @Builder.Default
    int maxTotal = 1000;
    @Builder.Default
    int maxPerRoute = 1000;
    @Builder.Default
    int soTimeoutInMilllis = timeoutBase * MILLIS;
    /**Defines period of inactivity in milliseconds after which persistent connections mustbe re-validated prior to being leased to the consumer.
     * Non-positive value passedto this method disables connection validation. This check helps detect connectionsthat have become stale
     * (half-closed) while kept inactive in the pool.*/
    @Builder.Default
    int validateAfterInactivityInMilllis = timeoutBase * MILLIS;

    //    public HttpClientBuilder toHttpClientBuilder() {
    //      throw new RuntimeException("Not implemented yet!!!");
    //    }

    public JedioHttpClient createClient() {
      return new JedioHttpClient(this);
    }
  }

  public static JedioHttpClient createHighPerfHttpClient() {
    return JedioHttpConfig.create().withName("jedio1").createClient();
  }

  public static JedioHttpClient from(JedioHttpConfig config) {
    return new JedioHttpClient(config);
  }

  public final JedioHttpConfig config;
  private final PoolingHttpClientConnectionManager connManager;
  private final HttpClientBuilder builder;
  private final Lazy<CloseableHttpClient> client;
  private final ExecutorService executorService;

  //  private static final int timeout = 20;
  //  // private static final int hardTimeout = 5 * timeout; // seconds
  //  private static final int ROUTES = 1000;
  //  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BaseHttpLocationLike.class);
  //  private static final Scheduler scheduler = Schedulers.newParallel("http-hard-abort", 100);
  //  private static final boolean enableHardAbort = true;

  public JedioHttpClient(JedioHttpConfig config) {
    this.config = config;
    this.connManager = createConnectionManager();
    this.builder = createHttpBuilder();
    this.executorService = createExecutor();
    this.client = Lazy.of(() -> builder.build());
  }

  public CloseableHttpClient client() {
    return client.get();
  }

  public PoolStats getStatus() {
    return connManager.getTotalStats();
  }

  private ExecutorService createExecutor() {
    log.info("Create executor ... {} threads", this.config.threads);
    ThreadFactory builder = new ThreadFactoryBuilder().setNameFormat(this.config.name + "-%s").build();
    // .setUncaughtExceptionHandler(fiberExceptionHandler)
    ExecutorService result = Executors.newFixedThreadPool(this.config.threads, builder);
    log.info("Create executor done.");
    return result;
  }

  private PoolingHttpClientConnectionManager createConnectionManager() {
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
      createSocketFactoryRegistry(), SystemDefaultDnsResolver.INSTANCE);
    // HttpHost host = new HttpHost("hostname", 80);
    // HttpRoute route = new HttpRoute(host);
    // connManager.setSocketConfig(route.getTargetHost(), SocketConfig.custom().setSoTimeout(5000).build());
    // Set the maximum number of total open connections.
    connManager.setMaxTotal(config.maxTotal);
    connManager.closeExpiredConnections();
    connManager.closeIdleConnections(1000, TimeUnit.SECONDS);
    connManager.setValidateAfterInactivity(10 * MILLIS);
    // Set the maximum number of concurrent connections per route, which is 2 by default.
    connManager.setDefaultMaxPerRoute(config.maxPerRoute);
    ConnectionConfig defaultConnectionConfig = ConnectionConfig.custom().build();
    // Set the total number of concurrent connections to a specific route, which is 2 by default.
    // connManager.setMaxPerRoute(route, 5);
    connManager.setDefaultConnectionConfig(defaultConnectionConfig);
    SocketConfig defaultSocketConfig = SocketConfig.custom().setSoTimeout(config.soTimeoutInMilllis).build();
    connManager.setDefaultSocketConfig(defaultSocketConfig);
    connManager.setValidateAfterInactivity(config.validateAfterInactivityInMilllis);
    return connManager;
  }

  private Registry<ConnectionSocketFactory> createSocketFactoryRegistry() {
    return RegistryBuilder.<ConnectionSocketFactory>create()
      .register("http", createPlainConnectionSocketFactory())
      .register("https", createSSLConnectionSocketFactory())
      .build();
  }

  /**
   * See - https://www.baeldung.com/httpclient-connection-management - https://www.baeldung.com/httpclient-timeout
   */
  @SneakyThrows
  private HttpClientBuilder createHttpBuilder() {
    ConnectionKeepAliveStrategy keepAliveStrategy = keepAliveStrategy();
    RequestConfig requestConfig = createRequestConfig();
    HttpClientBuilder builder = HttpClients.custom()
      .setKeepAliveStrategy(keepAliveStrategy)
      .setConnectionManager(connManager)
      .setRetryHandler(retryHandler())
      .setConnectionTimeToLive(60, TimeUnit.SECONDS)
      .setDefaultRequestConfig(requestConfig)
      .disableRedirectHandling()
    // added already in connManager
    // .setSSLSocketFactory(createSSLSocketFactory())
    ;

    // HttpParams params = new BasicHttpParams();
    // HttpConnectionParams.setConnectionTimeout(params, 5000);
    // HttpConnectionParams.setSoTimeout(params, 20000);
    // HttpClient httpClient = new DefaultHttpClient(params);
    //
    return builder;
  }

  private RequestConfig createRequestConfig() {
    return RequestConfig.custom()
      // .setStaleConnectionCheckEnabled(true) - used setValidateAfterInactivity
      .setConnectTimeout(timeoutBase * MILLIS)
      .setConnectionRequestTimeout(timeoutBase * MILLIS)
      .setSocketTimeout(timeoutBase * MILLIS)
      .setContentCompressionEnabled(true)
      .build();
  }

  @Data
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @ToString
  public static class SerializableInetSocketAddress {
    public static SerializableInetSocketAddress from(InetSocketAddress address) {
      return address == null ? null : new SerializableInetSocketAddress(address);
    }

    @JsonIgnore
    private InetSocketAddress address;

    @JsonProperty
    public String hostName() {
      return address.getHostName();
    }

    @JsonProperty
    public String hostString() {
      return address.getHostString();
    }

    @JsonProperty
    public int hostPort() {
      return address.getPort();
    }

    @JsonProperty
    public boolean isUnresolved() {
      return address.isUnresolved();
    }

    @JsonProperty
    public String hostAddress() {
      return address.getAddress().getHostAddress();
    }

    @JsonProperty
    public String hostName2() {
      return address.getAddress().getHostName();
    }
  }

  private static ConnectionSocketFactory createPlainConnectionSocketFactory() {
    // return PlainConnectionSocketFactory.getSocketFactory();
    return new PlainConnectionSocketFactory()
      {
        @Override
        public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress,
            InetSocketAddress localAddress, HttpContext context) throws IOException {
          context.setAttribute(SOCKET_LOCAL_ADDRESS, SerializableInetSocketAddress.from(localAddress));
          context.setAttribute(SOCKET_REMOTE_ADDRESS, SerializableInetSocketAddress.from(remoteAddress));
          return super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
        }
      };
  }

  @SneakyThrows
  private static SSLConnectionSocketFactory createSSLConnectionSocketFactory() {
    SSLContextBuilder builder = new SSLContextBuilder();
    builder.loadTrustMaterial(null, (chain, authType) -> true);
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
      (String hostname, SSLSession session) -> {
        log.debug("checking hostname {}", hostname);
        return true;
      })
      {
        @Override
        public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress,
            InetSocketAddress localAddress, HttpContext context) throws IOException {
          context.setAttribute(SOCKET_LOCAL_ADDRESS, SerializableInetSocketAddress.from(localAddress));
          context.setAttribute(SOCKET_REMOTE_ADDRESS, SerializableInetSocketAddress.from(remoteAddress));
          return super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
        }
      };
    return sslsf;
  }

  private static ConnectionKeepAliveStrategy keepAliveStrategy() {
    return (response, context) -> {
      HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
      while (it.hasNext()) {
        HeaderElement he = it.nextElement();
        String param = he.getName();
        String value = he.getValue();
        if (value != null && param.equalsIgnoreCase("timeout")) {
          return Long.parseLong(value) * MILLIS;
        }
      }
      return timeoutBase * MILLIS;
    };
  }

  private static HttpRequestRetryHandler retryHandler() {
    return (exception, executionCount, context) -> {
      boolean retryable = checkRetriable(exception, executionCount, context);
      log.warn("error. try again request: {}. {} Enable debug to see fullstacktrace.", retryable,
        exception.getMessage(), exception);
      // log.debug("error. try again request: {}. Fullstacktrace.", retryable, exception.getMessage(), exception);
      return retryable;
    };
  }

  private static boolean checkRetriable(IOException exception, int executionCount, HttpContext context) {
    if (executionCount >= 5) {
      // Do not retry if over max retry count
      return false;
    }
    if (exception instanceof InterruptedIOException) {
      // Timeout
      return false;
    }
    if (exception instanceof UnknownHostException) {
      // Unknown host
      return false;
    }
    if (exception instanceof SSLException) {
      // SSL handshake exception
      return false;
    }

    HttpClientContext clientContext = HttpClientContext.adapt(context);
    HttpRequest request = clientContext.getRequest();
    boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
    if (idempotent) {
      // Retry if the request is considered idempotent
      return true;
    }
    return false;
  }

  @Deprecated // use createHighPerfHttpClient
  public static HttpClientBuilder createHighPerfHttpClientOld(Consumer<PoolingHttpClientConnectionManager> manager) {
    ConnectionKeepAliveStrategy myStrategy = (response, context) -> {
      HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
      while (it.hasNext()) {
        HeaderElement he = it.nextElement();
        String param = he.getName();
        String value = he.getValue();
        if (value != null && param.equalsIgnoreCase("timeout")) {
          return Long.parseLong(value) * MILLIS;
        }
      }
      return 5 * 1000;
    };
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
    // HttpHost host = new HttpHost("hostname", 80);
    // HttpRoute route = new HttpRoute(host);
    // connManager.setSocketConfig(route.getTargetHost(), SocketConfig.custom().setSoTimeout(5000).build());
    // Set the maximum number of total open connections.
    connManager.setMaxTotal(1000);
    // Set the maximum number of concurrent connections per route, which is 2 by default.
    connManager.setDefaultMaxPerRoute(1000);
    // Set the total number of concurrent connections to a specific route, which is 2 by default.
    // connManager.setMaxPerRoute(route, 5);
    manager.accept(connManager);
    HttpClientBuilder builder = HttpClients.custom().setKeepAliveStrategy(myStrategy).setConnectionManager(connManager);
    return builder;
  }

  public <T> Mono<T> execute(Function0<T> supplier) {
    //
    // return Mono.fromCallable(() -> {
    // fiber.start();
    // return fiber.get();
    // });
    CompletableFuture<T> yourCompletableFuture = new CompletableFuture<>();
    // Fiber<String> fiber = new Fiber<String>(fiberForkJoinScheduler, () -> {throw new
    // RuntimeException("yourCompletableFuture.complete(readContent())");});
    // Thread.activeCount()
    executorService.execute(
      // Thread fiber = new Thread(//fiberForkJoinScheduler,
      () -> {
        try {
          // throw new RuntimeException("hahahah");
          yourCompletableFuture.complete(supplier.apply());
        } catch (Exception e) {
          if (!yourCompletableFuture.completeExceptionally(e)) {
            // throw e;
            log.error("Error in thread", e);
          }
        }
      });
    // TODO fiber.setUncaughtExceptionHandler(fiberExceptionHandler);
    // fiber.start();
    return Mono.fromFuture(yourCompletableFuture);
  }
}
