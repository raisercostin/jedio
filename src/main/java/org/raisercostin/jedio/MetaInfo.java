package org.raisercostin.jedio;

import java.io.InputStream;
import java.nio.charset.Charset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import io.vavr.API;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.jedio.NodeUtils;
import org.raisercostin.nodes.Nodes;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
@Slf4j
@ToString
public class MetaInfo {
  public static MetaInfo error(Throwable e) {
    String errorAsString = Nodes.json.toString(e);
    return new MetaInfo(false, e, errorAsString, null);
  }

  public static MetaInfo payload(Map<String, Object> payload) {
    return new MetaInfo(true, null, null, payload);
  }

  public static MetaInfo success(ReadableFileLocation source) {
    return new MetaInfo(true, null, null, API.Map("source", source));
  }

  @AllArgsConstructor
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @Slf4j
  public static class StreamAndMeta implements AutoCloseable {
    public static StreamAndMeta fromThrowable(Throwable e) {
      return new StreamAndMeta(MetaInfo.error(e), null);
    }

    public static StreamAndMeta fromPayload(Object payload, InputStream in) {
      return fromPayloadMap(toMap(payload), in);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object payload) {
      return Nodes.json.toObject(Nodes.json.toString(payload), Map.class);
    }

    public static StreamAndMeta fromPayloadMap(Map<String, Object> payload, InputStream in) {
      return new StreamAndMeta(MetaInfo.payload(payload), in);
    }

    public MetaInfo meta;
    public InputStream is;

    @Override
    @SneakyThrows
    public void close() {
      if (this.is != null) {
        this.is.close();
      }
    }

    @SneakyThrows
    public String readContent() {
      return readContent(ReadableFileLocation.charset1_UTF8);
    }

    @SneakyThrows
    public String readContent(Charset charset) {
      return IOUtils.toString(is, charset);
    }
  }

  boolean isSuccess;
  @JsonProperty(access = Access.READ_ONLY)
  Throwable error;
  String errorAsString;
  Map<String, Object> payload;

  /** Selector is converted to lowercase before search since http header is case insensitive. */
  public Option<String> field(String pointSelector) {
    String[] keys = pointSelector.split("[.]");
    return Option.of(NodeUtils.nullableString(this, keys));
  }

  public Option<String> httpResponseHeaderContentType() {
    return field("payload.response.header.Content-Type");
  }

  public Option<Integer> httpMetaResponseStatusCode() {
    return field("payload.response.statusLine.statusCode").map(x -> new Integer(x));
  }

  private Option<String> httpMetaResponseStatusPhrase() {
    return field("payload.response.statusLine.reasonPhrase");
  }

  public boolean httpMetaResponseStatusCodeIs200() {
    return httpMetaResponseStatusCode().map(x -> x.equals(200)).getOrElse(false);
  }

  public Option<String> httpMetaRequestUri() {
    return field("payload.request.requestLine.uri");
  }

  public Option<String> httpMetaResponseHeaderLocation() {
    return field("payload.response.header.Location");
  }

  public boolean httpResponseHeaderContentTypeIsHtml() {
    return httpResponseHeaderContentType().getOrElse("").startsWith("text/html");
  }

  public Option<String> httpMetaResponseStatusToString() {
    return httpMetaResponseStatusCode().map(x -> x + " : " + httpMetaResponseStatusPhrase().get());
  }
}
