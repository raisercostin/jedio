package org.jedio;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Vector;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.execchain.RequestAbortedException;

/** Responsibility: log stuff but not for so many times if is an often warn.
 * - warn - a problem that is temporary and could be recovered but is rare, normally enabled
 * - info - some information that is rare, normally enabled (catalog updates, systems starts, cleanups, etc)
 * - debug - some information that is not rare (match updates, user/api interactions)
 *
 *  Notions
 *  - rare - is not dependent on how long you run or how much processing you're doing
 *  */
@Slf4j
public class Audit {
  public static class AuditException extends RuntimeException {
    private static final long serialVersionUID = -1727380682618906991L;
    public final String formatter;
    public final Object[] args;

    public AuditException(Throwable cause, String formatter, Object... args) {
      super(String.format(formatter, args), cause);
      this.formatter = formatter;
      this.args = args;
    }

    public AuditException(String formatter, Object... args) {
      super(String.format(formatter, args));
      this.formatter = formatter;
      this.args = args;
    }
  }

  @Data
  @Setter(AccessLevel.NONE)
  @Getter(AccessLevel.NONE)
  @AllArgsConstructor
  public static class Situation {
    public String id;
    public volatile int counter;
    public Object[] values;
    public boolean isWarn;

    public Situation increased() {
      counter++;
      return this;
    }
  }

  private static volatile boolean askedSituations = false;
  //private static AtomicLongMap<String> all = AtomicLongMap.create();
  private static LoadingCache<String, Long> all = CacheBuilder.newBuilder()
    .maximumSize(100000)
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .build(new CacheLoader<String, Long>()
      {
        @Override
        public Long load(String key) throws Exception {
          return 0L;
        }
      });

  private static Long getAndIncrement(String situationId) {
    return all.asMap().compute(situationId, (key, value) -> {
      return value == null ? 0L : value + 1;
    });
  }

  private static Map<String, Situation> situations = HashMap.empty();
  private static long DEFAULT_MAX_COUNTER = 10000000;

  public static void info(Throwable throwable, String format, Object... values) {
    warn(DEFAULT_MAX_COUNTER, false, throwable, format, values);
  }

  public static void info(String format, Object... values) {
    warn(DEFAULT_MAX_COUNTER, false, null, format, values);
  }

  public static void warn(Throwable throwable, String format, Object... values) {
    warn(DEFAULT_MAX_COUNTER, true, throwable, format, values);
  }

  public static void warn(String format, Object... values) {
    warn(DEFAULT_MAX_COUNTER, true, null, format, values);
  }

  public static Throwable warnAndRetrhrowIfNotKnown(String format, Throwable throwable, Object... values) {
    boolean knownException = warn(DEFAULT_MAX_COUNTER, true, throwable, format, format, values);
    if (!knownException) {
      return ExceptionUtils.nowrap(throwable, format, values);
    }
    return throwable;
  }

  /**@return knownException*/
  private static boolean warn(long maxCounter, boolean isWarn, Throwable throwable,
      String formatter,
      Object... args) {
    if (throwable instanceof AuditException) {
      formatter = ((AuditException) throwable).formatter;
      args = ((AuditException) throwable).args;
    }
    final Object[] finalArgs = args;

    try {
      Preconditions.checkArgument(
        args == null || args.length == 0 || !Throwable.class.isInstance(args[args.length - 1]),
        "Throwable should be the first value passed. Last value was that throwable %s", (Object) args);

      boolean knownException = knownThrowable(throwable, formatter);
      if (throwable instanceof java.lang.AssertionError) {
        throwable = new RuntimeException(
          "Maybee!!!!!! co.paralleluniverse.fibers.SuspendExecution: Oops. Forgot to instrument a method. Run your program with -Dco.paralleluniverse.fibers.verifyInstrumentation=true to catch the culprit!",
          throwable);
      }

      String situationIdOrFormat = formatter;
      Preconditions.checkArgument(!Strings.isNullOrEmpty(formatter), "Message is empty");
      if (askedSituations) {
        situations = situations.computeIfPresent(situationIdOrFormat, (key, value) -> value.increased())._2;
        situations = situations.computeIfAbsent(situationIdOrFormat,
          x -> new Situation(situationIdOrFormat, 1, finalArgs, isWarn))._2;
      }

      Long counter = getAndIncrement(situationIdOrFormat);
      if (shouldLog(maxCounter, counter)) {
        String message2 = null;
        if (log.isDebugEnabled() || log.isInfoEnabled() || log.isWarnEnabled()) {
          message2 = String.format("#audit>" + counter + " times: " + formatter, finalArgs);
          message2 = StringUtils.truncate(message2, 1000);
        }

        if (throwable != null) {
          if (knownException) {
            if (isWarn) {
              if (log.isWarnEnabled()) {
                log.warn(message2 + "> " + throwable.getMessage() + ". Enable debug to see the stacktrace.");
              }
            } else {
              if (log.isInfoEnabled()) {
                log.info(message2 + "> " + throwable.getMessage() + ". Enable debug to see the stacktrace.");
              }
            }
            log.debug(message2, throwable);
          } else {
            if (isWarn) {
              log.warn(message2, throwable);
            } else {
              log.info(message2, throwable);
            }
          }
        } else {
          if (isWarn) {
            log.warn(message2);
          } else {
            log.info(message2);
          }
        }
      }
      return knownException;
    } catch (Throwable e) {
      log.warn("Really bad exception for formatter [{}] arguments {}", formatter,
        finalArgs == null ? null : Vector.of(finalArgs), e);
    }
    return false;
  }

  private static boolean shouldLog(long maxCounter, Long x) {
    if (x > maxCounter) {
      return false;
    }
    if (x < 3) {
      return true;
    }
    if (x < 100) {
      return x % 10 == 0;
    }
    if (x < 1000) {
      return x % 100 == 0;
    }
    if (x < 10000) {
      return x % 1000 == 0;
    }
    if (x < 100000) {
      return x % 10000 == 0;
    }
    if (x < 1000000) {
      return x % 100000 == 0;
    }
    return x % 100000 == 0;
  }

  private static boolean knownThrowable(Throwable throwable, String format) {
    if (throwable == null) {
      return true;
    }
    if (throwable instanceof AuditException) {
      return true;
    }
    if (throwable.getCause() != null) {
      // cannot be known since was generated by something else, maybe a NullPointerException??? huh???
      return false;
    }

    if (format.equals("Content could not be parsed to %s . Content saved in %s")) {
      return true;
    }
    if (throwable.getMessage() != null && throwable.getMessage().startsWith("revobet-knwon> ")) {
      return true;
    }
    boolean knownException = false;
    if (throwable.getCause() != null && throwable.getCause().getMessage().startsWith("Couldn't find match ")) {
      return true;
    }
    if (//throwable instanceof ForbiddenCallException &&
    throwable.getMessage() != null && throwable.getMessage()
      .contains(
        "Unauthorized/Forbidden call FiberUrlLocation2(useCircuitBreaker=false, url=http://inplay.goalserve.com/")) {
      return true;
    }
    if (throwable instanceof java.net.UnknownHostException && throwable.getMessage().equals("api.betsapi.com")) {
      knownException = true;
    } else if (throwable instanceof IOException && throwable.getMessage().contains("Socket closed")) {
      knownException = true;
    } else if (throwable instanceof java.net.ConnectException
        && throwable.getMessage().startsWith("Timeout connecting to [api.betsapi.com")) {
      knownException = true;
    } else if (throwable instanceof IOException
        && throwable.getMessage().startsWith("An existing connection was forcibly closed by the remote host")
        && throwable.getCause() == null) {
      knownException = true;
    } else if (throwable.getClass().toString().endsWith("HighPerfUrlLocation2$InvalidHttpResponse")) {
      knownException = true;
    } else if (throwable.getClass().toString().endsWith("FiberUrlLocation2$InvalidHttpResponse")) {
      knownException = true;
    } else if (throwable instanceof SocketException && throwable.getMessage().equals("Socket closed")) {
      knownException = true;
    } else if (throwable instanceof RequestAbortedException && throwable.getMessage().equals("Request aborted")) {
      knownException = true;
    } else if (throwable instanceof IllegalArgumentException
        && throwable.getMessage() != null
        && throwable.getMessage().startsWith("Invalid over/under.")) {
      knownException = true;
    }
    return knownException;
    // TODO: If log contains
    // is hogging the CPU or blocking a thread.
    // is blocking a thread
    // WARNING: fiber Fiber@10000252[task: ParkableForkJoinTask@18e14d84(Fiber@10000252), target:
    // co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable@53c79b54, scheduler:
    // co.paralleluniverse.fibers.FiberForkJoinScheduler@50f56b5b] is blocking a thread
    // (Thread[ForkJoinPool-fiber-url-worker-9,5,main]).
    // -Dco.paralleluniverse.fibers.detectRunawayFibers=false
  }

  public static List<Situation> situations(boolean askedSituationsParam, boolean onlyWarns) {
    askedSituations = askedSituationsParam;
    if (onlyWarns) {
      return knownSituations(askedSituationsParam);
    } else {
      return situations.values().sortBy(x -> -x.counter).toList();
    }
  }

  private static List<Situation> knownSituations(boolean askedSituationsParam) {
    askedSituations = askedSituationsParam;
    return situations.values().filter(x -> !x.isWarn).sortBy(x -> -x.counter).toList();
  }

  public static RuntimeException throwAuditException(String formatter, Object... args) {
    throw auditException(formatter, args);
  }

  public static RuntimeException throwAuditException(Throwable e, String formatter, Object... args) {
    throw auditException(e, formatter, args);
  }

  public static RuntimeException auditException(String formatter, Object... args) {
    return new AuditException(formatter, args);
  }

  public static RuntimeException auditException(Throwable e, String formatter, Object... args) {
    return new AuditException(e, formatter, args);
  }
}
