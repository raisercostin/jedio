package org.raisercostin.jedio.url;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class HttpUtils {
  private final static AtomicBoolean first = new AtomicBoolean(true);
  private static final String USER_AGENT = "Mozilla/5.0";

  private static void init() {
    if (first.getAndSet(false)) {
      try {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
          }

          public void checkClientTrusted(X509Certificate[] certs, String authType) {
          }

          public void checkServerTrusted(X509Certificate[] certs, String authType) {
          }

        } };

        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        Unirest.setHttpClient(httpclient);
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      } catch (KeyManagementException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static HttpResponse<String> getFromURLUsingUnirest(String url) {
    init();
    // logger.debug("call lsports ... " + urlAddress);
    // https://github.com/Kong/unirest-java/issues/148
    try {
      HttpResponse<String> res = Unirest.get(url).header("User-Agent", USER_AGENT).header("Accept", "*/*")
          .header("Content-Type", "application/json; charset=UTF-8").header("Accept-Encoding", "gzip,deflate,sdch")
          .asString();
      // logger.debug(
      // "call lsports done." + urlAddress + " done (HTTP-" + res.getStatus() +
      // ": " + res.getStatusText() + ")");
      return res;
    } catch (UnirestException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getFromURL(String url) {
    HttpResponse<String> result = getFromURLUsingUnirest(url);
    if (result.getStatus() == 500 && result.getBody().contains("Too many simultaneous requests."))
      throw new RuntimeException("Wait one minute to cool off. Too many simultaneous requests for [" + url
          + "]\nResult:\n" + result.getStatus() + "[" + result.getStatusText() + "]\nHeader:\n" + result.getHeaders()
          + "\nBody:\n" + result.getBody());
    if (result.getStatus() >= 300)
      throw new RuntimeException("Bad response for [" + url + "]\nResult:\n" + result.getStatus() + "["
          + result.getStatusText() + "]\nHeader:\n" + result.getHeaders() + "\nBody:\n" + result.getBody());
    return result.getBody();
  }
}
