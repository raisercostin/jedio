package org.jedio.feature;

import static java.time.temporal.ChronoUnit.NANOS;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

import lombok.ToString;

@ToString(callSuper = true)
public class DurationFeature extends GenericFeature<Duration> implements Feature<Duration> {
  public DurationFeature(String name, Duration compileDefault, String propertyName, boolean realtime) {
    super(name, compileDefault, propertyName, realtime, false);
  }

  public int secondsInt() {
    return Math.toIntExact(value().getSeconds());
  }

  public boolean isExpired(Temporal lastUpdate) {
    return isExpired(lastUpdate, ClockFeature.clock.nowOffsetDateTime());
  }

  public boolean isExpired(Temporal lastUpdate, Temporal now) {
    return lastUpdate != null && age(lastUpdate, now).compareTo(value()) >= 0;
  }

  private Duration age(Temporal lastUpdate, Temporal now) {
    return Duration.between(lastUpdate, now);
  }

  /**Faster since is using millis.*/
  public Duration ageIgnoringNano(Instant first, Instant second) {
    //return first.until(second, ChronoUnit.MILLIS);
    return Duration.ofMillis(ageInMillis(first.getEpochSecond(), second.getEpochSecond()));
  }

  public Duration age(OffsetDateTime lastUpdate) {
    return ClockFeature.clock.ageFrom(lastUpdate);
  }

  public boolean isEnabled() {
    return value().compareTo(Duration.ZERO) > 0;
  }

  @Override
  public Duration convertFromString(String stringValueOrAction, Class<Duration> clazz) {
    if (!clazz.equals(Boolean.class) && stringValueOrAction.equalsIgnoreCase("false")) {
      return Duration.ZERO;
    }
    if (!clazz.equals(Boolean.class) && stringValueOrAction.equalsIgnoreCase("true")) {
      if (oldRuntimeValue == null) {
        return null;
      }
      return oldRuntimeValue.getOrNull();
    }
    return super.convertFromString(stringValueOrAction, clazz);
  }

  public void deleteRuntimeValue() {
    setRuntimeValue(null);
  }

  public int compareToTimeout(Clock clock, Duration duration, OffsetDateTime lastUpdate) {
    //    return timestamp == null ? ChronoUnit.FOREVER.getDuration()
    //        : Duration.between(timestamp, ClockFeature.clock.nowOffsetDateTime());
    return Long.compare(ageInMillis(clock.millis(), lastUpdate.toInstant().toEpochMilli()), duration.toMillis());
  }

  private long ageInMillis(long now, long old) {
    return now - old;
  }
}