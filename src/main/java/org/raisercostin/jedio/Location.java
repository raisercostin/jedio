package org.raisercostin.jedio;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.raisercostin.jedio.impl.LocationLike;
import org.raisercostin.jedio.impl.SimpleRelativeLocation;
import org.raisercostin.jedio.op.OperationContext;

@JsonDeserialize(using = Location.LocationDeserializer.class)
public interface Location {
  //interface LocationFinal extends Location, LocationLike<LocationFinal> {}

  static class LocationDeserializer extends JsonDeserializer<Location> {
    @Override
    public Location deserialize(@Nullable JsonParser p, @Nullable DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
      if (p == null) {
        return null;
      }
      String externalUrl = p.readValueAs(String.class);
      return Locations.location(externalUrl);
    }
  }

  @SuppressWarnings("unchecked")
  default <T extends LocationLike<T>> T as(Class<T> clazz) {
    return (T) this;
  }
  //
  //  @SuppressWarnings("unchecked")
  //  <R extends Location> R log();
}
