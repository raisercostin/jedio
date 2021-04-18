package org.raisercostin.jedio;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.raisercostin.jedio.impl.LocationLike;

@JsonDeserialize(using = Location.LocationDeserializer.class)
@JsonSerialize(using = Location.LocationSerializer.class)
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

  static class LocationSerializer extends JsonSerializer<Location> {
    @Override
    public void serialize(@Nullable Location value, @Nullable JsonGenerator gen,
        @Nullable SerializerProvider serializers) throws IOException {
      Preconditions.checkNotNull(value);
      Preconditions.checkNotNull(gen);
      gen.writeString(Locations.toExternalUri(value));
    }
  }

  @SuppressWarnings("unchecked")
  default <T extends LocationLike<T>> T as(Class<T> clazz) {
    return (T) this;
  }
  //
  //  @SuppressWarnings("unchecked")
  //  <R extends Location> R log();

  String toExternalUri();
}
