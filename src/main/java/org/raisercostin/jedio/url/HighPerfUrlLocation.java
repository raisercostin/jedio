package org.raisercostin.jedio.url;

import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Function;

import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.io.IOUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.LinkLocation;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.PathWithAttributes;
import org.raisercostin.jedio.op.DeleteOptions;
import org.raisercostin.jedio.path.PathLocation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Data
// @Getter(lombok.AccessLevel.NONE)
// @Setter(lombok.AccessLevel.NONE)
@AllArgsConstructor
@ToString
public class HighPerfUrlLocation implements ReferenceLocation, ReadableFileLocation {
  public static CloseableHttpClient createHighPerfHttpClient() {
    return createHighPerfHttpClient(mgr -> {
    });
  }

  public static CloseableHttpClient createHighPerfHttpClient(Consumer<PoolingHttpClientConnectionManager> manager) {
    ConnectionKeepAliveStrategy myStrategy = new ConnectionKeepAliveStrategy() {
      @Override
      public long getKeepAliveDuration(org.apache.http.HttpResponse response, HttpContext context) {
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
      }
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
    CloseableHttpClient client = HttpClients.custom().setKeepAliveStrategy(myStrategy).setConnectionManager(connManager)
        .build();
    return client;
  }

  public final String url;
  public final CloseableHttpClient client;

  @Override
  public ReferenceLocation child(RelativeLocation path) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String absolute() {
    return url;
  }

  @Override
  public String normalized() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String canonical() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String absoluteAndNormalized() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String real() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String getName() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<RelativeLocation> stripAncestor(DirLocation x) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<ReferenceLocation> findAncestor(Function<ReferenceLocation, Boolean> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public PathLocation makeDirOnParentIfNeeded() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<? extends ReferenceLocation> parent() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<DirLocation> existing() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<NonExistingLocation> nonExisting() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation nonExistingOrElse(Function<DirLocation, NonExistingLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public DirLocation existingOrElse(Function<NonExistingLocation, DirLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean exists() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public WritableFileLocation asWritableFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    return this;
  }

  @Override
  public boolean isDir() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean isFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public void symlinkTo(ReferenceLocation parent) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public void junctionTo(ReferenceLocation parent) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<LinkLocation> asSymlink() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean isSymlink() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<PathWithAttributes> find(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore,
      boolean dirsFirst) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation deleteFile(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public void rename(FileLocation asWritableFile) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation delete(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public long length() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<String> readIfExists() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public InputStream unsafeInputStream() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String readContent() {
    return HttpUtils.getFromURL(url);
  }

  @Override
  public UrlLocation create(String path) {
    return new UrlLocation(path);
  }

  @Override
  public DirLocation asDir() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  public Mono<String> readContentAsync() {
    HttpGet get1 = new HttpGet(url);
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
}