package org.raisercostin.jedio.impl;

import lombok.Data;
import org.raisercostin.jedio.RelativeLocation;

@Data
public class SimpleRelativeLocation implements RelativeLocation, LocationLike<SimpleRelativeLocation> {
  private final String location;

  @Override
  public String relativePath() {
    return this.location;
  }
}
