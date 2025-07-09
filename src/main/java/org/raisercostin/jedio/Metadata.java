package org.raisercostin.jedio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jedio.RichThrowable;
import org.jedio.regex.RichRegex;
import org.jedio.struct.RichIterable;
import org.raisercostin.jedio.path.PathLocation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@NoArgsConstructor
@AllArgsConstructor
public class Metadata {

  public static final class HeaderMapSerializer extends StdSerializer<HttpHeaders> {
    protected HeaderMapSerializer() {
      super((Class<HttpHeaders>) null);
    }

    @Override
    public void serialize(HttpHeaders value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeStartObject();
      for (Entry<String, List<String>> entry : value.entrySet()) {
        if (entry.getValue().size() == 1) {
          gen.writeStringField(entry.getKey(), entry.getValue().get(0));
        } else {
          gen.writeArrayFieldStart(entry.getKey());
          for (String item : entry.getValue()) {
            gen.writeString(item);
          }
          gen.writeEndArray();
        }
      }
      gen.writeEndObject();
    }
  }

  // Custom deserializer
  public static final class HeaderMapDeserializer extends StdDeserializer<HttpHeaders> {
    protected HeaderMapDeserializer() {
      super((Class<?>) null);
    }

    @Override
    public HttpHeaders deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      HttpHeaders result = new HttpHeaders();
      if (p.currentToken() != JsonToken.START_OBJECT) {
        throw ctxt.mappingException("Expected JSON object");
      }

      while (p.nextToken() != JsonToken.END_OBJECT) {
        String fieldName = p.getCurrentName();
        p.nextToken(); // move to value, it should either start an array or present a single value
        if (p.currentToken() == JsonToken.START_ARRAY) {
          List<String> values = new ArrayList<>();
          while (p.nextToken() != JsonToken.END_ARRAY) {
            values.add(p.getText());
          }
          result.addAll(fieldName, values);
        } else if (p.currentToken().isScalarValue()) { // ensure the token is a scalar value (like a string)
          result.add(fieldName, p.getText());
        } else {
          throw ctxt.mappingException("Expected a JSON array or a scalar value for field: " + fieldName);
        }
      }
      return result;
    }
  }

  public static Metadata error(String url, Throwable e) {
    return new Metadata(url, "GET", null, -1, null, null, RichThrowable.toString(e),
      null);
  }

  public String url;
  public String method;
  public String statusCode;
  public int statusCodeValue;
  @JsonSerialize(using = HeaderMapSerializer.class)
  @JsonDeserialize(using = HeaderMapDeserializer.class)
  public HttpHeaders responseHeaders;
  @JsonSerialize(using = HeaderMapSerializer.class)
  @JsonDeserialize(using = HeaderMapDeserializer.class)
  public HttpHeaders requestHeaders;
  public String error;

  @JsonAnyGetter
  public Map<String, Object> fields;

  @JsonAnySetter
  public void addField(String name, Object value) {
    if (fields == null) {
      fields = new HashMap<>();
    }
    fields.put(name, value);
  }

  public long length() {
    return responseHeaders.getContentLength();
  }

  public MediaType contentType() {
    return responseHeaders.getContentType();
  }

  public String httpMetaRequestUri() {
    //return requestHeaders.getrequest.requestLine.uri;
    return url;
  }

  public static Metadata empty(String url) {
    Metadata res = new Metadata();
    res.url = url;
    return res;
  }

  public boolean httpResponseHeaderContentTypeIsHtml() {
    if (responseHeaders == null) {
      return false;
    }
    return contentType().toString().startsWith("text/html");
  }
}
