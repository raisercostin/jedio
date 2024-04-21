package org.raisercostin.jedio.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
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

  private static final String FILTER_ID = "filter properties by name";

  //Custom property filter
  static class CustomFieldHidingFilter extends SimpleBeanPropertyFilter {
    private Set<String> fieldsToHide;

    public CustomFieldHidingFilter(String... fieldsToHide) {
      this.fieldsToHide = new HashSet<>(Arrays.asList(fieldsToHide));
    }

    @Override
    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer)
        throws Exception {
      if (fieldsToHide.contains(writer.getName())) {
        jgen.writeFieldName(writer.getName() + "-ignored");
        jgen.writeString("***");
      } else {
        super.serializeAsField(pojo, jgen, provider, writer);
      }
    }
  }

  //Mixin class
  @JsonFilter(FILTER_ID)
  static class PropertyFilterMixin {
  }

  //Configuration method
  public static void configureFieldHiding(ObjectMapper mapper, String... excludedFields) {
    mapper.setFilterProvider(new SimpleFilterProvider().addFilter(
      FILTER_ID,
      new CustomFieldHidingFilter(excludedFields)));
    mapper.addMixIn(Object.class, PropertyFilterMixin.class);
  }
  //
  //  public static ObjectMapper cloneMapperWithSettings(ObjectMapper original) {
  //    ObjectMapper copy = original.copy();
  //
  //    // Manually copy mix-ins from the original to the new mapper
  //    for (Map.Entry<Class<?>, Class<?>> entry : original._mixIns.entrySet()) {
  //      copy.addMixIn(entry.getKey(), entry.getValue());
  //    }
  //
  //    // Manually copy the filter provider settings
  //    if (original.getFilterProvider() != null) {
  //      copy.setFilterProvider(original.getFilterProvider());
  //    }
  //
  //    return copy;
  //  }

  @Test
  void testAddingMetadataToNonJson() {
    RequestResponse content = WebClientLocation2
      .get("https://www.google.com/robots.txt")
      .readCompleteContentSync(null);
    String body = content.getBody();
    String meta = content.computeMetadata();
    configureFieldHiding(Nodes.json.mapper, "server", "date", "etag", "last-modified", "report-to",
      "x-kong-upstream-latency", "nel", "cf-ray", "alt-svc",
      "expires", "cross-origin-opener-policy-report-only");
    meta = Nodes.json
      //      .excluding("date", "etag", "last-modified", "report-to", "x-kong-upstream-latency", "nel", "cf-ray", "alt-svc",
      //        "expires", "cross-origin-opener-policy-report-only")
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
              "cross-origin-opener-policy-report-only-ignored" : "***",
              "report-to-ignored" : "***",
              "date-ignored" : "***",
              "expires-ignored" : "***",
              "cache-control" : "private, max-age=0",
              "last-modified-ignored" : "***",
              "x-content-type-options" : "nosniff",
              "server-ignored" : "***",
              "x-xss-protection" : "0",
              "alt-svc-ignored" : "***",
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
