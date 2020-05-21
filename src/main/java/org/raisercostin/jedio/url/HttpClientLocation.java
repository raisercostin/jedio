package org.raisercostin.jedio.url;

import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import io.vavr.API;
import io.vavr.collection.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.execchain.RequestAbortedException;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.jedio.Audit;
import org.raisercostin.jedio.MetaInfo.StreamAndMeta;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.url.JedioHttpClients.JedioHttpClient;
import org.raisercostin.util.functions.JedioFunction;
import reactor.core.publisher.Mono;

@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@Slf4j
public class HttpClientLocation extends BaseHttpLocation<HttpClientLocation> {
  @SneakyThrows
  public static HttpClientLocation url(String sourceHyperlink, String relativeOrAbsoluteHyperlink,
      JedioHttpClient defaultClient) {
    return new HttpClientLocation(SimpleUrl.resolve(sourceHyperlink, relativeOrAbsoluteHyperlink), true,
      defaultClient);
  }

  private static final int retries = 5;
  @JsonIgnore
  public final JedioHttpClient client;

  public HttpClientLocation(String url, boolean escaped, JedioHttpClient client) {
    super(url, escaped);
    this.client = client;
  }

  public HttpClientLocation(URL url, boolean escaped, JedioHttpClient client) {
    super(url, escaped);
    this.client = client;
  }

  @Override
  public String toString() {
    return String.format("HttpClientLocation(%s, client=%s)", url, client.name);
  }

  @Override
  public String absolute() {
    return url.toExternalForm();
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    return this;
  }

  @Override
  @SneakyThrows
  public InputStream unsafeInputStream() {
    HttpGet get1 = new HttpGet(url.toExternalForm());
    CloseableHttpResponse lastResponse = null;
    Throwable ignoredExceptionForRetry = null;
    try (CloseableHttpResponse response = client.client().execute(get1)) {
      int code = response.getStatusLine().getStatusCode();
      String reason = response.getStatusLine().getReasonPhrase();
      lastResponse = response;
      return response.getEntity().getContent();
    }
    // //TODO use client
    // URLConnection conn = connection.get();
    // // if(url instanceof HttpURLConnection) {
    // // }
    // return conn.getInputStream();
  }

  @Data
  @AllArgsConstructor
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @Slf4j
  public static class HttpClientLocationMeta {
    public HttpClientLocationMetaRequest request;
    public HttpClientLocationMetaResponse response;
    public java.util.Map<String, Object> attrs;
  }

  @Data
  @AllArgsConstructor
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @Slf4j
  public static class HttpClientLocationMetaRequest {
    public RequestLine requestLine;
    public RequestConfig requestConfig;
    public HttpParams httpParams;
    public Map<String, Object> header;
  }

  @Data
  @AllArgsConstructor
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @Slf4j
  public static class HttpClientLocationMetaResponse {
    public StatusLine statusLine;
    public Map<String, Object> header;
  }

  @Override
  @SneakyThrows
  public <R> R usingInputStreamAndMeta(boolean returnExceptionsAsMeta,
      JedioFunction<StreamAndMeta, R> inputStreamConsumer) {
    HttpGet request = new HttpGet(url.toExternalForm());
    request.addHeader("Connection", "keep-alive");
    request.addHeader("Pragma", "no-cache");
    request.addHeader("Cache-Control", "no-cache");
    request.addHeader("Upgrade-Insecure-Requests", "1");
    request.addHeader("User-Agent",
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36");
    request.addHeader("Accept",
      "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
    request.addHeader("Accept-Encoding", "gzip, deflate");
    // not needed anymore - request.addHeader("Accept-Charset", "utf-8, iso-8859-1;q=0.5");
    request.addHeader("Accept-Language", "en-US,en;q=0.9,ro;q=0.8");
    java.util.Map<String, Object> attrs = Maps.newConcurrentMap();
    HttpClientContext context = HttpClientContext.adapt(new HttpContext()
      {
        @Override
        public void setAttribute(String id, Object obj) {
          if (id == null || obj == null) {
            // log.warn("cannot add attribute {}:{}", id, obj);
          } else {
            attrs.put(id, obj);
          }
        }

        @Override
        public Object removeAttribute(String id) {
          return attrs.remove(id);
        }

        @Override
        public Object getAttribute(String id) {
          return attrs.get(id);
        }
      });
    try (CloseableHttpResponse response = client.client().execute(request, context)) {
      int code = response.getStatusLine().getStatusCode();
      String reason = response.getStatusLine().getReasonPhrase();
      InputStream in = response.getEntity().getContent();
      HttpClientLocationMetaRequest req = new HttpClientLocationMetaRequest(request.getRequestLine(),
        request.getConfig(), request.getParams(), toHeaders(request.getAllHeaders()));
      HttpClientLocationMetaResponse res = new HttpClientLocationMetaResponse(response.getStatusLine(),
        toHeaders(response.getAllHeaders()));
      context.removeAttribute(context.HTTP_RESPONSE);
      context.removeAttribute(context.HTTP_REQUEST);
      context.removeAttribute(context.HTTP_CONNECTION);
      context.removeAttribute("http.cookie-spec");
      context.removeAttribute("http.cookiespec-registry");
      return inputStreamConsumer.apply(StreamAndMeta.fromPayload(new HttpClientLocationMeta(req, res, attrs), in));
    }
  }

  private Map<String, Object> toHeaders(Header[] allHeaders) {
    return API.List(allHeaders).toMap(x -> x.getName(), x -> x.getValue());
  }

  @Override
  @SneakyThrows
  public boolean exists() {
    return true;
    // HttpGet get1 = new HttpGet(url.toExternalForm());
    // CloseableHttpResponse response = client.execute(get1);
  }

  @Override
  public String readContent() {
    return readContentAsync().block();
  }

  public Mono<String> readContentAsyncOld() {
    HttpGet get1 = new HttpGet(url.toExternalForm());
    return Mono.fromCallable(() -> {
      try (CloseableHttpResponse response = client.client().execute(get1)) {
        return IOUtils.toString(response.getEntity().getContent());
      }
    });
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

  private static class InvalidHttpResponse extends RuntimeException {
    private static final long serialVersionUID = 1288977711847072395L;
    public final CloseableHttpResponse response;

    public InvalidHttpResponse(String message, CloseableHttpResponse response, Throwable ignoredExceptionForRetry) {
      super(message + " response " + response);
      this.response = response;
      if (ignoredExceptionForRetry != null) {
        addSuppressed(ignoredExceptionForRetry);
      }
    }
  }

  @Override
  public Mono<String> readContentAsync() {
    HttpGet get1 = new HttpGet(url.toExternalForm());
    return Mono.fromCallable(() -> {
      // if (enableHardAbort) {
      // scheduler.schedule(() -> {
      // // redundant null check
      // // if (get1 != null) {
      // get1.abort();
      // // }
      // }, hardTimeout, TimeUnit.SECONDS);
      // }

      CloseableHttpResponse lastResponse = null;
      Throwable ignoredExceptionForRetry = null;
      for (int attempt = 1; attempt <= retries; attempt++) {
        try (CloseableHttpResponse response = client.client().execute(get1)) {
          int code = response.getStatusLine().getStatusCode();
          String reason = response.getStatusLine().getReasonPhrase();
          lastResponse = response;
          if (code == 200) {
            final InputStream content = response.getEntity().getContent();
            return IOUtils.toString(content);
          } else if (code == 502 || code == 520) {
            log.info("Attempt {} to {} failed: {}", attempt, url, response);
          } else {
            throw new InvalidHttpResponse("Invalid call " + this, response, null);
          }
        } catch (SocketException e) {
          ignoredExceptionForRetry = Audit.warnAndRetrhrowIfNotKnown("While reading %s", e, get1);
        } catch (RequestAbortedException e) {
          ignoredExceptionForRetry = Audit.warnAndRetrhrowIfNotKnown("While reading %s", e, get1);
        }
      }
      throw new InvalidHttpResponse("Invalid call " + this, lastResponse, ignoredExceptionForRetry);
    }).checkpoint("read", true);
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

  @Override
  public HttpClientLocation child(String link) {
    return create(url, true);
  }

  @Override
  @SneakyThrows
  public URI toUri() {
    return url.toURI();
  }

  @Override
  protected HttpClientLocation create(URL url, boolean escaped) {
    return new HttpClientLocation(url, escaped, client);
  }
}
