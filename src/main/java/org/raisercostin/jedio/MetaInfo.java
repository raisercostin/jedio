package org.raisercostin.jedio;

import java.io.InputStream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
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
  Object payload;
}
