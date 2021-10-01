package org.jedio.feature;

import java.util.Map;

import io.vavr.API;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.jedio.struct.RichIterable;

//TODO improve with config from properties and database
public class FeatureService {
  static final org.slf4j.Logger SERVICE_LOG = org.slf4j.LoggerFactory.getLogger(FeatureService.class);

  public static io.vavr.collection.Map<String, Feature<?>> features = API.SortedMap();

  public static void register(Feature<?> feature) {
    SERVICE_LOG.info("Register feature {}", feature.description());
    features = features.put(feature.name(), feature);
  }

  /**In a spring application the logger was changed. Loggers must be again reinitialized.*/
  public static void postConstruct() {
    SERVICE_LOG.info("FeatureService postConstruct ...");
    features.values().forEach(x -> x.postConstruct());
    SERVICE_LOG.info("FeatureService postConstruct done");
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> features() {
    return (Map<String, Object>) features.mapValues(x -> x.value()).toJavaMap();
  }

  @SuppressWarnings("unchecked")
  public static Tuple2<Map<String, Object>, RichIterable<Tuple2<String, Object>>> featuresOverride(
      Map<String, Object> overrides) {
    var parts = RichIterable
      .ofJava(overrides.entrySet())
      .map(x -> Tuple.of(x.getKey(), features.get(x.getKey()), x.getValue()))
      .partition(x -> x._2.isDefined());
    parts._1.forEach(x -> ((Feature<Object>) x._2.get()).setRuntimeValue(x._3));
    var ignored = parts._2.map(x -> Tuple.of(x._1, x._3)).memoizeJava();
    var all = (Map<String, Object>) features.mapValues(x -> x.runtimeValue().getOrNull()).toJavaMap();
    return Tuple.of(all, ignored);
  }
}
