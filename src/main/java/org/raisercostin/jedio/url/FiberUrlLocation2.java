package org.raisercostin.jedio.url;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLException;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberForkJoinScheduler;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.httpclient.FiberHttpClient;
import co.paralleluniverse.strands.Strand.UncaughtExceptionHandler;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.vavr.collection.Iterator;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.execchain.RequestAbortedException;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.nio.reactor.IOReactor;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jedio.Audit;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.nodes.Nodes;
import reactor.core.publisher.Mono;

@Data
// @Getter(lombok.AccessLevel.NONE)
// @Setter(lombok.AccessLevel.NONE)
@ToString
@Slf4j
public class FiberUrlLocation2 extends BaseHttpLocationLike<@NonNull FiberUrlLocation2>
    implements ReadableFileLocation {
  private static final int retries = 5;

  // 100, 10000, 100 (worked great locally with only 750MB of memory)
  private static final int ROUTES = 100;
  private static final int MILLIS = 10000;
  private static final int FIBER_PARALLELISM = 100;

  public static class FiberUrlLocationFactory {
    public final CloseableHttpClient client;

    public FiberUrlLocation2 url(String url) {
      return new FiberUrlLocation2(url, false, false, client);
    }

    public FiberUrlLocationFactory() {
      client = createStandard6();
    }

    private CloseableHttpClient createStandard6() {
      IOReactor ioreactor = null;
      HttpRequestRetryHandler retryHandler = retryHandler();
      return new FiberHttpClient(clientBuilder(32).build(), retryHandler, ioreactor);
    }

    @SneakyThrows
    public static HttpAsyncClientBuilder clientBuilder(int ioThreadCount) {
      int timeoutInMilis = 1000;
      /**
       * See - https://www.baeldung.com/httpclient-connection-management -
       * https://www.baeldung.com/httpclient-timeout
       */

      // HttpHost host = new HttpHost("hostname", 80);
      // HttpRoute route = new HttpRoute(host);
      // connManager.setSocketConfig(route.getTargetHost(), SocketConfig.custom().setSoTimeout(5000).build());
      // Set the maximum number of total open connections.
      // connManager.setMaxTotal(ROUTES);
      // Set the maximum number of concurrent connections per route, which is 2 by default.
      // connManager.setDefaultMaxPerRoute(ROUTES);
      ConnectionConfig.Builder defaultConnectionConfig = ConnectionConfig.custom();
      // Set the total number of concurrent connections to a specific route, which is 2 by default.
      // connManager.setMaxPerRoute(route, 5);
      // connManager.setDefaultConnectionConfig(defaultConnectionConfig);
      // SocketConfig.Builder defaultSocketConfig = SocketConfig.custom().setSoTimeout(soTimeoutInMilllis);
      // connManager.setDefaultSocketConfig(defaultSocketConfig);
      // TODO to use this connManager.setValidateAfterInactivity(timeout * MILLIS);
      // manager.accept(connManager);
      RequestConfig.Builder requestConfig = RequestConfig.custom()
        // .setStaleConnectionCheckEnabled(true) - used setValidateAfterInactivity
        // Determines whether stale connection check is to be used. The stale
        // connection check can cause up to 30 millisecond overhead per request and
        // should be used only when appropriate. For performance critical
        // operations this check should be disabled.
        .setStaleConnectionCheckEnabled(true)
        // Determines the timeout in milliseconds until a connection is established.
        // raisercostin: get fast the connection otherwise timeout, what about in warmup phase when ssl is
        // also done?
        .setConnectTimeout(3 * timeoutInMilis)// timeoutInMilis
        // https://github.com/elastic/elasticsearch/issues/24069
        // This configuration property validates if the Request is stale; for some reason it's also applied
        // to
        // instances of org.apache.http.nio.pool.LeaseRequest
        // Returns the timeout in milliseconds used when requesting a connection from the connection
        // manager.
        .setConnectionRequestTimeout(0)// timeout * MILLIS)
        // Defines the socket timeout ({@code SO_TIMEOUT}) in milliseconds,
        // which is the timeout for waiting for data or, put differently,
        // a maximum period inactivity between two consecutive data packets).
        .setSocketTimeout(0)// timeoutInMilis
        .setContentCompressionEnabled(true)
        .setCookieSpec(CookieSpecs.IGNORE_COOKIES); // warkaround for
      // using cache/proxy
      // instead of real
      // site

      // session like config - we don't want timeouts on sessions
      IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
        // Determines time interval in milliseconds at which the I/O reactor wakes up to check for timed out
        // sessions and session requests.
        .setSelectInterval(timeoutInMilis / 10)// not 0, default is 1000
        // Determines the default socket timeout value for non-blocking I/O operations.
        .setSoTimeout(0)// default is 0
        // Determines the default connect timeout value for non-blocking connection requests.
        .setConnectTimeout(0)// default is 0
        // Determines the number of I/O dispatch threads to be used by the I/O reactor. Default: 2
        .setIoThreadCount(ioThreadCount)
        .build();

      // ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
      // NHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(ioReactor);

      HttpAsyncClientBuilder clientBuilder = HttpAsyncClientBuilder.create()
        .setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).build())
        .setDefaultIOReactorConfig(ioReactorConfig)
        .setMaxConnPerRoute(ROUTES)
        .setMaxConnTotal(ROUTES)
        // browsers use a value around 120s. We always need the connections so: 0.
        .setKeepAliveStrategy(createConnectionKeepAliveStrategy(0))
        // .setConnectionReuseStrategy(reuseStrategy) - already configured see
        // org.apache.http.impl.nio.client.AbstractClientExchangeHandler.manageConnectionPersistence()
        .setDefaultRequestConfig(requestConfig.build())
        .setSSLHostnameVerifier(new NoopHostnameVerifier())
        // .setSSLContext(createSslContextBuilder().build())
        .setConnectionReuseStrategy(new DefaultConnectionReuseStrategy()
          {
            @SuppressWarnings("unused") // used by json persistence
            public String description = "DefaultConnectionReuseStrategy";
          });
      // .setConnectionManager(connManager) - overrides setDefaultIOReactorConfig
      // .setDefaultConnectionConfig(defaultConnectionConfig.build())
      // .setRetryHandler(retryHandler())
      // .setConnectionTimeToLive(60, TimeUnit.SECONDS)
      // .setDefaultRequestConfig(requestConfig.build())
      // .setDefaultIOReactorConfig(ioReactorConfig);

      // HttpParams params = new BasicHttpParams();
      // HttpConnectionParams.setConnectionTimeout(params, 5000);
      // HttpConnectionParams.setSoTimeout(params, 20000);
      // HttpClient httpClient = new DefaultHttpClient(params);
      //
      // return HttpAsyncClients.custom().setMaxConnPerRoute(1000).setMaxConnTotal(1000)
      // .setKeepAliveStrategy(createConnectionKeepAliveStrategy());
      log.info("Using the following\nFibered Apache HttpAsyncClient Settings:\n{}",
        Nodes.json.toString(clientBuilder));
      return clientBuilder;
    }

    @SneakyThrows
    private static SSLContextBuilder createSslContextBuilder() {
      SSLContextBuilder builder = new SSLContextBuilder();
      builder.loadTrustMaterial(null, (chain, authType) -> true);
      return builder;
      // SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), new
      // NoopHostnameVerifier());
    }

    //
    // private static final int hardTimeout = 5 * timeout; // seconds
    // private static final Scheduler scheduler = Schedulers.newParallel("http-hard-abort", 100);
    // private static final boolean enableHardAbort = true;
    // private static final int ioThreadCount = 2;
    //
    // public static CloseableHttpAsyncClient createHighPerfHttpClient() {
    // return createHighPerfHttpClient(mgr -> {
    // }, ioThreadCount);
    // }
    //
    // public static CloseableHttpAsyncClient createHighPerfHttpClient(Consumer<NHttpClientConnectionManager>
    // manager,
    // int ioThreadCount) {
    // HttpAsyncClientBuilder client = new FiberConfig(ioThreadCount).client;
    // return client.build();
    // }

    private static HttpRequestRetryHandler retryHandler() {
      return (exception, executionCount, context) -> {
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
        if (context == null) {
          return false;
        }
        HttpClientContext clientContext = HttpClientContext.adapt(context);
        HttpRequest request = clientContext.getRequest();
        boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
        if (idempotent) {
          // Log only on retry
          if (exception.getMessage().startsWith("Timeout connecting to ")) {
            log.debug("request retrry #" + executionCount + " " + exception.getMessage(), exception);
            log.info("request retry #" + executionCount + " " + exception.getMessage()
                + ". Enable debug for full exception.");
          } else {
            log.warn("request retry #" + executionCount + " " + exception.getMessage(), exception);
          }
          // Retry if the request is considered idempotent
          return true;
        }
        return false;
      };
    }

    private static ConnectionKeepAliveStrategy createConnectionKeepAliveStrategy(
        int defaultKeepAliveDurationInMillis) {
      ConnectionKeepAliveStrategy keepAliveStrategy = new ConnectionKeepAliveStrategy()
        {
          @SuppressWarnings("unused") // used by json persistence
          public String description = "Look inside header " + HTTP.CONN_KEEP_ALIVE
              + " for a parameter timeout in ms otherwise use default value: "
              + defaultKeepAliveDurationInMillis + "ms";

          @Override
          public long getKeepAliveDuration(org.apache.http.HttpResponse response, HttpContext context) {
            HeaderElementIterator it = new BasicHeaderElementIterator(
              response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
              HeaderElement he = it.nextElement();
              String param = he.getName();
              String value = he.getValue();
              if (value != null && param.equalsIgnoreCase("timeout")) {
                log.info("Keep-Alive.timeout=" + value);
                try {
                  return Long.parseLong(value) * MILLIS;
                } catch (final NumberFormatException ignore) {
                  log.debug("ignored returned value", ignore);
                }
              }
            }
            // log.info("Keep-Alive.timeout.default=" + defaultKeepAliveDurationInMillis, new
            // RuntimeException());
            return defaultKeepAliveDurationInMillis;
          }
        };
      return keepAliveStrategy;
    }
  }

  public static final FiberUrlLocationFactory defaultFactory = new FiberUrlLocationFactory();
  // private static final FiberForkJoinScheduler scheduler = new FiberForkJoinScheduler("fiber-url-location", 4);
  //
  // TODO to investigate FiberForkJoinScheduler vs FiverExecutorScheduler
  // TODO investigate parallelism
  private static FiberScheduler fiberForkJoinScheduler = new FiberForkJoinScheduler("fiber-url", FIBER_PARALLELISM);

  private static final UncaughtExceptionHandler fiberExceptionHandler = (s, e) -> {
    // System.err.print("Exception in Fiber \"" + s.getName() + "\" ");
    if (e instanceof NullPointerException || e instanceof ClassCastException
        || Exceptions.unwrap(e) instanceof NullPointerException
        || Exceptions.unwrap(e) instanceof ClassCastException) {
      Audit.warn(e,
        "Revobet-StrangeException in Fiber %s. If this exception looks strange, perhaps you've forgotten to instrument a blocking method. Run your program with -Dco.paralleluniverse.fibers.verifyInstrumentation to catch the culprit!",
        s);
    } else {
      Audit.warn(e, "Revobet-Exception in Fiber %s.", s);
      // System.err.println(e);
      // Strand.printStackTrace(threadToFiberStack(e.getStackTrace()), System.err);
      // Fiber.currentFiber().checkInstrumentation(ExtendedStackTrace.of(e), true);
      // Fiber.getDefaultUncaughtExceptionHandler().uncaughtException(fiber, e);
    }
  };

  private boolean useCircuitBreaker = false;
  public final CloseableHttpClient client;

  public FiberUrlLocation2(String url, boolean escaped, boolean useCircuitBreaker, CloseableHttpClient client) {
    super(url, escaped);
    this.useCircuitBreaker = useCircuitBreaker;
    this.client = client;
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    return this;
  }
  //
  //  @Override
  //  public UrlLocation create(String path) {
  //    return new UrlLocation(path);
  //  }

  @Override
  public Mono<String> readContentAsync(Charset charset) {
    //
    // return Mono.fromCallable(() -> {
    // fiber.start();
    // return fiber.get();
    // });
    CompletableFuture<String> yourCompletableFuture = new CompletableFuture<>();
    // Fiber<String> fiber = new Fiber<String>(fiberForkJoinScheduler, () -> {throw new
    // RuntimeException("yourCompletableFuture.complete(readContent())");});
    Fiber<String> fiber = new Fiber<>(fiberForkJoinScheduler, () -> {
      try {
        // throw new RuntimeException("hahahah");
        yourCompletableFuture.complete(readContentSync(charset));
      } catch (Exception e) {
        if (!yourCompletableFuture.completeExceptionally(e)) {
          throw e;
        }
      }
    });
    fiber.setUncaughtExceptionHandler(fiberExceptionHandler);
    fiber.start();
    return Mono.fromFuture(yourCompletableFuture);
    //
    // log.info("readContentAsync {}", this.url);
    // final Channel<String> publisherChannel = Channels.newChannel(1, OverflowPolicy.BLOCK);
    // final Publisher<String> publisher = ReactiveStreams.toPublisher(publisherChannel);
    //
    // new Fiber(fiberForkJoinScheduler, new SuspendableRunnable() {
    // @Override
    // public void run() throws SuspendExecution, InterruptedException {
    // publisherChannel.send(readContent());
    // publisherChannel.close();
    // }
    // }).start();
    //
    // Fiber<String> fiber = scheduler.newFiber(() -> {
    // publisherChannel.send(readContent());
    // publisherChannel.close();
    // return "";
    // }).start();
    // return Mono.from(publisher);
  }

  //
  // @Suspendable
  // public String readContentAsync3() {
  // try {
  // HttpGet get1 = new HttpGet(url);
  // return client.execute(get1, response -> {
  // log.info("response headers {}", Iterator.ofAll(response.headerIterator()).mkString(","));
  // return "";
  // });
  // } catch (IOException e) {
  // throw org.jedio.RichThrowable.nowrap(e);
  // }
  // }
  private static final AtomicLong counter = new AtomicLong(0);
  private static final CircuitBreaker circuitBreaker = createCircuitBreaker();

  private static CircuitBreaker createCircuitBreaker() {
    // CircuitBreakerConfig config =
    // CircuitBreakerConfig.custom().failureRateThreshold(50).ringBufferSizeInClosedState(100).build();
    // CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
    // return registry.circuitBreaker("fiberUrlLocation");
    return CircuitBreaker.ofDefaults("fiberUrlLocation");
  }

  @Override
  @Suspendable
  public String readContentSync(Charset charset) {
    if (useCircuitBreaker) {
      return readContentWithCircuitBreaker(charset);
    } else {
      return readContentSuspendableDirect(charset);
      // return HttpUtils.getFromURL(url);
    }
  }

  @Suspendable
  public String readContentWithCircuitBreaker(Charset charset) {
    circuitBreaker.acquirePermission();
    long start = System.nanoTime();
    try {
      String returnValue = readContentSuspendableDirect(charset);
      long durationInNanos = System.nanoTime() - start;
      circuitBreaker.onSuccess(durationInNanos, TimeUnit.NANOSECONDS);
      return returnValue;
    } catch (Exception exception) {
      // Do not handle java.lang.Error
      long durationInNanos = System.nanoTime() - start;
      circuitBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS, exception);
      throw exception;
    }
    // try {
    // return circuitBreaker.decorateCallable(()->readContentSuspendable2()).call();
    // } catch (Exception e) {
    // throw org.jedio.RichThrowable.nowrap(e);
    // }
  }

  @Suspendable
  private String readContentSuspendableDirect(Charset charset) {
    long count = counter.incrementAndGet();
    log.debug("fiber {}> readContent start {}", count, url);
    HttpGet get1 = new HttpGet(toExternalForm());
    // return Mono.fromCallable(() -> {
    //// if (enableHardAbort) {
    //// scheduler.schedule(() -> {
    //// if (get1 != null) {
    //// get1.abort();
    //// }
    //// }, hardTimeout, TimeUnit.SECONDS);
    //// }
    //
    CloseableHttpResponse lastResponse = null;
    Throwable ignoredExceptionForRetry = null;
    for (int attempt = 1; attempt <= retries; attempt++) {
      try (CloseableHttpResponse response = client.execute(get1)) {
        int code = response.getStatusLine().getStatusCode();
        // String reason = response.getStatusLine().getReasonPhrase();
        if (log.isTraceEnabled()) {
          log.trace("response headers {} => {}", url,
            Iterator.ofAll(response.headerIterator()).mkString(", "));
        }
        if (code == 200) {
          final InputStream content = response.getEntity().getContent();
          final String res = IOUtils.toString(content);
          log.debug("fiber {}> readContent done  {}", count, url);
          content.close();
          return res;
        } else if (code == 502 || code == 520) {
          log.warn("Attempt {} to {} failed: {}", attempt, url, response);
        } else if (code == 403 || code == 401) {
          throw new ForbiddenCallException("Unauthorized/Forbidden call " + this, response, null);
        } else {
          throw new InvalidHttpResponse("Invalid call " + this, response, null);
        }
      } catch (SocketException e) {
        // TODO review socket exception
        // throw new RuntimeException(e);
        throw org.jedio.RichThrowable.nowrap(e);
        // ignoredExceptionForRetry = RevobetExceptionLogger.handleWarnOrRetrhrow("While reading %s", e, get1);
      } catch (RequestAbortedException e) {
        // throw new RuntimeException(e);
        throw org.jedio.RichThrowable.nowrap(e);
        // ignoredExceptionForRetry = RevobetExceptionLogger.handleWarnOrRetrhrow("While reading %s", e, get1);
      } catch (ClientProtocolException e) {
        // throw new RuntimeException(e);
        throw org.jedio.RichThrowable.nowrap(e);
      } catch (IOException e) {
        // throw new RuntimeException(e);
        throw org.jedio.RichThrowable.nowrap(e);
      }
    }
    throw new InvalidHttpResponse("Invalid call " + this, lastResponse, ignoredExceptionForRetry);
    // return "error";
    // }).checkpoint("read", true);
    // response.getEntity().
    // EntityUtils.Future
    // <HttpResponse<String>> res = Unirest.get(url)
    // // .header("User-Agent", USER_AGENT).header("Accept", "*/*")
    // // .header("Content-Type", "application/json; charset=UTF-8").header("Accept-Encoding", "gzip,deflate,sdch")
    // // .asStringAsync(callback)();
    // .asStringAsync();
    //
    // return Mono.fromCallable(() -> res.get().getBody());
  }

  // specific exception for 401 || 403 respose codes
  public static class ForbiddenCallException extends RuntimeException {
    private static final long serialVersionUID = 1288977711847072395L;

    public ForbiddenCallException(String message, CloseableHttpResponse response,
        Throwable ignoredExceptionForRetry)
    {
      super(message + " response " + response);
      if (ignoredExceptionForRetry != null) {
        addSuppressed(ignoredExceptionForRetry);
      }
    }
  }

  // exception thrown for all other 40x cases
  private static class InvalidHttpResponse extends RuntimeException {
    private static final long serialVersionUID = 1288977711847072395L;

    public InvalidHttpResponse(String message, CloseableHttpResponse response, Throwable ignoredExceptionForRetry) {
      super(message + " response " + response);
      if (ignoredExceptionForRetry != null) {
        addSuppressed(ignoredExceptionForRetry);
      }
    }
  }

  @Override
  public InputStream unsafeInputStream() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  protected FiberUrlLocation2 create(URL url, boolean escaped) {
    throw new RuntimeException("Not implemented yet!!!");
  }
}