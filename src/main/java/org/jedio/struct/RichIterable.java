package org.jedio.struct;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import io.vavr.API;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction0;
import io.vavr.PartialFunction;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.Value;
import io.vavr.collection.Array;
import io.vavr.collection.CharSeq;
import io.vavr.collection.Iterator;
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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.raisercostin.jedio.Location;
import org.raisercostin.jedio.Locations;

/**
 * Intentionally doesn't implement Iterable to force you to pass RichIterable around.
 * You can get that with iterable().
 *
 *
 * TODO
 * - IndexedIterable should be created? For underlying collections that could offer efficient get(index)
 *   - filter/flatMap destroyes indexed addressing
 *   - map keeps indexed addressing
 *   - Map structures allow get(key) but not index
 */
@JsonDeserialize(using = RichIterable.RichIterableDeserializer.class)
@JsonSerialize(using = RichIterable.RichIterableSerializer.class)
public interface RichIterable<T> {

  static class RichIterableDeserializer<T> extends JsonDeserializer<RichIterable<T>> {
    @Override
    public RichIterable<T> deserialize(@Nullable JsonParser p, @Nullable DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
      if (p == null) {
        return null;
      }
      return RichIterable.ofIterable(p.readValueAs(Iterable.class));
    }
  }

  static class RichIterableSerializer<T> extends JsonSerializer<RichIterable<T>> {
    @Override
    public void serialize(RichIterable<T> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      serializers.findValueSerializer(Iterable.class).serialize(value.iterable(), gen, serializers);
    }
  }

  static <T> RichIterable<T> ofAll(Iterable<T> iterable) {
    return ofIterable(iterable);
  }

  //  static <T> RichIterable<T> ofAll(Iterator<T> all) {
  //    throw new RuntimeException("Not implemented yet!!!");
  //  }
  /**
   * Use fromJava, fromVavr, fromGuava if possible. Otherwise this is fine.
   */
  static <T> RichIterable<T> ofIterable(Iterable<T> iterable) {
    if (iterable instanceof Collection) {
      return ofJava((Collection<T>) iterable);
    }
    if (iterable instanceof Value) {
      return ofVavr((Value<T>) iterable);
    }
    return new RichIterableUsingIterator<>("ofIterable", Option.none(), null, iterable, iterable);
  }

  static <T> RichIterable<T> ofJava(Collection<T> collection) {
    return new RichIterableUsingIterator<>("ofJava", Option.of(collection.size()), null, collection, collection);
  }

  @SuppressWarnings("unchecked")
  static <T> RichIterable<T> ofVavr(Value<T> iterable) {
    return new RichIterableUsingIterator<>("ofVavr", smartSize(iterable), null, iterable, iterable);
  }

  @SafeVarargs
  static <T> RichIterable<T> of(T... elements) {
    return ofJava(elements == null ? Lists.newArrayList() : Lists.newArrayList(elements));
  }

  static <T> RichIterable<T> empty() {
    return of();
  }

  @SafeVarargs
  static <T> RichIterable<T> concatAll(RichIterable<T>... iterables) {
    return new RichIterableUsingIterator<>("concatAll", Option.none(), null,
      () -> Iterator.concat(io.vavr.collection.List.of(iterables).map(x -> x.iterator("concat"))),
      (Object[]) iterables);
  }

  @SafeVarargs
  static <T> RichIterable<T> concatAll(Iterable<T>... iterables) {
    return new RichIterableUsingIterator<>("concatAll", Option.none(), null,
      () -> {
        io.vavr.collection.List<Iterable<T>> all = io.vavr.collection.List.of(iterables);
        return Iterator.concat(all);
      }, (Object[]) iterables);
  }

  <U extends T> RichIterable<U> narrow(Class<U> clazz);

  /**Computes and stores the result of iterable. Creates a new RichIterator based on this.*/
  RichIterable<T> memoizeVavr();

  RichIterable<T> memoizeJava();

  RichIterable<T> concat(Iterable<T> next);

  RichIterable<T> concat(RichIterable<T> next);

  /**This will try not to iterate on collection if possible.*/
  T get(int index);

  /**This will try not to iterate on collection if possible.*/
  Option<T> getOption(int index);

  RichIterable<T> sorted();

  RichIterable<T> sorted(Comparator<? super T> comparator);

  <U extends Comparable<? super U>> RichIterable<T> sortBy(Function<? super T, ? extends U> mapper);

  <U extends Comparable<? super U>> RichIterable<T> sortByReversed(Function<? super T, ? extends U> mapper);

  RichIterable<T> reverse();

  default RichIterable<T> append(T element) {
    return concatAll(this, RichIterable.of(element));
  }

  RichIterable<T> doOnNext(CheckedConsumer<T> callable);

  /* **************************************/

  Iterable<T> iterable();

  Iterator<T> iterator();

  Iterator<T> iterator(String operation);

  T fold(T zero, BiFunction<? super T, ? super T, ? extends T> combine);

  T reduce(BiFunction<? super T, ? super T, ? extends T> op);

  Option<T> reduceOption(BiFunction<? super T, ? super T, ? extends T> op);

  <R, A> R collect(Collector<? super T, A, R> collector);

  <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);

  boolean contains(T element);

  <U> boolean corresponds(Iterable<U> that, BiPredicate<? super T, ? super U> predicate);

  boolean eq(Object o);

  boolean exists(Predicate<? super T> predicate);

  boolean forAll(Predicate<? super T> predicate);

  void forEach(Consumer<? super T> action);

  T getOrElse(T other);

  T getOrElse(Supplier<? extends T> supplier);

  <X extends Throwable> T getOrElseThrow(Supplier<X> supplier) throws X;

  T getOrElseTry(CheckedFunction0<? extends T> supplier);

  T getOrNull();

  void out(PrintStream out);

  void out(PrintWriter writer);

  void stderr();

  void stdout();

  Array<T> toArray();

  CharSeq toCharSeq();

  CompletableFuture<T> toCompletableFuture();

  Object[] toJavaArray();

  T[] toJavaArray(Class<T> componentType);

  T[] toJavaArray(IntFunction<T[]> arrayFactory);

  <C extends Collection<T>> C toJavaCollection(Function<Integer, C> factory);

  List<T> toJavaList();

  <LIST extends List<T>> LIST toJavaList(Function<Integer, LIST> factory);

  <K, V> Map<K, V> toJavaMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f);

  <K, V, MAP extends Map<K, V>> MAP toJavaMap(Supplier<MAP> factory, Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper);

  <K, V, MAP extends Map<K, V>> MAP toJavaMap(Supplier<MAP> factory,
      Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f);

  Optional<T> toJavaOptional();

  Set<T> toJavaSet();

  <SET extends Set<T>> SET toJavaSet(Function<Integer, SET> factory);

  Stream<T> toJavaStream();

  Stream<T> toJavaParallelStream();

  io.vavr.collection.List<T> toList();

  <K, V> io.vavr.collection.Map<K, V> toMap(Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper);

  <K, V> io.vavr.collection.Map<K, V> toMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f);

  <K, V> io.vavr.collection.Map<K, V> toLinkedMap(Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper);

  <K, V> io.vavr.collection.Map<K, V> toLinkedMap(
      Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f);

  <K extends Comparable<? super K>, V> SortedMap<K, V> toSortedMap(Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper);

  <K extends Comparable<? super K>, V> SortedMap<K, V> toSortedMap(
      Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f);

  <K, V> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator,
      Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper);

  <K, V> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator,
      Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f);

  Option<T> toOption();

  <L> Either<L, T> toEither(L left);

  <L> Either<L, T> toEither(Supplier<? extends L> leftSupplier);

  <E> Validation<E, T> toValidation(E invalid);

  <E> Validation<E, T> toValidation(Supplier<? extends E> invalidSupplier);

  Queue<T> toQueue();

  PriorityQueue<T> toPriorityQueue();

  PriorityQueue<T> toPriorityQueue(Comparator<? super T> comparator);

  io.vavr.collection.Set<T> toSet();

  io.vavr.collection.Set<T> toLinkedSet();

  SortedSet<T> toSortedSet() throws ClassCastException;

  SortedSet<T> toSortedSet(Comparator<? super T> comparator);

  io.vavr.collection.Stream<T> toStream();

  Try<T> toTry();

  Try<T> toTry(Supplier<? extends Throwable> ifEmpty);

  Tree<T> toTree();

  <ID> io.vavr.collection.List<Node<T>> toTree(Function<? super T, ? extends ID> idMapper,
      Function<? super T, ? extends ID> parentMapper);

  Vector<T> toVector();

  <K> Option<io.vavr.collection.Map<K, T>> arrangeBy(Function<? super T, ? extends K> getKey);

  Option<Double> average();

  boolean containsAll(Iterable<? extends T> elements);

  int count(Predicate<? super T> predicate);

  boolean existsUnique(Predicate<? super T> predicate);

  Option<T> find(Predicate<? super T> predicate);

  <U> U foldLeft(U zero, BiFunction<? super U, ? super T, ? extends U> f);

  void forEachWithIndex(ObjIntConsumer<? super T> action);

  Option<T> headOption();

  boolean isDistinct();

  boolean isOrdered();

  boolean isSingleValued();

  Option<T> lastOption();

  /**Find max using natural ordering (Comparable)*/
  Option<T> max();

  /**Find max using the Comparable extracted from each element.*/
  Option<T> maxBy(Comparator<? super T> comparator);

  <U extends Comparable<? super U>> Option<T> maxBy(Function<? super T, ? extends U> f);

  /**Find min using natural ordering (Comparable)*/
  Option<T> min();

  /**Find min using the Comparable extracted from each element.*/
  Option<T> minBy(Comparator<? super T> comparator);

  <U extends Comparable<? super U>> Option<T> minBy(Function<? super T, ? extends U> f);

  CharSeq mkCharSeq();

  CharSeq mkCharSeq(CharSequence delimiter);

  CharSeq mkCharSeq(CharSequence prefix, CharSequence delimiter, CharSequence suffix);

  String mkString();

  String mkString(CharSequence delimiter);

  String mkString(CharSequence prefix, CharSequence delimiter, CharSequence suffix);

  boolean nonEmpty();

  Number product();

  Option<T> reduceLeftOption(BiFunction<? super T, ? super T, ? extends T> op);

  Option<T> reduceRightOption(BiFunction<? super T, ? super T, ? extends T> op);

  T single();

  Option<T> singleOption();

  int size();

  static <T> Option<Integer> smartSize(Value<T> iterable) {
    if (iterable.isEmpty()) {
      return Option.of(0);
    }
    if (iterable.isSingleValued()) {
      return Option.of(1);
    }
    if (iterable instanceof Traversable) {
      return ((Traversable) iterable).hasDefiniteSize() ? Option.of(((Traversable) iterable).size()) : Option.none();
    } else {
      return API.TODO("Don't know how to get smartSize for " + iterable.getClass());
    }
  }

  Spliterator<T> spliterator();

  Number sum();

  <R> RichIterable<R> collect(PartialFunction<? super T, ? extends R> partialFunction);

  RichIterable<T> concat(java.util.Iterator<? extends T> that);

  RichIterable<T> intersperse(T element);

  <U> U transform(Function<? super Iterator<T>, ? extends U> f);

  <U> RichIterable<Tuple2<T, U>> zip(Iterable<? extends U> that);

  <U, R> RichIterable<R> zipWith(Iterable<? extends U> that,
      BiFunction<? super T, ? super U, ? extends R> mapper);

  <U> RichIterable<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem);

  RichIterable<Tuple2<T, Integer>> zipWithIndex();

  <U> RichIterable<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper);

  <T1, T2> Tuple2<RichIterable<T1>, RichIterable<T2>> unzip(
      Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper);

  <T1, T2, T3> Tuple3<RichIterable<T1>, RichIterable<T2>, RichIterable<T3>> unzip3(
      Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper);

  RichIterable<T> distinct();

  RichIterable<T> distinctBy(Comparator<? super T> comparator);

  <U> RichIterable<T> distinctBy(Function<? super T, ? extends U> keyExtractor);

  RichIterable<T> drop(int n);

  RichIterable<T> dropRight(int n);

  RichIterable<T> dropUntil(Predicate<? super T> predicate);

  RichIterable<T> dropWhile(Predicate<? super T> predicate);

  RichIterable<T> filter(Predicate<? super T> predicate);

  RichIterable<T> reject(Predicate<? super T> predicate);

  Option<T> findLast(Predicate<? super T> predicate);

  <U> RichIterable<U> flatMapFromIterable(Function<? super T, ? extends Iterable<? extends U>> mapper);

  default <U> RichIterable<U> flatMap(@NonNull Function<? super T, ? extends RichIterable<? extends U>> mapper) {
    return flatMapFromIterable(x -> mapper.apply(x).iterable());
  }

  <U> U foldRight(U zero, BiFunction<? super T, ? super U, ? extends U> f);

  T get();

  <C> RichIterable<Tuple2<? extends C, RichIterable<T>>> groupBy(Function<? super T, ? extends C> classifier);

  <C> io.vavr.collection.Map<C, RichIterable<T>> groupByAsRichIterable(Function<? super T, ? extends C> classifier);

  <C> io.vavr.collection.Map<C, Iterator<T>> groupByAsVavrIterator(Function<? super T, ? extends C> classifier);

  RichIterable<Seq<T>> grouped(int size);

  boolean hasDefiniteSize();

  T head();

  RichIterable<T> init();

  Option<RichIterable<T>> initOption();

  boolean isAsync();

  boolean isEmpty();

  boolean isLazy();

  boolean isTraversableAgain();

  boolean isSequential();

  T last();

  int length();

  <U> RichIterable<U> map(Function<? super T, ? extends U> mapper);

  RichIterable<T> orElse(Iterable<? extends T> other);

  RichIterable<T> orElse(Supplier<? extends Iterable<? extends T>> supplier);

  Tuple2<RichIterable<T>, RichIterable<T>> partition(Predicate<? super T> predicate);

  RichIterable<T> peek(Consumer<? super T> action);

  T reduceLeft(BiFunction<? super T, ? super T, ? extends T> op);

  T reduceRight(BiFunction<? super T, ? super T, ? extends T> op);

  RichIterable<T> replace(T currentElement, T newElement);

  RichIterable<T> replaceAll(T currentElement, T newElement);

  RichIterable<T> retainAll(Iterable<? extends T> elements);

  Traversable<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation);

  <U> RichIterable<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation);

  <U> RichIterable<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation);

  RichIterable<Seq<T>> slideBy(Function<? super T, ?> classifier);

  RichIterable<Seq<T>> sliding(int size);

  RichIterable<Seq<T>> sliding(int size, int step);

  Tuple2<RichIterable<T>, RichIterable<T>> span(Predicate<? super T> predicate);

  String stringPrefix();

  RichIterable<T> tail();

  Option<RichIterable<T>> tailOption();

  RichIterable<T> take(int n);

  RichIterable<T> takeRight(int n);

  RichIterable<T> takeUntil(Predicate<? super T> predicate);

  RichIterable<T> takeWhile(Predicate<? super T> predicate);

  boolean isCollection();

  /**Select a subset and total size in one iteration. The iteration might need to consume remaining elements when size is called.
   * For performance reasons size might be the same on map operations while for filter elements might be needed to be processed.*/
  Tuple2<RichIterable<T>, Integer> page(int offset, int pageSize);

  RichIterable<T> debug(boolean debugEnabled);
}