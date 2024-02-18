package org.raisercostin.jedio.impl;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.raisercostin.jedio.Location;
import org.raisercostin.jedio.op.OperationContext;

public interface LocationLike<SELF extends LocationLike<SELF>> extends Location {
  default SELF log() {
    return createWithContext(context().withReport(true));
  }

  default OperationContext context() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default @NonNull SELF createWithContext(@NonNull OperationContext withContext) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  //
  //  default SELF create(Object... args) {
  //    return (SELF) this;
  //  }

  default @NonNull SELF create(Object args) {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
