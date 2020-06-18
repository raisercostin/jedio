package org.raisercostin.jedio.op;

import org.raisercostin.jedio.ExistingLocation;
import org.raisercostin.jedio.ReferenceLocation;

@FunctionalInterface
public interface OperationListener {
  org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CopyOptions.class);
  OperationListener defaultListener = (event, exception, src, dst, args) -> {
    if (exception != null) {
      log.warn("copy {}: {} -> {} details:{}. Enable debug for stacktrace.", event, src, dst, args);
      log.debug("copy {}: {} -> {} details:{}. Error with stacktrace.", event, src, dst, args, exception);
    } else {
      log.info("copy {}: {} -> {} details:{}.", event, src, dst, args);
    }
  };

  void reportOperationEvent(CopyEvent event, Throwable exception, ExistingLocation src, ReferenceLocation dst,
      Object... args);
}
