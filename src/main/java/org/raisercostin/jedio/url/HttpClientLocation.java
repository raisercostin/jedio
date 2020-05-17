package org.raisercostin.jedio.url;

import java.io.InputStream;
import java.net.SocketException;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.execchain.RequestAbortedException;
import org.jedio.Audit;
import org.raisercostin.jedio.ReadableFileLocation;
import reactor.core.publisher.Mono;

@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@ToString
@Slf4j
public class HttpClientLocation extends HttpBaseLocation<HttpClientLocation> {
  private static final int retries = 5;
  public final CloseableHttpClient client;

  public HttpClientLocation(String url, CloseableHttpClient client) {
    super(url);
    this.client = client;
  }

  @Override
  public String absolute() {
    return url.toExternalForm();
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    return this;
  }

  /**
  //protected override
  def unsafeToInputStreamUsingJava: InputStream = {
    url.openConnection() match {
      case conn: HttpURLConnection =>
        config.configureConnection(conn)
        import scala.collection.JavaConverters._
        UrlLocation.logger.info("header:\n" + config.header.mkString("\n    "))
        UrlLocation.logger.info(s"RequestHeaders for $raw:\n    " + conn.getRequestProperties.asScala.mkString("\n    "))
        //if (UrlLocation.log.isDebugEnabled())
        UrlLocation.logger.info(s"ResponseHeaders for $raw:\n    " + Try { conn.getHeaderFields.asScala.mkString("\n    ") })
        handleCode(conn.getResponseCode, conn.getHeaderField("Location"), { conn.getInputStream }, Try { conn.getHeaderFields.asScala.toMap })
      case conn =>
        conn.getInputStream
    }
  }

  def handleCode(code: Int, location: String, stream: => InputStream, map: => Try[Map[String, _]]): InputStream =
    (code, location) match {
      case (200, _) =>
        stream
      case (code, location) if config.allowedRedirects > redirects.size && location != null && location.nonEmpty && location != raw =>
        //This is manual redirection. The connection should already do all the redirects if config.allowedRedirects is true
        closeStream(stream)
        UrlLocation(new java.net.URL(location), this +: redirects, config).unsafeToInputStream
      case (code, _) =>
        closeStream(stream)
        throw new HttpStatusException(s"Got $code response from $this. A 200 code is needed to get an InputStream. The header is\n    " + map.getOrElse(Map()).mkString("\n    ")
          + " After " + redirects.size + " redirects:\n    " + redirects.mkString("\n    "), code, this)
    }

  // * Shouldn't disconnect as it "Indicates that other requests to the server are unlikely in the near future."
  // * We should just close() on the input/output/error streams
  //  * http://stackoverflow.com/questions/15834350/httpurlconnection-closing-io-streams
  def closeStream(stream: => InputStream) = Try {
    if (stream != null)
      stream.close
  }.recover { case e => UrlLocation.logger.debug("Couldn't close input/error stream to " + this, e) }
  */
  @Override
  @SneakyThrows
  public InputStream unsafeInputStream() {
    throw new RuntimeException("Not implemented yet!!!");
    //    //TODO use client
    //    URLConnection conn = connection.get();
    //    //    if(url instanceof HttpURLConnection) {
    //    //    }
    //    return conn.getInputStream();
  }

  @Override
  @SneakyThrows
  public boolean exists() {
    return true;
    //  HttpGet get1 = new HttpGet(url.toExternalForm());
    //  CloseableHttpResponse response = client.execute(get1);
  }

  @Override
  public String readContent() {
    return readContentAsync().block();
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

  public Mono<String> readContentAsyncOld() {
    HttpGet get1 = new HttpGet(url.toExternalForm());
    return Mono.fromCallable(() -> {
      try (CloseableHttpResponse response = client.execute(get1)) {
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

  @Override
  public Mono<String> readContentAsync() {
    HttpGet get1 = new HttpGet(url.toExternalForm());
    return Mono.fromCallable(() -> {
      //      if (enableHardAbort) {
      //        scheduler.schedule(() -> {
      //          // redundant null check
      //          // if (get1 != null) {
      //          get1.abort();
      //          // }
      //        }, hardTimeout, TimeUnit.SECONDS);
      //      }

      CloseableHttpResponse lastResponse = null;
      Throwable ignoredExceptionForRetry = null;
      for (int attempt = 1; attempt <= retries; attempt++) {
        try (CloseableHttpResponse response = client.execute(get1)) {
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
}
