package org.jedio;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.Preconditions;
import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.util.ReflectionUtils;

@Slf4j
public class NodeUtils {
  public static Object object(Object object, Object firstKey, Object... restKeys) {
    return nullableInternal(object, Iterator.of(firstKey).concat(Iterator.of(restKeys)), false, true, true);
  }

  public static String notNullString(Object object, Object... keys) {
    return Objects.toString(nullableInternal(object, Iterator.of(keys), false, true, true), "returnedNullString");
  }

  public static String notNullString(Object object, String... keys) {
    return Objects.toString(nullableInternal(object, Iterator.of(keys), false, true, true), "returnedNullString");
  }

  public static String nullableString(Object object, String firstKey, Object... restKeys) {
    return Objects.toString(
      nullableInternal(object, Iterator.<Object>of(firstKey).concat(Iterator.of(restKeys)), true, true, true), null);
  }

  public static String nullableOrInexistentString(Object object, String firstKey, Object... restKeys) {
    return Try
      .of(() -> (String) nullableInternal(object, Iterator.<Object>of(firstKey).concat(Iterator.of(restKeys)), true,
        true, true))
      .getOrNull();
  }

  public static String nullableString(Object object, Object... keys) {
    return Objects.toString(nullableInternal(object, Iterator.of(keys), true, true, true), null);
  }

  public static String nullableStringWithFallback(String fallbackValue, Object object, Object firstKey,
      Object... restKeys) {
    return Objects.toString(
      nullableInternal(object, Iterator.of(firstKey).concat(Iterator.of(restKeys)), true, true, true),
      fallbackValue);
  }

  /**FirstKey must exist (otherwise compilation error) and must not be an array by mistake.
   * The legitimate keys are String or Number.
   */
  public static <T> T notNull(Object object, String firstKey) {
    return nullableInternal(object, Iterator.<Object>of(firstKey), false, true, true);
  }

  /**FirstKey must exist (otherwise compilation error) and must not be an array by mistake.
   * The legitimate keys are String or Number.
   */
  public static <T> T notNull(Object object, Number firstKey, Object... restKeys) {
    return nullableInternal(object, Iterator.<Object>of(firstKey).concat(Iterator.of(restKeys)), false, true, true);
  }

  /**FirstKey must exist (otherwise compilation error) and must not be an array by mistake.
   * The legitimate keys are String or Number.
   */
  public static <T> T notNull(Object object, Object... keys) {
    return nullableInternal(object, Iterator.of(keys), false, true, true);
  }

  /**FirstKey must exist (otherwise compilation error) and must not be an array by mistake.
   * The legitimate keys are String or Number.
   */
  public static <T> T notNull(Object object, String... keys) {
    return nullableInternal(object, Iterator.of((Object[]) keys), false, true, true);
  }

  /**FirstKey must exist (otherwise compilation error) and must not be an array by mistake.
   * The legitimate keys are String or Number.
   */
  public static <T> T nullable(Object object, String firstKey) {
    return nullableInternal(object, Iterator.<Object>of(firstKey), true, true, true);
  }

  /**FirstKey must exist (otherwise compilation error) and must not be an array by mistake.
   * The legitimate keys are String or Number.
   */
  public static <T> T nullable(Object object, Number firstKey, Object... restKeys) {
    return nullableInternal(object, Iterator.<Object>of(firstKey).concat(Iterator.of(restKeys)), true, true, true);
  }

  /**FirstKey must exist (otherwise compilation error) and must not be an array by mistake.
   * The legitimate keys are String or Number.
   */
  public static <T> T nullable(Object object, Object... keys) {
    return nullableInternal(object, Iterator.of(keys), true, true, true);
  }

  /**FirstKey must exist (otherwise compilation error) and must not be an array by mistake.
   * The legitimate keys are String or Number.
   */
  public static <T> T nullable(Object object, String... keys) {
    return nullableInternal(object, Iterator.of((Object[]) keys), true, true, true);
  }

  public static <T> T nullableWithFallback(T fallbackValue, Object object, Object firstKey, Object... restKeys) {
    T res = nullableInternal(object, Iterator.of(firstKey).concat(Iterator.of(restKeys)), true, true, true);
    return res == null ? fallbackValue : res;
  }

  public static <T> T nullable(Object object, Iterator<Object> keys, boolean nullable, boolean caseInsensitive,
      boolean nullIfInvalid) {
    return nullableInternal(object, keys, nullable, caseInsensitive, nullIfInvalid);
  }

  public static <T> T nullableInternal(Object object, Iterator<Object> keys, boolean nullable,
      boolean caseInsensitive, boolean nullIfInvalid) {
    T value = (T) keys.foldLeft(object,
      (currentObject, key) -> getByKey(currentObject, key, nullable, caseInsensitive, nullIfInvalid));
    return value;
  }

  public static <T> T withValueOf(T node, Object keyHead, Object... keysTailAndValue) {
    return withValueOf(false, true, node, keyHead, keysTailAndValue);
  }

  /**
   * Is equivalent of node[key1][key2]...[keyN-1] = lastValue (actually last value from keysTailAndValue)
   *
   * @return the given node object if is mutable or another object if node is imutable.
   */
  public static <T> T withValueOf(boolean caseInsensitive, boolean nullIfInvalid, T node, Object keyHead,
      Object... keysTailAndValue) {
    // return Iterator.of(keys).foldLeft(node, (last, key) -> setByKey(last, key, value));
    List<Object> all = List.of(keyHead).appendAll(List.of(keysTailAndValue));
    return withValueOfInternal(node, all.dropRight(1), all.last(), caseInsensitive, nullIfInvalid);
  }

  /**
   * Is equivalent of node[key1][key2]...[keyN] = value
   *
   * @return the given node object if is mutable or another object if node is imutable.
   */
  @SuppressWarnings("unchecked")
  private static <T> T withValueOfInternal(@Nullable T node, Seq<Object> keys, Object value, boolean caseInsensitive,
      boolean nullIfInvalid) {
    if (keys.isEmpty()) {
      return (T) value;
    }
    @NonNull
    T node2 = Preconditions.checkNotNull(node);
    Object key = keys.head();
    if (keys.size() == 1) {
      return (T) setByKey(node2, key, value);
    }
    Object currentValue = withValueOfInternal(getByKey(node, key, false, caseInsensitive, nullIfInvalid), keys.tail(),
      value,
      caseInsensitive, nullIfInvalid);
    return (T) setByKey(node2, key, currentValue);
  }

  @SneakyThrows
  private static <T, V> Object setByKey(@NonNull T object, @Nullable Object key,
      V value) {
    if (object instanceof java.util.Map) {
      ((java.util.Map<Object, V>) object).put(key, value);
      return object;
    }
    if (object instanceof Map) {
      return ((Map) object).put(key, value);
    }
    if (object instanceof java.util.List) {
      ((java.util.List<Object>) object).set((Integer) Preconditions.checkNotNull(key), value);
      return object;
    }
    if (object instanceof Seq) {
      Seq<Object> seq = (Seq<Object>) object;
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
      field.setAccessible(true);
      field.set(object, value);
      return object;
    }
    throw new RuntimeException(
      String.format("Don't know how to set %s[%s] on object %s", object.getClass(), key, object));
  }

  private static <T extends @NonNull Object> @Nullable T getByKey(@Nullable Object object, Object key, boolean nullable,
      boolean caseInsensitive, boolean nullIfInvalid) {
    @Nullable
    T value = getByKeyNullable(object, key, caseInsensitive, nullIfInvalid);
    if (!nullable) {
      return Preconditions.checkNotNull(value, "%s is null on Object %s", key, object);
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  private static <T> @org.checkerframework.checker.nullness.qual.Nullable T getByKeyNullable(@Nullable Object object,
      Object key, boolean caseInsensitive, boolean nullIfInvalid) {
    // return BeanUtils.getValue(node, "rawNodes/adOrgs/0/_key");
    // return PropertyUtils.getProperty(node, key.toString());
    if (object == null) {
      return null;
    }
    if (object instanceof java.util.Map) {
      java.util.Map<Object, T> map = (java.util.Map<Object, T>) object;
      T res = map.get(key);
      if (res == null && caseInsensitive) {
        return Iterator.ofAll(map.entrySet())
          .find(entry -> entry.getKey().toString().equalsIgnoreCase(key.toString()))
          .map(entry -> entry.getValue())
          .getOrNull();
      }
      return res;
    }
    if (object instanceof Map) {
      Map<Object, T> map = (Map<Object, T>) object;
      T res = map.getOrElse(key, null);
      if (res == null && caseInsensitive) {
        return map.find(entry -> entry._1.toString().equalsIgnoreCase(key.toString()))
          .map(entry -> entry._2)
          .getOrNull();
      }
      return res;
    }
    if (object instanceof java.util.List) {
      return ((java.util.List<T>) object).get((Integer) key);
    }
    if (object instanceof Seq) {
      return ((Seq<T>) object).get((Integer) key);
    }
    if (object instanceof Option) {
      return getByKeyNullable(((Option<T>) object).get(), key, caseInsensitive, nullIfInvalid);
    }
    if (object instanceof Optional) {
      return getByKeyNullable(((Optional<T>) object).get(), key, caseInsensitive, nullIfInvalid);
    }
    if (object instanceof com.google.common.base.Optional) {
      return getByKeyNullable(((com.google.common.base.Optional<T>) object).get(), key, caseInsensitive, nullIfInvalid);
    }
    Field field = ReflectionUtils.findField(object.getClass(), (String) key);
    if (field != null) {
      field.setAccessible(true);
      return (T) field.get(object);
    }
    String msg = String.format("Don't know how to get %s[%s] on object %s", object.getClass(), key, object);
    if (nullIfInvalid) {
      log.warn(msg);
      return null;
    }
    throw new RuntimeException(msg);
  }
}
