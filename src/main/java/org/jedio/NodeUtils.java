package org.jedio;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import org.springframework.util.ReflectionUtils;

public class NodeUtils {
  public static Object object(Object object, Object firstKey, Object... restKeys) {
    return nullable(object, Iterator.of(firstKey).concat(Iterator.of(restKeys)), false);
  }

  public static String notNullString(Object object, Object firstKey, Object... restKeys) {
    return Objects.toString(nullable(object, Iterator.of(firstKey).concat(Iterator.of(restKeys)), false),
      "returnedNullString");
  }

  public static String nullableString(Object object, String firstKey, Object... restKeys) {
    return Objects.toString(nullable(object, Iterator.<Object>of(firstKey).concat(Iterator.of(restKeys)), true), null);
  }

  public static String nullableString(Object object, Object... keys) {
    return Objects.toString(nullable(object, Iterator.of(keys), true), null);
  }

  public static String nullableStringWithFallback(String fallbackValue, Object object, Object firstKey,
      Object... restKeys) {
    return Objects.toString(nullable(object, Iterator.of(firstKey).concat(Iterator.of(restKeys)), true), fallbackValue);
  }

  public static <T> T notNull(Object object, Object firstKey, Object... restKeys) {
    return nullable(object, Iterator.of(firstKey).concat(Iterator.of(restKeys)), false);
  }

  public static <T> T nullable(Object object, Object firstKey, Object... restKeys) {
    return nullable(object, Iterator.of(firstKey).concat(Iterator.of(restKeys)), true);
  }

  public static <T> T nullableWithFallback(T fallbackValue, Object object, Object firstKey, Object... restKeys) {
    T res = nullable(object, Iterator.of(firstKey).concat(Iterator.of(restKeys)), true);
    return res == null ? fallbackValue : res;
  }

  public static <T> T nullable(Object object, Iterator<Object> keys, boolean nullable) {
    T value = (T) keys.foldLeft(object, (currentObject, key) -> getByKey(currentObject, key, nullable));
    return value;
  }

  /**
   * Is equivalent of node[key1][key2]...[keyN-1] = lastValue (actually last value from keysTailAndValue)
   *@return the given node object if is mutable or another object if node is imutable.
   */
  public static <T> T withValueOf(T node, Object keyHead, Object... keysTailAndValue) {
    // return Iterator.of(keys).foldLeft(node, (last, key) -> setByKey(last, key, value));
    List<Object> all = List.of(keyHead).appendAll(List.of(keysTailAndValue));
    return withValueOfInternal(node, all.dropRight(1), all.last());
  }

  /**
   * Is equivalent of node[key1][key2]...[keyN] = value
   *@return the given node object if is mutable or another object if node is imutable.
   */
  private static <T> T withValueOfInternal(T node, Seq<Object> keys, Object value) {
    if (keys.isEmpty()) {
      return (T) value;
    }
    Object key = keys.head();
    if (keys.size() == 1) {
      return (T) setByKey(node, key, value);
    }
    Object currentValue = withValueOfInternal(getByKey(node, key, false), keys.tail(), value);
    return (T) setByKey(node, key, currentValue);
  }

  @SneakyThrows
  private static <T, V> Object setByKey(T object, Object key, V value) {
    if (object instanceof java.util.Map) {
      ((java.util.Map<Object, V>) object).put(key, value);
      return object;
    }
    if (object instanceof Map) {
      return ((Map) object).put(key, value);
    }
    if (object instanceof java.util.List) {
      ((java.util.List<Object>) object).set((Integer) key, value);
      return object;
    }
    if (object instanceof Seq) {
      Seq<Object> seq = ((Seq<Object>) object);
      if (key == null) {
        return seq.append(value);
      }
      int index = (Integer) key;
      if (seq.length() == index) {
        return seq.append(value);
      }
      return seq.update((Integer) key, value);
    }
    Field field = ReflectionUtils.findField(object.getClass(), (String) key);
    if (field != null) {
      field.set(object, value);
      return object;
    }
    throw new RuntimeException(
      String.format("Don't know how to set %s[%s] on object %s", object.getClass(), key, object));
  }

  private static <T> T getByKey(Object object, Object key, boolean nullable) {
    @Nullable
    T value = getByKeyNullable(object, key);
    if (!nullable) {
      Preconditions.checkNotNull(value, "%s is null on Object %s", key, object);
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  private static <T> T getByKeyNullable(Object object, Object key) {
    // return BeanUtils.getValue(node, "rawNodes/adOrgs/0/_key");
    // return PropertyUtils.getProperty(node, key.toString());
    if (object == null) {
      return null;
    }
    if (object instanceof java.util.Map) {
      return ((java.util.Map<Object, T>) object).get(key);
    }
    if (object instanceof Map) {
      return ((Map<Object, T>) object).getOrElse(key, null);
    }
    if (object instanceof java.util.List) {
      return ((java.util.List<T>) object).get((Integer) key);
    }
    if (object instanceof Seq) {
      return ((Seq<T>) object).get((Integer) key);
    }
    if (object instanceof Option) {
      return getByKeyNullable(((Option<T>) object).get(), key);
    }
    if (object instanceof Optional) {
      return getByKeyNullable(((Optional<T>) object).get(), key);
    }
    if (object instanceof com.google.common.base.Optional) {
      return getByKeyNullable(((com.google.common.base.Optional<T>) object).get(), key);
    }
    Field field = ReflectionUtils.findField(object.getClass(), (String) key);
    if (field != null) {
      field.setAccessible(true);
      return (T) field.get(object);
    }
    throw new RuntimeException(
      String.format("Don't know how to get %s[%s] on object %s", object.getClass(), key, object));
  }
}