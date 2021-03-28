package org.raisercostin.jedio;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.checkerframework.checker.nullness.qual.Nullable;

@JsonDeserialize(using = Location.LocationDeserializer.class)
public interface Location {
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
}
