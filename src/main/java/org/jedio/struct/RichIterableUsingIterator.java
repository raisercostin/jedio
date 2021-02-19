package org.jedio.struct;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.introspect.TypeResolutionContext.Empty;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction0;
import io.vavr.Function0;
import io.vavr.Function1;
import io.vavr.Lazy;
import io.vavr.PartialFunction;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.Value;
import io.vavr.collection.Array;
import io.vavr.collection.CharSeq;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.Iterator;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.Map;
import io.vavr.collection.PriorityQueue;
import io.vavr.collection.Queue;
import io.vavr.collection.Seq;
import io.vavr.collection.SortedMap;
import io.vavr.collection.SortedSet;
import io.vavr.collection.Traversable;
import io.vavr.collection.Tree;
import io.vavr.collection.Tree.Node;
import io.vavr.collection.Vector;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;
import org.jedio.LazyToString;

//TODO Couldn't make it work for CrudRepositories
//  @SuppressWarnings("rawtypes")
//  @Component
//  public class ToRichIterableConverter implements Converter<Iterable, RichIterable> {
//    @SuppressWarnings("unchecked")
//    @Override
//    public RichIterable convert(Iterable source) {
//      return RichIterable.fromJava(source);
//    }
//  }

public class RichIterableUsingIterator<T> implements RichIterable<T> {
  private final String operation;
  private final RichIterableUsingIterator<?> origin;
  private transient final Object[] params;
  private final Lazy<? extends Iterable<T>> iterableLazy;
  private transient final AtomicInteger iterated;
  private transient int maxIterations = 1;
  private transient String lastOperation;
  private transient Option<RuntimeException> lastOperationPlace;
  private boolean debugEnabled = false;

  public RichIterableUsingIterator(String operation, RichIterableUsingIterator<?> origin, Iterable<T> iterable,
      Object... params)
  {
    this.operation = operation;
    this.origin = origin;
    this.params = params;
    this.iterableLazy = Lazy.of(() -> iterable);
    this.iterated = new AtomicInteger(0);
  }

  public RichIterableUsingIterator(String operation, RichIterableUsingIterator<?> origin, Lazy<List<T>> iterableLazy,
      Object... params)
  {
    this.operation = operation;
    this.origin = origin;
    this.params = params;
    this.iterableLazy = iterableLazy;
    this.iterated = new AtomicInteger(0);
  }

  @Override
  public boolean isCollection() {
    Iterable<T> iterable = iterableLazy.get();
    if (iterable instanceof Collection) {
      return true;
    }
    if (iterable instanceof Value) {
      if (iterable instanceof Iterator) {
        return false;
      } else {
        return true;
      }
    }
    Preconditions.checkArgument(
      origin != null || this.operation.equals("concatAll") || this.operation.equals("ofIterable"),
      "Origin should not be null when operation is [%s] and iterable is %s. Iterable was generated from an origin since this is not a collection.",
      this.operation, iterable.getClass().getName());
    return false;
  }

  @Override
  public boolean isTraversableAgain() {
    //return iterator("isTraversableAgain",true).isTraversableAgain();
    if (isCollection()) {
      return true;
    }
    return false;
  }

  @Override
  public String stringPrefix() {
    return "RichIterable";
  }

  @Override
  public String toString() {
    if (isCollection()) {
      return take(3).mkString(stringPrefix() + ".take(3)(", ", ", ")");
    } else {
      return stringPrefix() + "(" + operation + ")";
    }
  }

  @Override
  public Iterable<T> iterable() {
    Iterable<T> iterable = iterableLazy.get();
    return iterable;
  }

  @Override
  public Iterator<T> iterator() {
    return iterator("directIterator");
  }

  public Iterator<T> iteratorInternal() {
    return iterator("internal");
  }

  @Override
  public Iterator<T> iterator(String operation) {
    return iteratorInternal(operation, true);
  }

  //@Override
  public Iterator<T> iteratorOverCollection(String operation) {
    return iteratorInternal(operation, true);
  }

  private Iterator<T> iteratorInternal(String operation, boolean allowIteratorIfCollection) {
    Iterable<T> iterable = iterableLazy.get();
    if (allowIteratorIfCollection && isCollection()) {
      //its safe to iterate
      return Iterator.ofAll(iterable.iterator());
    } else {
      int current = iterated.updateAndGet(x -> Math.min(x + 1, maxIterations + 1));
      if (current == maxIterations) {
        this.lastOperation = operation;
        this.lastOperationPlace = debugEnabled
            ? Option.of(new RuntimeException(
              "Successful operation " + operation + " on " + internalToString() + ". To store stacktrace."))
            : Option.none();
      }
      if (current > maxIterations) {
        String messageIfDebugNotEnabled = lastOperationPlace.isDefined() ? ""
            : "\n\nUse debug to get the last successfull location where you could memoize the collection before iterating too many times.";
        throw new IllegalStateException("While doing [" + operation + "] operation a maximum number of iterations of "
            + maxIterations
            + " has been reached.\nPrevious successfull operation was [" + lastOperation
            + "].\nIs it better to memoize the RichIterable as this will not use iterator for this operation. The root cause is not actually causing this but the stacktrace can help to detect where the memoization would have been helpful."
            + messageIfDebugNotEnabled,
          lastOperationPlace.get());
      } //
      return Iterator.ofAll(iterable.iterator());
    }
  }

  private String internalToString() {
    Iterable<T> iterable = iterableLazy.get();
    return String.format("%s(op=%s on %s and %s params %s)", getClass(), this.operation, iterable.getClass(),
      this.params.length,
      Arrays.toString(this.params));
  }

  @Override
  public RichIterable<Seq<T>> grouped(int size) {
    return new RichIterableUsingIterator<>("grouped", this, () -> iterator("grouped").grouped(size), size);
  }

  @Override
  public <C> RichIterable<Tuple2<? extends C, RichIterable<T>>> groupBy(Function<? super T, ? extends C> classifier) {
    return RichIterable.ofJava(groupByInternal(this, classifier))
      .map(x -> Tuple.of(x.getKey(), RichIterable.ofJava(x.getValue())));
  }

  @Override
  public <C> Map<C, Iterator<T>> groupByAsVavrIterator(Function<? super T, ? extends C> classifier) {
    return groupByWithGenericMapper(this, classifier, Iterator::ofAll);
  }

  @Override
  public <C> Map<C, RichIterable<T>> groupByAsRichIterable(Function<? super T, ? extends C> classifier) {
    return groupByWithGenericMapper(this, classifier, RichIterable::ofIterable);
  }

  private static <T, C, R extends Iterable<T>> Map<C, R> groupBy(RichIterable<T> source,
      Function<? super T, ? extends C> classifier, Function<? super Iterable<T>, R> mapper) {
    return groupByWithGenericMapper(source, classifier, mapper);
  }

  //Copied from vavr Collections.groupBy
  private static <T, C, R> Map<C, R> groupByWithGenericMapper(RichIterable<T> source,
      Function<? super T, ? extends C> classifier, Function<? super Iterable<T>, R> mapper) {
    Objects.requireNonNull(classifier, "classifier is null");
    Objects.requireNonNull(mapper, "mapper is null");
    Map<C, R> results = LinkedHashMap.empty();
    for (java.util.Map.Entry<? extends C, Collection<T>> entry : groupByInternal(source, classifier)) {
      results = results.put(entry.getKey(), mapper.apply(entry.getValue()));
    }
    return results;
  }

  private static <T, C> java.util.Set<java.util.Map.Entry<C, Collection<T>>> groupByInternal(RichIterable<T> source,
      Function<? super T, ? extends C> classifier) {
    final java.util.Map<C, Collection<T>> results = new java.util.LinkedHashMap<>(
      source.isTraversableAgain() ? source.size() : 16);
    for (T value : source.iterator("groupByInternal")) {
      final C key = classifier.apply(value);
      results.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }
    return results.entrySet();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <U extends T> RichIterable<U> narrow(Class<U> clazz) {
    return (RichIterable<U>) (Object) new RichIterableUsingIterator<>("narrow", this,
      () -> iterator("narrow").filter(y -> y.getClass() == clazz), clazz);
  }

  /**Computes and stores the result of iterable. Creates a new RichIterator based on this.*/
  @Override
  public RichIterable<T> memoizeVavr() {
    return RichIterable.ofVavr(toList());
  }

  @Override
  public RichIterable<T> memoizeJava() {
    return RichIterable.ofJava(toJavaList());
  }

  @Override
  public RichIterable<T> concat(Iterable<T> next) {
    return RichIterable.concatAll(this, new RichIterableUsingIterator<>("concat", this, next, next));
  }

  @Override
  public RichIterable<T> concat(RichIterable<T> next) {
    return RichIterable.concatAll(this, next);
  }

  @Override
  @Deprecated
  public T get(int index) {
    return op(l -> l.get(index), c -> null, s -> s.get(index), t -> null, v -> null, () -> drop(index).head());
  }

  @Override
  @Deprecated
  public Option<T> getOption(int index) {
    if (index >= 0 && index < size()) {
      return Option.of(get(index));
    }
    return Option.none();
  }

  @Override
  public RichIterable<T> sorted() {
    Lazy<List<T>> sorted = Lazy.of(() -> {
      @SuppressWarnings("unchecked")
      T[] arrayToSort = (T[]) iteratorOverCollection("sorted").toJavaArray();
      Arrays.sort(arrayToSort);
      return Arrays.asList(arrayToSort);
    });
    return new RichIterableUsingIterator<>("sorted", this, sorted);
  }

  @Override
  public RichIterable<T> sorted(Comparator<? super T> comparator) {
    //Do not use lazy as this will cache the value. The user should decide if he wants that via using memoize
    //Lazy<java.util.List<T>> sorted =Lazy.of(
    Lazy<List<T>> sorted = Lazy.of(() -> {
      @SuppressWarnings("unchecked")
      T[] arrayToSort = (T[]) iterator("sortedWithComparator").toJavaArray();
      Arrays.sort(arrayToSort, comparator);
      //      java.util.List<T> list = iterator().toJavaList();
      //      list.sort(comparator);
      return Arrays.asList(arrayToSort);
    });
    //return new RichIterableUsingIterator<>("sortedWithComparator", this, () -> sorted.get(), comparator);
    return new RichIterableUsingIterator<>("sortedWithComparator", this, sorted);
  }

  @Override
  public <U extends Comparable<? super U>> RichIterable<T> sortBy(Function<? super T, ? extends U> mapper) {
    return sorted(comparator(mapper));
  }

  @Override
  public <U extends Comparable<? super U>> RichIterable<T> sortByReversed(Function<? super T, ? extends U> mapper) {
    return sorted(reversedComparator(mapper));
  }

  private <U extends Comparable<? super U>> Comparator<? super T> reversedComparator(
      Function<? super T, ? extends U> mapper) {
    return (o1, o2) -> {
      U v1 = mapper.apply(o1);
      U v2 = mapper.apply(o2);
      return -v1.compareTo(v2);
    };
  }

  private <U extends Comparable<? super U>> Comparator<? super T> comparator(Function<? super T, ? extends U> mapper) {
    return (o1, o2) -> {
      U v1 = mapper.apply(o1);
      U v2 = mapper.apply(o2);
      return v1.compareTo(v2);
    };
  }

  @Override
  public RichIterable<T> reverse() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public RichIterable<T> doOnNext(CheckedConsumer<T> consumer) {
    return new RichIterableUsingIterator<>("doOnNext", this, () -> iterator("doOnNext").map(x -> {
      consumer.unchecked().accept(x);
      return x;
    }), consumer);
  }

  @Override
  public boolean isAsync() {
    return iterator("isAsync").isAsync();
  }

  @Override
  public boolean isEmpty() {
    return iterator("isEmpty").isEmpty();
  }

  @Override
  public boolean isLazy() {
    return iterator("isLazy").isLazy();
  }

  //  @Override
  //  public boolean isTraversableAgain() {
  //    return iterator().isTraversableAgain();
  //  }

  @Override
  public boolean isSequential() {
    return iterator("isSequential").isSequential();
  }

  @Override
  public T last() {
    return op(l -> l.get(l.size() - 1), c -> null, s -> s.last(), t -> t.last(), v -> v.get(),
      () -> iterator("last").last());
  }

  @Override
  public int length() {
    return size();
  }

  @Override
  public int size() {
    return op(l -> l.size(), c -> c.size(), s -> s.size(), t -> t.size(), v -> 1, () -> iterator("size").size());
  }

  //TODO implement this using wrappers that implement RichIterable operations
  /**
   * If a function resturns null the next in line will be tried.
   */
  private <R> R op(Function1<List<T>, R> opForList, Function1<Collection<T>, R> opForCollection,
      Function1<IndexedSeq<T>, R> opForIndexedSeq,
      Function1<Traversable<T>, R> opForTraversable,
      Function1<Value<T>, R> opForValue,
      Function0<R> opForRest) {
    R res = null;
    Iterable<T> iterable = iterableLazy.get();
    if (iterable instanceof List) {
      //TODO should check RandomAccess interface?
      res = opForList.apply((List<T>) iterable);
    }
    if (res == null && iterable instanceof Collection) {
      res = opForCollection.apply((Collection<T>) iterable);
    }
    if (res == null && iterable instanceof IndexedSeq) {
      res = opForIndexedSeq.apply((IndexedSeq<T>) iterable);
    }
    if (res == null && iterable instanceof Traversable) {
      res = opForTraversable.apply((Traversable<T>) iterable);
    }
    if (res == null && iterable instanceof Value) {
      res = opForValue.apply((Value<T>) iterable);
    }
    if (res == null) {
      res = opForRest.apply();
    }
    return res;
  }

  @Override
  public boolean equals(Object o) {
    return equalsHere(this, o);
  }

  @Override
  public int hashCode() {
    return hashOrdered(this.iterable());
  }

  private static <V> boolean equalsHere(RichIterable<V> source, Object object) {
    if (object == source) {
      return true;
    } else if (source != null && object instanceof RichIterable) {
      final RichIterable<V> seq = (RichIterable<V>) object;
      return seq.size() == source.size() && areEqual(source.iterable(), seq.iterable());
    } else {
      return false;
    }
  }

  static boolean areEqual(Iterable<?> iterable1, Iterable<?> iterable2) {
    final java.util.Iterator<?> iter1 = iterable1.iterator();
    final java.util.Iterator<?> iter2 = iterable2.iterator();
    while (iter1.hasNext() && iter2.hasNext()) {
      if (!Objects.equals(iter1.next(), iter2.next())) {
        return false;
      }
    }
    return iter1.hasNext() == iter2.hasNext();
  }

  private static int hashOrdered(Iterable<?> iterable) {
    return hash(iterable, (acc, hash) -> acc * 31 + hash);
  }

  // hashes the elements regardless of their order
  private static int hashUnordered(Iterable<?> iterable) {
    return hash(iterable, (acc, hash) -> acc + hash);
  }

  private static int hash(Iterable<?> iterable, IntBinaryOperator accumulator) {
    if (iterable == null) {
      return 0;
    } else {
      int hashCode = 1;
      for (Object o : iterable) {
        hashCode = accumulator.applyAsInt(hashCode, Objects.hashCode(o));
      }
      return hashCode;
    }
  }

  /**********************************************************************/
  /** Next operations just delegate to `vavr.Iterator` */
  /**********************************************************************/
  @Override
  public T fold(T zero, BiFunction<? super T, ? super T, ? extends T> combine) {
    return iterator("fold").fold(zero, combine);
  }

  @Override
  public T reduce(BiFunction<? super T, ? super T, ? extends T> op) {
    return iterator("reduce").reduce(op);
  }

  @Override
  public Option<T> reduceOption(BiFunction<? super T, ? super T, ? extends T> op) {
    return iterator("reduceOption").reduceOption(op);
  }

  @Override
  public <R, A> R collect(Collector<? super T, A, R> collector) {
    return iterator("collect").collect(collector);
  }

  @Override
  public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
    return iterator("collect").collect(supplier, accumulator, combiner);
  }

  @Override
  public boolean contains(T element) {
    return iterator("contains").contains(element);
  }

  @Override
  public <U> boolean corresponds(Iterable<U> that, BiPredicate<? super T, ? super U> predicate) {
    return iterator("corresponds").corresponds(that, predicate);
  }

  @Override
  public boolean eq(Object o) {
    return iterator("eq").eq(o);
  }

  @Override
  public boolean exists(Predicate<? super T> predicate) {
    return iterator("exists").exists(predicate);
  }

  @Override
  public boolean forAll(Predicate<? super T> predicate) {
    return iteratorOverCollection("forAll").forAll(predicate);
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    iteratorOverCollection("forEach").forEach(action);
  }

  @Override
  public T getOrElse(T other) {
    return iterator("getOrElse").getOrElse(other);
  }

  @Override
  public T getOrElse(Supplier<? extends T> supplier) {
    return iterator("getOrElse").getOrElse(supplier);
  }

  @Override
  public <X extends Throwable> T getOrElseThrow(Supplier<X> supplier) throws X {
    return iterator("getOrElseThrow").getOrElseThrow(supplier);
  }

  @Override
  public T getOrElseTry(CheckedFunction0<? extends T> supplier) {
    return iterator("getOrElseTry").getOrElseTry(supplier);
  }

  @Override
  public T getOrNull() {
    return iterator("getOrNull").getOrNull();
  }

  @Override
  public void out(PrintStream out) {
    iterator("out").out(out);
  }

  @Override
  public void out(PrintWriter writer) {
    iterator("out").out(writer);
  }

  @Override
  public void stderr() {
    iterator("stderr").stderr();
  }

  @Override
  public void stdout() {
    iterator("stdout").stdout();
  }

  @Override
  public Array<T> toArray() {
    return iterator("toArray").toArray();
  }

  @Override
  public CharSeq toCharSeq() {
    return iteratorInternal().toCharSeq();
  }

  @Override
  public CompletableFuture<T> toCompletableFuture() {
    return iterator("toCompletableFuture").toCompletableFuture();
  }

  @Override
  public Object[] toJavaArray() {
    return iterator("toJavaArray").toJavaArray();
  }

  @Override
  public T[] toJavaArray(Class<T> componentType) {
    return iterator("toJavaArray").toJavaArray(componentType);
  }

  @Override
  public T[] toJavaArray(IntFunction<T[]> arrayFactory) {
    return iterator("toJavaArray").toJavaArray(arrayFactory);
  }

  @Override
  public <C extends Collection<T>> C toJavaCollection(Function<Integer, C> factory) {
    return iteratorInternal().toJavaCollection(factory);
  }

  @Override
  public java.util.List<T> toJavaList() {
    return op(l -> l, c -> null, s -> s.toJavaList(), t -> null, v -> null,
      () -> iteratorInternal().toJavaList());
  }

  @Override
  public <LIST extends java.util.List<T>> LIST toJavaList(Function<Integer, LIST> factory) {
    return iteratorInternal().toJavaList(factory);
  }

  @Override
  public <K, V> java.util.Map<K, V> toJavaMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
    return iteratorInternal().toJavaMap(f);
  }

  @Override
  public <K, V, MAP extends java.util.Map<K, V>> MAP toJavaMap(Supplier<MAP> factory,
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper) {
    return iteratorInternal().toJavaMap(factory, keyMapper, valueMapper);
  }

  @Override
  public <K, V, MAP extends java.util.Map<K, V>> MAP toJavaMap(Supplier<MAP> factory,
      Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
    return iteratorInternal().toJavaMap(factory, f);
  }

  @Override
  public Optional<T> toJavaOptional() {
    return iteratorInternal().toJavaOptional();
  }

  @Override
  public java.util.Set<T> toJavaSet() {
    return iteratorInternal().toJavaSet();
  }

  @Override
  public <SET extends java.util.Set<T>> SET toJavaSet(Function<Integer, SET> factory) {
    return iteratorInternal().toJavaSet(factory);
  }

  @Override
  public Stream<T> toJavaStream() {
    return iteratorInternal().toJavaStream();
  }

  @Override
  public Stream<T> toJavaParallelStream() {
    return iteratorInternal().toJavaParallelStream();
  }

  @Override
  public io.vavr.collection.List<T> toList() {
    return iteratorInternal().toList();
  }

  @Override
  public <K, V> io.vavr.collection.Map<K, V> toMap(Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper) {
    return iteratorInternal().toMap(keyMapper, valueMapper);
  }

  @Override
  public <K, V> io.vavr.collection.Map<K, V> toMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
    return iteratorInternal().toMap(f);
  }

  @Override
  public <K, V> io.vavr.collection.Map<K, V> toLinkedMap(Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper) {
    return iteratorInternal().toLinkedMap(keyMapper, valueMapper);
  }

  @Override
  public <K, V> io.vavr.collection.Map<K, V> toLinkedMap(
      Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
    return iteratorInternal().toLinkedMap(f);
  }

  @Override
  public <K extends Comparable<? super K>, V> SortedMap<K, V> toSortedMap(Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper) {
    return iteratorInternal().toSortedMap(keyMapper, valueMapper);
  }

  @Override
  public <K extends Comparable<? super K>, V> SortedMap<K, V> toSortedMap(
      Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
    return iteratorInternal().toSortedMap(f);
  }

  @Override
  public <K, V> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator,
      Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
    return iteratorInternal().toSortedMap(comparator, keyMapper, valueMapper);
  }

  @Override
  public <K, V> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator,
      Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
    return iteratorInternal().toSortedMap(comparator, f);
  }

  @Override
  public Option<T> toOption() {
    return iteratorInternal().toOption();
  }

  @Override
  public <L> Either<L, T> toEither(L left) {
    return iteratorInternal().toEither(left);
  }

  @Override
  public <L> Either<L, T> toEither(Supplier<? extends L> leftSupplier) {
    return iteratorInternal().toEither(leftSupplier);
  }

  @Override
  public <E> Validation<E, T> toValidation(E invalid) {
    return iteratorInternal().toValidation(invalid);
  }

  @Override
  public <E> Validation<E, T> toValidation(Supplier<? extends E> invalidSupplier) {
    return iteratorInternal().toValidation(invalidSupplier);
  }

  @Override
  public Queue<T> toQueue() {
    return iteratorInternal().toQueue();
  }

  @Override
  public PriorityQueue<T> toPriorityQueue() {
    return iteratorInternal().toPriorityQueue();
  }

  @Override
  public PriorityQueue<T> toPriorityQueue(Comparator<? super T> comparator) {
    return iteratorInternal().toPriorityQueue(comparator);
  }

  @Override
  public io.vavr.collection.Set<T> toSet() {
    return iteratorInternal().toSet();
  }

  @Override
  public io.vavr.collection.Set<T> toLinkedSet() {
    return iteratorInternal().toLinkedSet();
  }

  @Override
  public SortedSet<T> toSortedSet() throws ClassCastException {
    return iteratorInternal().toSortedSet();
  }

  @Override
  public SortedSet<T> toSortedSet(Comparator<? super T> comparator) {
    return iteratorInternal().toSortedSet(comparator);
  }

  @Override
  public io.vavr.collection.Stream<T> toStream() {
    return iteratorInternal().toStream();
  }

  @Override
  public Try<T> toTry() {
    return iteratorInternal().toTry();
  }

  @Override
  public Try<T> toTry(Supplier<? extends Throwable> ifEmpty) {
    return iteratorInternal().toTry(ifEmpty);
  }

  @Override
  public Tree<T> toTree() {
    return iteratorInternal().toTree();
  }

  @Override
  public <ID> io.vavr.collection.List<Node<T>> toTree(Function<? super T, ? extends ID> idMapper,
      Function<? super T, ? extends ID> parentMapper) {
    return iteratorInternal().toTree(idMapper, parentMapper);
  }

  @Override
  public Vector<T> toVector() {
    return iteratorInternal().toVector();
  }

  @Override
  public <K> Option<io.vavr.collection.Map<K, T>> arrangeBy(Function<? super T, ? extends K> getKey) {
    return iteratorInternal().arrangeBy(getKey);
  }

  @Override
  public Option<Double> average() {
    return iteratorInternal().average();
  }

  @Override
  public boolean containsAll(Iterable<? extends T> elements) {
    return iteratorInternal().containsAll(elements);
  }

  @Override
  public int count(Predicate<? super T> predicate) {
    return iteratorInternal().count(predicate);
  }

  @Override
  public boolean existsUnique(Predicate<? super T> predicate) {
    return iteratorInternal().existsUnique(predicate);
  }

  @Override
  public Option<T> find(Predicate<? super T> predicate) {
    return iteratorInternal().find(predicate);
  }

  @Override
  public <U> U foldLeft(U zero, BiFunction<? super U, ? super T, ? extends U> f) {
    return iteratorInternal().foldLeft(zero, f);
  }

  @Override
  public void forEachWithIndex(ObjIntConsumer<? super T> action) {
    iteratorInternal().forEachWithIndex(action);
  }

  @Override
  public Option<T> headOption() {
    return iteratorInternal().headOption();
  }

  @Override
  public boolean isDistinct() {
    return iteratorInternal().isDistinct();
  }

  @Override
  public boolean isOrdered() {
    return iteratorInternal().isOrdered();
  }

  @Override
  public boolean isSingleValued() {
    return iteratorInternal().isSingleValued();
  }

  @Override
  public Option<T> lastOption() {
    return iteratorInternal().lastOption();
  }

  @Override
  public Option<T> max() {
    return iteratorInternal().max();
  }

  @Override
  public Option<T> maxBy(Comparator<? super T> comparator) {
    return iteratorInternal().maxBy(comparator);
  }

  @Override
  public <U extends Comparable<? super U>> Option<T> maxBy(Function<? super T, ? extends U> f) {
    return iteratorInternal().maxBy(f);
  }

  @Override
  public Option<T> min() {
    return iteratorInternal().min();
  }

  @Override
  public Option<T> minBy(Comparator<? super T> comparator) {
    return iteratorInternal().minBy(comparator);
  }

  @Override
  public <U extends Comparable<? super U>> Option<T> minBy(Function<? super T, ? extends U> f) {
    return iteratorInternal().minBy(f);
  }

  @Override
  public CharSeq mkCharSeq() {
    return iteratorInternal().mkCharSeq();
  }

  @Override
  public CharSeq mkCharSeq(CharSequence delimiter) {
    return iteratorInternal().mkCharSeq(delimiter);
  }

  @Override
  public CharSeq mkCharSeq(CharSequence prefix, CharSequence delimiter, CharSequence suffix) {
    return iteratorInternal().mkCharSeq(prefix, delimiter, suffix);
  }

  @Override
  public String mkString() {
    return iteratorInternal().mkString();
  }

  @Override
  public String mkString(CharSequence delimiter) {
    return iteratorInternal().mkString(delimiter);
  }

  @Override
  public String mkString(CharSequence prefix, CharSequence delimiter, CharSequence suffix) {
    return iteratorOverCollection("mkString").mkString(prefix, delimiter, suffix);
  }

  @Override
  public boolean nonEmpty() {
    return iteratorInternal().nonEmpty();
  }

  @Override
  public Number product() {
    return iteratorInternal().product();
  }

  @Override
  public Option<T> reduceLeftOption(BiFunction<? super T, ? super T, ? extends T> op) {
    return iteratorInternal().reduceLeftOption(op);
  }

  @Override
  public Option<T> reduceRightOption(BiFunction<? super T, ? super T, ? extends T> op) {
    return iteratorInternal().reduceRightOption(op);
  }

  @Override
  public T single() {
    return iteratorInternal().single();
  }

  @Override
  public Option<T> singleOption() {
    return iteratorInternal().singleOption();
  }

  @Override
  public Spliterator<T> spliterator() {
    return iteratorInternal().spliterator();
  }

  @Override
  public Number sum() {
    return iteratorInternal().sum();
  }

  @Override
  public <R> RichIterable<R> collect(PartialFunction<? super T, ? extends R> partialFunction) {
    return new RichIterableUsingIterator<>("collect", this, () -> iteratorInternal().collect(partialFunction),
      partialFunction);
  }

  @Override
  public RichIterable<T> concat(java.util.Iterator<? extends T> that) {
    return new RichIterableUsingIterator<>("concat", this, () -> iteratorInternal().concat(that), that);
  }

  @Override
  public RichIterable<T> intersperse(T element) {
    return new RichIterableUsingIterator<>("intersperse", this, () -> iteratorInternal().intersperse(element), element);
  }

  @Override
  public <U> U transform(Function<? super Iterator<T>, ? extends U> f) {
    return iteratorInternal().transform(f);
  }

  @Override
  public <U> RichIterable<Tuple2<T, U>> zip(Iterable<? extends U> that) {
    return new RichIterableUsingIterator<>("zip", this, () -> iteratorInternal().zip(that), that);
  }

  @Override
  public <U, R> RichIterable<R> zipWith(Iterable<? extends U> that,
      BiFunction<? super T, ? super U, ? extends R> mapper) {
    return new RichIterableUsingIterator<>("zipWith", this, () -> iteratorInternal().zipWith(that, mapper), that,
      mapper);
  }

  @Override
  public <U> RichIterable<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
    return new RichIterableUsingIterator<>("zipAll", this, () -> iteratorInternal().zipAll(that, thisElem, thatElem),
      that,
      thisElem, thatElem);
  }

  @Override
  public RichIterable<Tuple2<T, Integer>> zipWithIndex() {
    return new RichIterableUsingIterator<>("zipWithIndex", this, () -> iteratorInternal().zipWithIndex());
  }

  @Override
  public <U> RichIterable<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
    return new RichIterableUsingIterator<>("zipWithIndex", this, () -> iteratorInternal().zipWithIndex(mapper), mapper);
  }

  @Override
  @Deprecated //not implemented yet
  public <T1, T2> Tuple2<RichIterable<T1>, RichIterable<T2>> unzip(
      Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
    throw new RuntimeException("Not implemented yet!!!");
    //return new RichIterable3<>(() -> iterator().unzip(unzipper));
  }

  @Override
  @Deprecated //not implemented yet
  public <T1, T2, T3> Tuple3<RichIterable<T1>, RichIterable<T2>, RichIterable<T3>> unzip3(
      Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
    throw new RuntimeException("Not implemented yet!!!");
    //return new RichIterable3<>(() -> iterator().unzip3(unzipper));
  }

  @Override
  public RichIterable<T> distinct() {
    return new RichIterableUsingIterator<>("distinct", this, () -> iteratorInternal().distinct());
  }

  @Override
  public RichIterable<T> distinctBy(Comparator<? super T> comparator) {
    return new RichIterableUsingIterator<>("distinctBy", this, () -> iteratorInternal().distinctBy(comparator),
      comparator);
  }

  @Override
  public <U> RichIterable<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
    return new RichIterableUsingIterator<>("distinctBy", this, () -> iteratorInternal().distinctBy(keyExtractor),
      keyExtractor);
  }

  @Override
  public RichIterable<T> drop(int n) {
    return new RichIterableUsingIterator<>("drop", this, () -> iterator("drop").drop(n), n);
  }

  @Override
  public RichIterable<T> dropRight(int n) {
    return new RichIterableUsingIterator<>("dropRight", this, () -> iteratorInternal().dropRight(n), n);
  }

  @Override
  public RichIterable<T> dropUntil(Predicate<? super T> predicate) {
    return new RichIterableUsingIterator<>("dropUntil", this, () -> iteratorInternal().dropUntil(predicate), predicate);
  }

  @Override
  public RichIterable<T> dropWhile(Predicate<? super T> predicate) {
    return new RichIterableUsingIterator<>("dropWhile", this, () -> iteratorInternal().dropWhile(predicate), predicate);
  }

  @Override
  public RichIterable<T> filter(Predicate<? super T> predicate) {
    return new RichIterableUsingIterator<>("filter", this, () -> iteratorInternal().filter(predicate), predicate);
  }

  @Override
  public RichIterable<T> reject(Predicate<? super T> predicate) {
    return new RichIterableUsingIterator<>("reject", this, () -> iteratorInternal().reject(predicate), predicate);
  }

  @Override
  public Option<T> findLast(Predicate<? super T> predicate) {
    return iteratorInternal().findLast(predicate);
  }

  @Override
  public <U> RichIterable<U> flatMapFromIterable(Function<? super T, ? extends Iterable<? extends U>> mapper) {
    return new RichIterableUsingIterator<>("flatMapFromIterable", this, () -> iteratorInternal().flatMap(mapper),
      mapper);
  }

  @Override
  public <U> U foldRight(U zero, BiFunction<? super T, ? super U, ? extends U> f) {
    return iteratorInternal().foldRight(zero, f);
  }

  @Override
  public T get() {
    return iteratorInternal().get();
  }

  @Override
  public boolean hasDefiniteSize() {
    return iteratorInternal().hasDefiniteSize();
  }

  @Override
  public T head() {
    return iteratorInternal().head();
  }

  @Override
  public RichIterable<T> init() {
    return new RichIterableUsingIterator<>("init", this, () -> iteratorInternal().init());
  }

  @Override
  @Deprecated //not implemented yet
  public Option<RichIterable<T>> initOption() {
    throw new RuntimeException("Not implemented yet!!!");
    //return new RichIterable3<>(() -> iterator().initOption());
  }

  @Override
  public <U> RichIterable<U> map(Function<? super T, ? extends U> mapper) {
    return new RichIterableUsingIterator<>("map", this, () -> iterator("map").map(mapper));
  }

  @Override
  public RichIterable<T> orElse(Iterable<? extends T> other) {
    return new RichIterableUsingIterator<>("orElse", this, () -> iterator("orElse").orElse(other));
  }

  @Override
  public RichIterable<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
    return new RichIterableUsingIterator<>("orElse", this, () -> iterator("orElse").orElse(supplier));
  }

  @Override
  public Tuple2<RichIterable<T>, RichIterable<T>> partition(Predicate<? super T> predicate) {
    Tuple2<Iterator<T>, Iterator<T>> partition = iterator("partition").partition(predicate);
    return Tuple.of(RichIterable.ofVavr(partition._1), RichIterable.ofVavr(partition._2));
  }

  @Override
  public RichIterable<T> peek(Consumer<? super T> action) {
    return new RichIterableUsingIterator<>("peek", this, () -> iterator("peek").peek(action));
  }

  @Override
  public T reduceLeft(BiFunction<? super T, ? super T, ? extends T> op) {
    return iterator("reduceLeft").reduceLeft(op);
  }

  @Override
  public T reduceRight(BiFunction<? super T, ? super T, ? extends T> op) {
    return iterator("reduceRight").reduceRight(op);
  }

  @Override
  public RichIterable<T> replace(T currentElement, T newElement) {
    return new RichIterableUsingIterator<>("replace", this,
      () -> iterator("replace").replace(currentElement, newElement),
      currentElement, newElement);
  }

  @Override
  public RichIterable<T> replaceAll(T currentElement, T newElement) {
    return new RichIterableUsingIterator<>("replaceAll", this,
      () -> iterator("replaceAll").replaceAll(currentElement, newElement),
      currentElement, newElement);
  }

  @Override
  public RichIterable<T> retainAll(Iterable<? extends T> elements) {
    return new RichIterableUsingIterator<>("retainAll", this, () -> iterator("retainAll").retainAll(elements),
      elements);
  }

  @Override
  public Traversable<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
    return iterator("scan").scan(zero, operation);
  }

  @Override
  public <U> RichIterable<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
    return new RichIterableUsingIterator<>("scanLeft", this, () -> iterator("scanLeft").scanLeft(zero, operation),
      zero);
  }

  @Override
  public <U> RichIterable<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
    return new RichIterableUsingIterator<>("scanRight", this, () -> iterator("scanRight").scanRight(zero, operation),
      zero);
  }

  @Override
  public RichIterable<Seq<T>> slideBy(Function<? super T, ?> classifier) {
    return new RichIterableUsingIterator<>("slideBy", this, () -> iterator("slideBy").slideBy(classifier));
  }

  @Override
  public RichIterable<Seq<T>> sliding(int size) {
    return new RichIterableUsingIterator<>("sliding", this, () -> iterator("sliding").sliding(size), size);
  }

  @Override
  public RichIterable<Seq<T>> sliding(int size, int step) {
    return new RichIterableUsingIterator<>("sliding", this, () -> iterator("sliding").sliding(size, step), size, step);
  }

  @Override
  @Deprecated //not implemented yet
  public Tuple2<RichIterable<T>, RichIterable<T>> span(Predicate<? super T> predicate) {
    throw new RuntimeException("Not implemented yet!!!");
    //return new RichIterable3<>(() -> iterator().span(predicate));
  }

  @Override
  public RichIterable<T> tail() {
    return new RichIterableUsingIterator<>("tail", this, () -> iterator("span").tail());
  }

  @Override
  @Deprecated //not implemented yet
  public Option<RichIterable<T>> tailOption() {
    throw new RuntimeException("Not implemented yet!!!");
    //return new RichIterable3<>(() -> iterator().tailOption());
  }

  @Override
  public RichIterable<T> take(int n) {
    return new RichIterableUsingIterator<>("take", this, () -> iterator("take").take(n), n);
  }

  @Override
  public RichIterable<T> takeRight(int n) {
    return new RichIterableUsingIterator<>("takeRight", this, () -> iterator("takeRight").takeRight(n), n);
  }

  @Override
  public RichIterable<T> takeUntil(Predicate<? super T> predicate) {
    return new RichIterableUsingIterator<>("takeUntil", this, () -> iterator("takeUntil").takeUntil(predicate));
  }

  @Override
  public RichIterable<T> takeWhile(Predicate<? super T> predicate) {
    return new RichIterableUsingIterator<>("takeWhile", this, () -> iterator("takeWhile").takeWhile(predicate));
  }

  public Tuple3<RichIterable<T>, RichIterable<T>, RichIterable<T>> split(int offset, int pageSize) {
    //TODO could be improved by iterating
    Iterator<T> iterator = iterator("page");
    if (iterator.isEmpty()) {
      return Tuple.of(RichIterable.empty(), RichIterable.empty(), RichIterable.empty());
    }
    final io.vavr.collection.Stream<T> that = iterator.toStream();
    return Tuple.of(rich("split_1", that.take(offset), offset, pageSize),
      rich("split_2", that.drop(offset).take(pageSize), offset, pageSize),
      rich("split_3", that.drop(offset + pageSize), offset, pageSize));
  }

  private RichIterable<T> rich(String operation, io.vavr.collection.Stream<T> iterator, int offset, int pageSize) {
    return new RichIterableUsingIterator<>(operation, this, iterator, offset, pageSize);
  }

  @Override
  public Tuple2<RichIterable<T>, Integer> page(int offset, int pageSize) {
    Tuple3<RichIterable<T>, RichIterable<T>, RichIterable<T>> x = split(offset, pageSize);
    return Tuple.of(x._2, x._1.size() + x._2.size() + x._3.size());
  }

  @Override
  public RichIterable<T> debug(boolean debugEnabled) {
    this.debugEnabled = debugEnabled;
    return this;
  }
}
