package org.raisercostin.jedio.url;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpUtils {
  private final static AtomicBoolean first = new AtomicBoolean(true);
  private static final String USER_AGENT = "Mozilla/5.0";

  private static void init() {
    if (first.getAndSet(false)) {
      try {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
          {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }

          } };

        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
        //        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
        //        try (CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build()) {
        //          Unirest.setHttpClient(httpclient);
        //        } catch (IOException e) {
        //          throw new RuntimeException(e);
        //        }
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      } catch (KeyManagementException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static HttpResponse<String> getFromURLUsingStandardHttp(String url) {
    try {
      HttpRequest request = HttpRequest
        .newBuilder()
        .uri(new URI(url))
        .header("User-Agent", USER_AGENT)
        .header("Accept", "*/*")
        .header("Content-Type", "application/json; charset=UTF-8")
        .header("Accept-Encoding", "gzip,deflate,sdch")
        .GET()
        .build();
      HttpClient client = HttpClient.newHttpClient();
      return client.send(request, BodyHandlers.ofString());
    } catch (URISyntaxException e) {
      throw org.jedio.RichThrowable.nowrap(e);
    } catch (IOException e) {
      throw org.jedio.RichThrowable.nowrap(e);
    } catch (InterruptedException e) {
      throw org.jedio.RichThrowable.nowrap(e);
    }
  }

  public static String getFromURL(String url) {
    HttpResponse<String> result = getFromURLUsingStandardHttp(url);
    if (result.statusCode() == 500 && result.body().contains("Too many simultaneous requests.")) {
      throw new RuntimeException("Wait one minute to cool off. Too many simultaneous requests for [" + url
          + "]\nResult:\n" + result.statusCode() + "[" + getStatusText(result) + "]\nHeader:\n" + result.headers()
          + "\nBody:\n" + result.body());
    }
    if (result.statusCode() >= 300) {
      throw new RuntimeException("Bad response for [" + url + "]\nResult:\n" + result.statusCode() + "["
          + getStatusText(result) + "]\nHeader:\n" + result.headers() + "\nBody:\n" + result.body());
    }
    return result.body();
  }

  private static String getStatusText(java.net.http.HttpResponse<String> result) {
    return getReasonPhrase(result.statusCode());
  }

  public static String getReasonPhrase(int statusCode) {
    return switch (statusCode) {
      case 200 -> "OK";
      case 201 -> "Created";
      case 202 -> "Accepted";
      case 203 -> "Non Authoritative Information";
      case 204 -> "No Content";
      case 205 -> "Reset Content";
      case 206 -> "Partial Content";
      case 207 -> "Partial Update OK";
      case 300 -> "Mutliple Choices";
      case 301 -> "Moved Permanently";
      case 302 -> "Moved Temporarily";
      case 303 -> "See Other";
      case 304 -> "Not Modified";
      case 305 -> "Use Proxy";
      case 307 -> "Temporary Redirect";
      case 400 -> "Bad Request";
      case 401 -> "Unauthorized";
      case 402 -> "Payment Required";
      case 403 -> "Forbidden";
      case 404 -> "Not Found";
      case 405 -> "Method Not Allowed";
      case 406 -> "Not Acceptable";
      case 407 -> "Proxy Authentication Required";
      case 408 -> "Request Timeout";
      case 409 -> "Conflict";
      case 410 -> "Gone";
      case 411 -> "Length Required";
      case 412 -> "Precondition Failed";
      case 413 -> "Request Entity Too Large";
      case 414 -> "Request-URI Too Long";
      case 415 -> "Unsupported Media Type";
      case 416 -> "Requested Range Not Satisfiable";
      case 417 -> "Expectation Failed";
      case 418 -> "Reauthentication Required";
      case 419 -> "Proxy Reauthentication Required";
      case 422 -> "Unprocessable Entity";
      case 423 -> "Locked";
      case 424 -> "Failed Dependency";
      case 500 -> "Server Error";
      case 501 -> "Not Implemented";
      case 502 -> "Bad Gateway";
      case 503 -> "Service Unavailable";
      case 504 -> "Gateway Timeout";
      case 505 -> "HTTP Version Not Supported";
      case 507 -> "Insufficient Storage";
      default -> "Unexpected value: " + statusCode;
    };
  }

}
