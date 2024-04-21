package org.raisercostin.jedio.url;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.raisercostin.jedio.url.WebClientLocation2.RequestResponse;
import org.raisercostin.nodes.Nodes;

class WebClientLocation2Test {

  @Test
  void test() {
    String content = WebClientLocation2
      .httpGet("https://www.bnr.ro/files/xml/years/nbrfxrates2023.xml")
      .readContentSync();
    assertThat(content.length()).isGreaterThan(100000);
  }

  @Test
  void testAddingMetadata() {
    String content = WebClientLocation2
      .get("https://spotonfinance.com/version.json")
      .readContentSync();
    content = Nodes.json
      .excluding("date", "etag", "last-modified", "report-to", "x-kong-upstream-latency", "x-kong-proxy-latency", "nel",
        "cf-ray", "alt-svc")
      .prettyPrint(content);
    assertThat(content).isEqualTo(
      """
          {
            "metadata" : {
              "url" : "https://spotonfinance.com/version.json",
              "method" : "GET",
              "statusCode" : "200 OK",
              "statusCodeValue" : 200,
              "responseHeaders" : {
                "content-type" : "application/json",
                "accept-ranges" : "bytes",
                "via" : "kong/3.4.0",
                "cf-cache-status" : "DYNAMIC",
                "server" : "cloudflare",
                "content-length" : "23",
                "x-http2-stream-id" : "3"
              },
              "requestHeaders" : { }
            },
            "version" : "3686"
          }""");
  }

  @Test
  void testAddingMetadataToNonJson() {
    RequestResponse content = WebClientLocation2
      .get("https://www.google.com/robots.txt")
      .readCompleteContentSync(null);
    String body = content.getBody();
    String meta = content.computeMetadata();
    meta = Nodes.json
      .redacting("server", "date", "etag", "last-modified", "report-to", "x-kong-upstream-latency", "nel", "cf-ray",
        "alt-svc", "expires", "cross-origin-opener-policy-report-only")
      .prettyPrint(meta);
    assertThat(meta).isEqualTo(
      """
          {
            "url" : "https://www.google.com/robots.txt",
            "method" : "GET",
            "statusCode" : "200 OK",
            "statusCodeValue" : 200,
            "responseHeaders" : {
              "accept-ranges" : "bytes",
              "vary" : "Accept-Encoding",
              "content-type" : "text/plain",
              "cross-origin-resource-policy" : "cross-origin",
              "cross-origin-opener-policy-report-only-redacted" : "***",
              "report-to-redacted" : "***",
              "date-redacted" : "***",
              "expires-redacted" : "***",
              "cache-control" : "private, max-age=0",
              "last-modified-redacted" : "***",
              "x-content-type-options" : "nosniff",
              "server-redacted" : "***",
              "x-xss-protection" : "0",
              "alt-svc-redacted" : "***",
              "x-http2-stream-id" : "3",
              "transfer-encoding" : "chunked"
            },
            "requestHeaders" : { }
          }""");
    assertThat(StringUtils.abbreviateMiddle(body, "...", 200)).isEqualTo(
      """
          User-agent: *
          Disallow: /search
          Allow: /search/about
          Allow: /search/static
          Allow: /search/howsearch...low: /groups
          Disallow: /hosted/images/
          Disallow: /m/

          Sitemap: https://www.google.com/sitemap.xml
          """);
  }
}
