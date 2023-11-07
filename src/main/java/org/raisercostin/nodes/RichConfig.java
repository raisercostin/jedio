package org.raisercostin.nodes;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigResolveOptions;
import com.typesafe.config.ConfigSyntax;
import io.vavr.collection.Iterator;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import namek.util.RichString;
import org.jedio.RichThrowable;
import org.jedio.regex.RichRegex;
import org.raisercostin.jedio.Locations;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.nodes.impl.HoconNodes;

/**
 * There are two config states: original and resolved.
 * To understand where the values are inherited from you need the original.
 * To understand what the values are you need the resolved. Ideally you would have the original/formula as comment or as
 * an artificial key added like `prop1` and `prop1.formula`
 *
 * https://docs.spring.io/spring-boot/docs/1.2.3.RELEASE/reference/html/boot-features-external-config.html
 */
@Slf4j
public class RichConfig {

  /**@deprecated config might be read from properties that do not use substitutions like in spring.*/
  @Deprecated
  public static RichConfig from(Config config) {
    return new RichConfig(config);
  }

  public static Config fixSubstitutions(Config config) {
    String content = toReport(config, false);
    String transformedContent = Arrays.stream(content.split("\n"))
      .map(line -> {
        //Also the properties will not be "escaped" if they contain : inside
        String newLine = RichRegex.replaceAll(line, "^([^=]+=)(\"[^$]*)(\\$\\{[^}:]+\\})([^}]*\")$", "$1$2\"$3\"$4");
        if (line != newLine) {
          log.warn("Line was processed to understand one substitution in it:\nfrom: {}\nto:   {}\n", line.trim(),
            newLine.trim());
        }
        return newLine;
      })
      .collect(Collectors.joining("\n"));
    try {
      return Nodes.hocon.toConfigFromHocon(transformedContent);
    } catch (Exception e) {
      throw new RuntimeException("%s on content \n---\n%s\n---\n".formatted(e.getMessage(), content), e);
    }
  }

  private static String toReport(Config config, boolean jsonNotHocon) {
    return config.resolve(ConfigResolveOptions.defaults().setUseSystemEnvironment(false).setAllowUnresolved(true))
      .root()
      .render(
        ConfigRenderOptions.defaults()
          .setOriginComments(true)
          .setComments(true)
          .setFormatted(true)
          .setJson(jsonNotHocon));
  }

  public static <T> T readConfig(String location, String prefix, Class<T> clazz) {
    return readConfigFromLocation(Locations.readable(location), prefix, clazz);
  }

  public static <T> T readConfigFromLocation(ReadableFileLocation location, String prefix, Class<T> clazz) {
    String content = RichThrowable.tryWithSuppressed(() -> location.readContentSync(), "Reading from %s", location);
    return RichThrowable.tryWithSuppressed(
      () -> {
        Config config = loadWithPropertiesAndEnvOverrides(
          toUnresolvedConfig(location.toString(), content, ConfigSyntax.CONF)
        //          Nodes.hocon
        //          //.withUseSystemEnvironment(true)
        //          //.withUseSystemProperties(true)
        //          .withIgnoreUnknwon()
        //          .toConfigFromHocon(content)
        ).config;
        //Convert to prop to filter by prefix
        String contentProps = HoconNodes.hoconToProperties(config);
        T conf = Nodes.prop
          .withIgnoreUnknwon()
          .withPrefix(prefix)
          .toObject(contentProps, clazz);
        return conf;
      }, "When reading from [%s] class %s: [%s]", location, clazz, RichString.abbreviateMiddle(content, 1000));
  }

  private static Config toUnresolvedConfig(String originDescription, String content, ConfigSyntax configSyntax) {
    Config config = ConfigFactory
      .parseString(content,
        ConfigParseOptions.defaults().setSyntax(configSyntax).setOriginDescription(originDescription));
    //if (dump) {
    //log.info("Config from {} with fallback to properties and environment: {}", toReport(config));
    //}
    return config;
  }

  public static void invalidateCaches() {
    ConfigFactory.invalidateCaches();
  }

  public static RichConfig systemEnvironmentAndProperties() {
    return new RichConfig(Nodes.hocon
      .withUseSystemEnvironment(true)
      .withUseSystemProperties(true)
      .toConfigFromHocon("")
      .resolve());
  }

  /**Loads the config and resolves with in this order:
   * - properties (java passed -Dprop=value)
   * - environment variables
   * - resource
   */
  public static RichConfig load(String resourceBasename) {
    Config resourceConfig = ConfigFactory.load(resourceBasename,
      ConfigParseOptions.defaults().setAllowMissing(false),
      ConfigResolveOptions.noSystem().setAllowUnresolved(true));
    return loadWithPropertiesAndEnvOverrides(resourceConfig);
  }

  private static RichConfig loadWithPropertiesAndEnvOverrides(Config resourceConfig) {
    return new RichConfig(combine(resourceConfig, true, true, null));
  }

  public final Config config;
  public volatile boolean strictResolve = true;

  private RichConfig(Config config) {
    log.info("Loading config file from [" + config.origin() + "]");
    //Since config might have come from properties is best to dump to properties/fix wrong quotation and parse back?
    this.config = fixSubstitutions(config.resolve(ConfigResolveOptions.noSystem().setAllowUnresolved(false)));
  }

  public <T> T getValueOrElse(String path, T elseValue, Class<T> clazz) {
    String propertyNameCorrected = path != null && path.isEmpty() ? null : path;
    T result = propertyNameCorrected != null && config.hasPath(propertyNameCorrected)
        ? (T) Try.of(() -> convert(config.getAnyRef(propertyNameCorrected), clazz))
          .onFailure(
            x -> x.addSuppressed(new RuntimeException("while trying to get [" + propertyNameCorrected + "]")))
          .get()
        : null;
    if (result == null) {
      if (elseValue instanceof String valueString) {
        elseValue = (T) resolve(valueString).get();
      }
      result = elseValue;
    }
    log.info("property {}={} fallback:{} type:{}", path, result, elseValue, clazz);
    return result;
  }

  public String resolve(String content, String defaultValue) {
    return resolve(content).getOrElse(defaultValue);
  }

  public String resolveBoth(String content, String defaultValue) {
    return resolve(content).getOrElse(resolve(defaultValue).get());
  }

  public Try<String> resolve(String content) {
    String template = "template=" + escapeOneValue(content);
    Config placeholderConfig = Try.of(() -> ConfigFactory.parseString(template,
      ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF)))
      .recoverWith(x -> Try.failure(new RuntimeException("When parsing [" + template + "]", x)))
      .get();
    Config resolvedConfig = placeholderConfig.withFallback(config)
      .resolve(ConfigResolveOptions.noSystem().setUseSystemEnvironment(false).setAllowUnresolved(false));
    String resolvedString = resolvedConfig.getString("template");
    if (strictResolve) {
      if (p.matcher(resolvedString).find()) {
        return Try.failure(new RuntimeException(
          "Value [%s] not resolved.".formatted(resolvedString)));
      }
    }
    return Try.success(resolvedString);
  }

  /**Values from json or properties must be changed to be seen by HOCON.
   */
  private static String escapeOneValue(String content) {
    if (content.startsWith("\"") && content.endsWith("\"")) {
    } else {
      content = "\"" + content + "\"";
    }
    return escapeSubstitutionsInJson(content);
  }

  @SuppressWarnings("unchecked")
  private <T> T convert(Object value, Class<T> clazz) {
    if (value == null) {
      return null;
    }
    if (clazz.isAssignableFrom(value.getClass())) {
      return (T) value;
    }
    String stringValue = Nodes.json.toString(value);
    return convertFromString(stringValue, clazz);
  }

  private <T> T convertFromString(String stringValue, Class<T> clazz) {
    return Nodes.json.toObject(stringValue, clazz);
  }

  public String getStringOrElse(String path, String elseValue) {
    return getValueOrElse(path, elseValue, String.class);
  }

  public RichConfig dump() {
    return dump("");
  }

  public RichConfig dump(String title) {
    log.info("RichConfig[{}]:\n-----\n{}-----\n", title, toJson());
    return this;
  }

  public static RichConfig fromHocon(String content) {
    return new RichConfig(Nodes.hocon.toConfigFromHocon(content));
  }

  public static RichConfig fromJson(String content) {
    String escaped = escapeSubstitutionsInJson(content);
    return new RichConfig(Nodes.hocon.toConfigFromHocon(escaped));
  }

  private static Pattern p = Pattern.compile("(\"?(\\$\\{[^}]+\\})\"?)");

  public static String escapeSubstitutionsInJson(String content) {
    //    if (content.startsWith("\"") && content.endsWith("\"")) {
    //    } else {
    //      content = "\"" + content + "\"";
    //    }
    Matcher matcher = p.matcher(content);
    if (!matcher.find()) {
      return content;
    }
    String content2 = content;
    String escaped = matcher.replaceAll(mr -> {
      boolean startsWithQuote = mr.start(0) >= 0 && content2.charAt(mr.start(0)) == '"';
      boolean endsWithQuote = mr.end(0) - 1 < content2.length() && content2.charAt(mr.end(0) - 1) == '"';
      return (startsWithQuote ? "" : "\"") + "$2" + (endsWithQuote ? "" : "\"");
    });
    return escaped;
  }

  public static RichConfig fromJsonNoSubstitutions(String content) {
    return new RichConfig(Nodes.hocon.toConfigFromJson(content));
  }

  public static RichConfig fromProperties(String content) {
    String hoconContent = propertiesToHoconFormat(content);
    return fromHocon(hoconContent);
  }

  public static RichConfig fromPropertiesWithHoconEscaping(String content) {
    return fromHocon(content);
  }

  public static RichConfig fromPropertiesNoSubstitutions(String content) {
    //TODO change toConfigFromProperties
    //return new RichConfig(Nodes.hocon.toConfigFromHocon(content));
    return new RichConfig(toConfig(content, ConfigSyntax.PROPERTIES, false, false, false, ""));
  }

  private static Config toConfig(String content, ConfigSyntax configSyntax, boolean useSystemProperties,
      boolean useSystemEnvironment, boolean dump, String originDescription) {
    Config config = ConfigFactory
      .parseString(content,
        ConfigParseOptions.defaults().setSyntax(configSyntax).setOriginDescription(originDescription));

    config = combine(null, useSystemEnvironment, useSystemProperties, config);
    config = config
      .resolve(ConfigResolveOptions.noSystem().setUseSystemEnvironment(false).setAllowUnresolved(false));
    if (dump) {
      String newConfig = config.root()
        .render(
          ConfigRenderOptions.defaults().setOriginComments(true).setComments(true).setFormatted(true).setJson(false));
      log.info("Config from {} with fallback to properties and environment: {}", originDescription, newConfig);
    }
    return config;
  }

  /**
   * Configs in order: base config (usually internal to app), system env, properties (in command line), dynamic (in an external file)
   */
  private static Config combine(Config configBaseUsuallyClasspath, boolean useSystemEnvironment,
      boolean useSystemProperties,
      Config configDynamicUsuallyExternalFile) {
    Config config = configDynamicUsuallyExternalFile;
    if (useSystemProperties) {
      config = config == null ? ConfigFactory.systemProperties()
          : config.withFallback(ConfigFactory.systemProperties());
    }
    if (useSystemEnvironment) {
      config = config == null ? ConfigFactory.systemEnvironment()
          : config.withFallback(ConfigFactory.systemEnvironment());
    }
    if (configBaseUsuallyClasspath != null) {
      config = config == null ? configBaseUsuallyClasspath
          : config.withFallback(configBaseUsuallyClasspath);
    }
    return config;
  }

  public static String propertiesToHoconFormat(String content) {
    return propertiesToHoconFormatWithRegexp(content);
  }

  public static String propertiesToHoconFormat1(String content) {
    Properties properties = new Properties();
    try (StringReader stringReader = new StringReader(content)) {
      properties.load(stringReader);
    } catch (IOException e) {
      throw org.jedio.RichThrowable.nowrap(e);
    }
    ImmutableMap<String, String> all = Maps.fromProperties(properties);
    StringBuilder sb2 = Iterator.ofAll(all.entrySet())
      .foldLeft(new StringBuilder(),
        (sb, entry) -> sb.append(entry.getKey())
          .append("=")
          .append(escapeOneValue(entry.getValue().toString()))
          .append("\n"));
    //    ImmutableMap<String, String> all = Maps.fromProperties(properties);
    //    StringSubstitutor subst = new StringSubstitutor(all);
    //    Map<Object, Object> map = Iterator.ofAll(all.entrySet())
    //      .toMap(x -> Tuple.of(x.getKey(), subst.replace(x.getValue())));
    //    System.out.println(map.mkString("\n"));
    String hoconContent = sb2.toString();
    return hoconContent;
  }

  public static String propertiesToHoconFormatWithRegexp(String properties) {
    return Arrays.stream(properties.split("\n"))
      .map(line -> {
        String[] parts = line.split("=", 2);
        String value = parts[1].replaceAll("(\\$\\{[^}]+\\})", "\"$1\"");
        return parts[0] + "=\"" + value + "\"";
      })
      .collect(Collectors.joining("\n"));
  }

  public String toJson() {
    return toReport(config, true);
  }

  public String toHocon() {
    return toReport(config, false);
  }
}
