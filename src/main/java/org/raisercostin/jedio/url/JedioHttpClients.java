package org.raisercostin.jedio.url;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vavr.Lazy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
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
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class JedioHttpClients {
  private static final String SOCKET_REMOTE_ADDRESS = "socket-remote-address";
  private static final String SOCKET_LOCAL_ADDRESS = "socket-local-address";

  @Data
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @ToString
  public static class JedioHttpClient {
    public static JedioHttpClient from(String name, HttpClientBuilder builder) {
      return new JedioHttpClient(name, builder, Lazy.of(() -> builder.build()));
    }

    public String name;
    public HttpClientBuilder builder;
    public Lazy<CloseableHttpClient> client;

    public CloseableHttpClient client() {
      return client.get();
    }
  }

  private static final int timeout = 20;
  // private static final int hardTimeout = 5 * timeout; // seconds
  private static final int MILLIS = 1000;
  private static final int ROUTES = 1000;
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BaseHttpLocation.class);
  private static final Scheduler scheduler = Schedulers.newParallel("http-hard-abort", 100);
  private static final boolean enableHardAbort = true;

  public static JedioHttpClient createHighPerfHttpClient() {
    return JedioHttpClient.from("jedio1", createHighPerfHttpClient(mgr -> {
    }));
  }

  /**
   * See - https://www.baeldung.com/httpclient-connection-management - https://www.baeldung.com/httpclient-timeout
   */
  @SneakyThrows
  public static HttpClientBuilder createHighPerfHttpClient(Consumer<PoolingHttpClientConnectionManager> manager) {
    ConnectionKeepAliveStrategy keepAliveStrategy = keepAliveStrategy();
    PoolingHttpClientConnectionManager connManager = createConnectionManager(manager);
    RequestConfig requestConfig = createRequestConfig();
    HttpClientBuilder builder = HttpClients.custom().setKeepAliveStrategy(keepAliveStrategy)
        .setConnectionManager(connManager).setRetryHandler(retryHandler()).setConnectionTimeToLive(60, TimeUnit.SECONDS)
        .setDefaultRequestConfig(requestConfig).disableRedirectHandling()
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

  private static RequestConfig createRequestConfig() {
    return RequestConfig.custom()
        // .setStaleConnectionCheckEnabled(true) - used setValidateAfterInactivity
        .setConnectTimeout(timeout * MILLIS).setConnectionRequestTimeout(timeout * MILLIS)
        .setSocketTimeout(timeout * MILLIS).setContentCompressionEnabled(true).build();
  }

  private static PoolingHttpClientConnectionManager createConnectionManager(
      Consumer<PoolingHttpClientConnectionManager> manager) {
    PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
        createSocketFactoryRegistry(), SystemDefaultDnsResolver.INSTANCE);
    // HttpHost host = new HttpHost("hostname", 80);
    // HttpRoute route = new HttpRoute(host);
    // connManager.setSocketConfig(route.getTargetHost(), SocketConfig.custom().setSoTimeout(5000).build());
    // Set the maximum number of total open connections.
    connManager.setMaxTotal(ROUTES);
    // Set the maximum number of concurrent connections per route, which is 2 by default.
    connManager.setDefaultMaxPerRoute(ROUTES);
    ConnectionConfig defaultConnectionConfig = ConnectionConfig.custom().build();
    // Set the total number of concurrent connections to a specific route, which is 2 by default.
    // connManager.setMaxPerRoute(route, 5);
    connManager.setDefaultConnectionConfig(defaultConnectionConfig);
    final int soTimeoutInMilllis = timeout * MILLIS;
    SocketConfig defaultSocketConfig = SocketConfig.custom().setSoTimeout(soTimeoutInMilllis).build();
    connManager.setDefaultSocketConfig(defaultSocketConfig);
    connManager.setValidateAfterInactivity(timeout * MILLIS);
    manager.accept(connManager);
    return connManager;
  }

  private static Registry<ConnectionSocketFactory> createSocketFactoryRegistry() {
    return RegistryBuilder.<ConnectionSocketFactory>create().register("http", createPlainConnectionSocketFactory())
        .register("https", createSSLConnectionSocketFactory()).build();
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
    return new PlainConnectionSocketFactory() {
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
          log.info("checking hostname {}", hostname);
          return true;
        }) {
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
      return timeout * MILLIS;
    };
  }

  private static HttpRequestRetryHandler retryHandler() {
    return (exception, executionCount, context) -> {
      boolean retryable = checkRetriable(exception, executionCount, context);
      log.warn("error. try again request: {}. {} Enable debug to see fullstacktrace.", retryable,
          exception.getMessage());
      log.debug("error. try again request: {}. Fullstacktrace.", retryable, exception.getMessage(), exception);
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
          return Long.parseLong(value) * 1000;
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
}
