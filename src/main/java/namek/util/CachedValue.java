package namek.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import io.vavr.collection.Iterator;
import org.raisercostin.nodes.RichConfig;

/**Drop in replacement to io.vavr.Lazy that additionally offers invalidation and propagation to dependent cached values.*/
//TODO: Config and the values could be reactive. On config change all other caches could be invalidated and recreated
//TODO: These would be a subset of features that will not be advertised there
public class CachedValue<T> {
  public static <T> CachedValue<T> of(T value) {
    return of(() -> value);
  }

  public static <T> CachedValue<T> of(Supplier<T> supplier) {
    return new CachedValue<>(supplier);
  }

  public static <T> CachedValue<T> of(Supplier<T> supplier, CachedValue<?>... parents) {
    return new CachedValue<>(supplier, parents);
  }

  @JsonIgnore
  private final Supplier<T> supplier;
  @JsonIgnore
  private volatile T value;
  @JsonIgnore
  private volatile boolean isValid;
  @JsonIgnore
  private List<CachedValue<?>> children;
  @JsonIgnore
  private CachedValue<?>[] parents;

  private CachedValue(Supplier<T> supplier, CachedValue<?>... parents) {
    this.supplier = supplier;
    this.value = null;
    this.parents = parents;
    watch(parents);
  }

  private void registerWatcher(CachedValue<?> watcher) {
    if (children == null) {
      children = new ArrayList<>();
    }
    children.add(watcher);
  }

  private void deregisterWatcher(CachedValue<?> watcher) {
    children.remove(watcher);
  }

  public CachedValue<T> invalidate() {
    this.value = null;
    this.isValid = false;
    if (children != null) {
      Iterator.ofAll(children).forEach(x -> x.invalidate());
    }
    return this;
  }

  public <CT> CachedValue<CT> child(Function<T, CT> supplier) {
    return new CachedValue<>(() -> supplier.apply(get()), this);
  }

  public CachedValue<String> child(String key, String defaultValue) {
    return child(c -> ((RichConfig) c).resolve(key, defaultValue));
  }

  @JsonValue
  public T get() {
    if (value == null || !isValid) {
      set(supplier.get());
    }
    return value;
  }

  public void set(T value) {
    invalidate();
    isValid = true;
    this.value = value;
  }

  /**
   * This will configure the value. The supplier will be called only if the cache is manualy invalidated.
   * Other values watching this will react.
   */
  public void setTillInvalidated(T value) {
    set(value);
    deregister();
  }

  private void watch(CachedValue<?>... parents) {
    if (parents != null) {
      Iterator.of(parents).forEach(x -> x.registerWatcher(this));
    }
  }

  private void deregister() {
    if (parents != null) {
      Iterator.of(parents).forEach(x -> x.deregisterWatcher(this));
    }
  }

  public T invalidateAndGet() {
    return invalidate().get();
  }

  @Override
  public String toString() {
    return "CachedValue[%s%s]".formatted(isValid ? "" : "dirty:", value == null ? null : value.toString());
  }
}
