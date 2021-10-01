package org.jedio.feature;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import lombok.ToString;

@ToString(callSuper = true)
public class ClockFeature extends GenericFeature<Clock> implements Feature<Clock> {
  public static ClockFeature clock = ClockFeature.create("clock", Clock.systemDefaultZone(), null, true, true);

  public static ClockFeature create(String name, Clock compileDefault, String propertyName,
      boolean realtime, boolean runtimeReadonly) {
    return new ClockFeature(name, compileDefault, propertyName, realtime, runtimeReadonly);
  }

  public ClockFeature(String name, Clock compileDefault, String propertyName, boolean realtime,
      boolean runtimeReadonly)
  {
    super(name, compileDefault, propertyName, realtime, runtimeReadonly);
  }

  public Instant nowInstant() {
    //return value().instant();
    return Instant.now(value());
  }

  public OffsetDateTime nowOffsetDateTime() {
    return OffsetDateTime.now(value());
  }

  @Override
  public Clock convert(Object value) {
    return (Clock) value;
  }

  public Duration ageFrom(OffsetDateTime timestamp) {
    return timestamp == null ? ChronoUnit.FOREVER.getDuration()
        : Duration.between(timestamp, ClockFeature.clock.nowOffsetDateTime());
  }
}