package org.jedio.feature;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**This "feature" controlls logging for a specific prefix. For now is switching from WARN(disabled) to INFO(enabled).
 * In the future the level could be selected from a list. For now the UI doesn't have this.
 */
@ToString(callSuper = true)
public class LoggerFeature extends GenericFeature<Boolean> implements Feature<Boolean> {
  public static LoggerFeature loggerFeature(String name, String description, Boolean compileDefault,
      String propertyName,
      boolean realtime, Class<?> clazz) {
    return new LoggerFeature(name, description, compileDefault, propertyName, realtime,
      org.slf4j.LoggerFactory.getLogger(clazz),
      Level.INFO, Level.DEBUG);
  }

  public static LoggerFeature loggerFeature(String name, String description, Boolean compileDefault,
      String propertyName,
      boolean realtime, String loggerName) {
    return new LoggerFeature(name, description, compileDefault, propertyName, realtime, loggerName, Level.INFO,
      Level.DEBUG);
  }

  public static LoggerFeature loggerFeature(String name, String description, Boolean compileDefault,
      String propertyName,
      boolean realtime, String loggerName, Level inactiveLevel, Level activeLevel) {
    return new LoggerFeature(name, description, compileDefault, propertyName, realtime, loggerName, inactiveLevel,
      activeLevel);
  }

  public Logger log;
  private Level activeLevel;
  private Level inactiveLevel;

  private LoggerFeature(String name, String description, Boolean compileDefault, String propertyName, boolean realtime,
      String loggerName,
      Level inactiveLevel, Level activeLevel)
  {
    this(name, description, compileDefault, propertyName, realtime, org.slf4j.LoggerFactory.getLogger(loggerName),
      inactiveLevel, activeLevel);
  }

  private LoggerFeature(String name, String description, Boolean compileDefault, String propertyName, boolean realtime,
      Logger log,
      Level inactiveLevel, Level activeLevel)
  {
    super(name, description, compileDefault, propertyName, realtime, false);
    this.inactiveLevel = inactiveLevel;
    this.activeLevel = activeLevel;
    this.log = log;
    registerAction((from, to) -> {
      switchLogger(to);
    });
    switchLogger(value());
  }

  @Override
  public void postConstruct() {
    switchLogger(value());
  }

  private void switchLogger(Boolean to) {
    //on disable switch to warn
    //on disable switch to warn
    Level level = to == true ? activeLevel : inactiveLevel;
    switchLevel(level);
  }

  public void switchLevel(Level level) {
    change(log, level);
  }

  private static void change(Logger log2, Level level) {
    FeatureService.SERVICE_LOG.info("switchLogger {} to {}", log2, level);
    String loggerName = log2.getName();
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger logger = loggerName.equalsIgnoreCase("root") ? loggerContext.getLogger(loggerName)
        : loggerContext.exists(loggerName);
    if (logger != null) {
      logger.setLevel(level);
    }
  }

  /**Create a sub-logger. This should also be disabled by feature but keeps it's separate identity.*/
  public Logger log(Class<?> clazz) {
    return org.slf4j.LoggerFactory.getLogger(name + "." + clazz.getName());
  }

  public boolean isInfoEnabled() {
    return log.isInfoEnabled();
  }

  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  public void enable() {
    setRuntimeValue(true);
  }
}