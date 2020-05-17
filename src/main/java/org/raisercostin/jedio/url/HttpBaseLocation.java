package org.raisercostin.jedio.url;

import java.net.URL;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;

@Data
@Getter(lombok.AccessLevel.NONE)
@Setter(lombok.AccessLevel.NONE)
@ToString
public abstract class HttpBaseLocation<SELF extends HttpBaseLocation<SELF>>
    implements ReferenceLocation<SELF>, ReadableFileLocation<SELF> {
  public final URL url;

  @SneakyThrows
  HttpBaseLocation(String url) {
    this.url = new URL(url);
  }

  @Override
  public String toExternalForm() {
    return url.toExternalForm();
  }
}