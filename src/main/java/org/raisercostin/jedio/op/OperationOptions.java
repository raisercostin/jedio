package org.raisercostin.jedio.op;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

public interface OperationOptions {

  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReadOptions implements OperationOptions {
    public static final Charset utf8 = StandardCharsets.UTF_8;
    public Charset defaultCharset = StandardCharsets.UTF_8;
    public Charset fallbackCharset = StandardCharsets.ISO_8859_1;
    public Duration blockingReadDuration = Duration.ofSeconds(30);

    public ReadOptions withDefaultCharset(Charset charset) {
      return new ReadOptions(charset, fallbackCharset, blockingReadDuration);
    }
  }
}
