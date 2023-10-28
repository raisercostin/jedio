package org.jedio.feature;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

import lombok.ToString;

@ToString(callSuper = true)
public class DurationFeature extends GenericFeature<Duration> implements Feature<Duration> {
  public DurationFeature(String name, String description, Duration compileDefault, String propertyName,
      boolean realtime)
  {
    super(name, description, compileDefault, propertyName, realtime, false);
  }

  public int secondsInt() {
    return Math.toIntExact(value().getSeconds());
  }

  public boolean isExpired(Temporal lastUpdate) {
    return isExpired(lastUpdate, now());
  }

  private Instant now() {
    return ClockFeature.clock.nowInstant();
  }

  public boolean isExpired(OffsetDateTime lastUpdate, Instant now) {
    return isExpired(lastUpdate.toInstant(), now);
  }

  public boolean isExpired(Temporal lastUpdate, Temporal now) {
    if (lastUpdate instanceof OffsetDateTime && now instanceof Instant) {
      return isExpired((OffsetDateTime) lastUpdate, (Instant) now);
    }
    return lastUpdate != null && age(lastUpdate, now).compareTo(value()) >= 0;
  }

  private Duration age(Temporal lastUpdate, Temporal now) {
    if (lastUpdate instanceof OffsetDateTime && now instanceof Instant) {
      return age((OffsetDateTime) lastUpdate, (Instant) now);
    }
    return Duration.between(lastUpdate, now);
  }

  /**Faster since is using millis.*/
  public Duration ageIgnoringNano(Instant first, Instant second) {
    //return first.until(second, ChronoUnit.MILLIS);
    return Duration.ofMillis(ageInMillis(first.getEpochSecond(), second.getEpochSecond()));
  }

  public Duration age(OffsetDateTime lastUpdate) {
    return age(now(), lastUpdate);
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
  public int compareToTimeout(Instant now, OffsetDateTime lastUpdate) {
    return Long.compare(ageInMillis(now.toEpochMilli(), lastUpdate.toInstant().toEpochMilli()), value().toMillis());
  }
  private long ageInMillis(long now, long old) {
    return now - old;
  }
}