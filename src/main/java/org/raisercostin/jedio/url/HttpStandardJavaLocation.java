package org.raisercostin.jedio.url;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import io.vavr.Lazy;
import lombok.SneakyThrows;
import org.apache.commons.httpclient.URI;

public class HttpStandardJavaLocation extends BaseHttpLocation<HttpStandardJavaLocation> implements Closeable {
  public Lazy<URLConnection> connection = Lazy.of(() -> createConnection());

  public HttpStandardJavaLocation(String url) {
    super(url);
  }

  public HttpStandardJavaLocation(URI uri) {
    super(uri);
  }

  public HttpStandardJavaLocation(URL url) {
    super(url);
  }

  @SneakyThrows
  private URLConnection createConnection() {
    return url.openConnection();
  }

  @Override
  @SneakyThrows
  public InputStream unsafeInputStream() {
    URLConnection conn = connection.get();
    return conn.getInputStream();
  }

  @Override
  @SneakyThrows
  public boolean exists() {
    HttpURLConnection huc = (HttpURLConnection) connection.get();
    int responseCode = huc.getResponseCode();
    return responseCode != HttpURLConnection.HTTP_NOT_FOUND;
  }

  @Override
  public String readContent() {
    return HttpUtils.getFromURL(url.toExternalForm());
  }

  @Override
  public void close() throws IOException {
    if (connection.isEvaluated()) {
      HttpURLConnection huc = (HttpURLConnection) connection.get();
      huc.disconnect();
    }
  }

  @Override
  protected HttpStandardJavaLocation create(URL resolve) {
    return new HttpStandardJavaLocation(url);
  }
}
