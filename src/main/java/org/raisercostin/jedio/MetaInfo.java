package org.raisercostin.jedio;

import java.io.InputStream;

import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jedio.NodeUtils;
import org.raisercostin.nodes.Nodes;

@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@FieldDefaults(makeFinal = true, level = AccessLevel.PUBLIC)
@Slf4j
public class MetaInfo {
  @AllArgsConstructor
  @Getter(lombok.AccessLevel.NONE)
  @Setter(lombok.AccessLevel.NONE)
  @Slf4j
  public static class StreamAndMeta implements AutoCloseable {
    public static StreamAndMeta fromThrowable(Throwable e) {
      return new StreamAndMeta(new MetaInfo(false, e, null), null);
    }

    public static StreamAndMeta fromPayload(Object payload, InputStream in) {
      return fromPayload(toMap(payload), in);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object payload) {
      return Nodes.json.toObject(Nodes.json.toString(payload), Map.class);
    }

    public static StreamAndMeta fromPayload(Map<String, Object> payload, InputStream in) {
      return new StreamAndMeta(new MetaInfo(true, null, payload), in);
    }

    public MetaInfo meta;
    public InputStream is;

    @Override
    @SneakyThrows
    public void close() {
      if (is != null) {
        is.close();
      }
    }
  }

  boolean isSuccess;
  Throwable error;
  Map<String, Object> payload;

  @SuppressWarnings("unchecked")
  public Option<String> field(String pointSelector) {
    String[] keys = pointSelector.split("[.]");
    return Option.of(NodeUtils.nullableString(this, keys));
  }

  public Option<String> httpResponseHeaderContentType() {
    return field("payload.response.header.Content-Type");
  }

  public Option<String> httpMetaResponseStatusCode() {
    return field("payload.response.statusLine.statusCode");
  }

  public boolean httpMetaResponseStatusCodeIs200() {
    return httpMetaResponseStatusCode().map(x -> x.equals("200")).getOrElse(false);
  }

  public Option<String> httpMetaRequestUri() {
    return field("payload.request.requestLine.uri");
  }

  public Option<String> httpMetaResponseHeaderLocation() {
    return field("payload.response.header.Location");
  }
}
