package org.raisercostin.jedio.url;

import java.util.Map;

import com.google.common.collect.Maps;
import org.apache.http.protocol.HttpContext;

public class JedioHttpClientContext implements HttpContext {
  public final Map<String, Object> attrs;

  public JedioHttpClientContext() {
    this.attrs = Maps.newConcurrentMap();
  }

  @Override
  public void setAttribute(String id, Object obj) {
    if (id == null || obj == null) {
      // log.warn("cannot add attribute {}:{}", id, obj);
    } else {
      attrs.put(id, obj);
    }
  }

  @Override
  public Object removeAttribute(String id) {
    return attrs.remove(id);
  }

  @Override
  public Object getAttribute(String id) {
    return attrs.get(id);
  }

  @Override
  public String toString() {
    return "JedioHttpClientContext(" + attrs.toString() + ")";
  }
}
