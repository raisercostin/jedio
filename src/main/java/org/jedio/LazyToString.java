package org.jedio;

import java.util.function.Supplier;

import javax.validation.constraints.NotNull;

import io.vavr.Lazy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Setter(AccessLevel.NONE)
@Getter(AccessLevel.NONE)
@NoArgsConstructor
@AllArgsConstructor
public class LazyToString<T> {
  public static @NotNull Object of(Supplier<Object> function) {
    return new Object()
      {
        @Override
        public String toString() {
          return function.get().toString();
        }
      };
  }

  public static <T> LazyToString<T> ofMemoized(Supplier<T> supplier) {
    return new LazyToString<>(Lazy.of(supplier));
  }

  public Lazy<T> value;

  @Override
  public String toString() {
    return value.get().toString();
  }
}
