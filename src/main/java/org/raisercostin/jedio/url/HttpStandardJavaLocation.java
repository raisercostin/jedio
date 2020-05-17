package org.raisercostin.jedio.url;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;

import io.vavr.Lazy;
import lombok.SneakyThrows;

public class HttpStandardJavaLocation extends HttpBaseLocation<HttpStandardJavaLocation> implements Closeable {
  public Lazy<URLConnection> connection = Lazy.of(() -> createConnection());

  public HttpStandardJavaLocation(String url) {
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
}
