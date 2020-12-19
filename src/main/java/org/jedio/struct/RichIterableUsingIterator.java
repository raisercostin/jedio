package org.jedio.struct;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
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

import com.google.common.base.Preconditions;
import io.vavr.CheckedFunction0;
import io.vavr.PartialFunction;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.Value;
import io.vavr.collection.Array;
import io.vavr.collection.CharSeq;
import io.vavr.collection.Iterator;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.PriorityQueue;
import io.vavr.collection.Queue;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.SortedMap;
import io.vavr.collection.SortedSet;
import io.vavr.collection.Traversable;
import io.vavr.collection.Tree;
import io.vavr.collection.Vector;
import io.vavr.collection.Tree.Node;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import io.vavr.control.Validation;

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

/**
 * Intentionally doesn't implement Iterable to force you to pass RichIterable around.
 * You can get that with iterable().
 */
public class RichIterableUsingIterator<T> implements RichIterable2<T> {
  private Iterable<T> iterable;

  public RichIterableUsingIterator(Iterable<T> iterable) {
    this.iterable = iterable;
  }

  @Override
  public Iterable<T> iterable() {
    return iterable;
  }

  @Override
  public Iterator<T> iterator() {
    return Iterator.ofAll(iterable.iterator());
  }

  public <C> Map<C, Iterator<T>> groupBy2(Function<? super T, ? extends C> classifier) {
    //public <C> Map<? extends C, Iterator<T>> groupBy(Function<? super T, ? extends C> classifier) {
    return groupBy(this, classifier, Iterator::ofAll);
  }

  //Copied from vavr Collections.groupBy
  private static <T, C, R extends Iterable<T>> Map<C, R> groupBy(RichIterable2<T> source,
      Function<? super T, ? extends C> classifier, Function<? super Iterable<T>, R> mapper) {
    Objects.requireNonNull(classifier, "classifier is null");
    Objects.requireNonNull(mapper, "mapper is null");
    Map<C, R> results = LinkedHashMap.empty();
    for (java.util.Map.Entry<? extends C, Collection<T>> entry : groupBy(source, classifier)) {
      results = results.put(entry.getKey(), mapper.apply(entry.getValue()));
    }
    return results;
  }

  private static <T, C> java.util.Set<java.util.Map.Entry<C, Collection<T>>> groupBy(RichIterable2<T> source,
      Function<? super T, ? extends C> classifier) {
    final java.util.Map<C, Collection<T>> results = new java.util.LinkedHashMap<>(
      source.isTraversableAgain() ? source.size() : 16);
    for (T value : source.iterator()) {
      final C key = classifier.apply(value);
      results.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }
    return results.entrySet();
  }

  @SuppressWarnings("unchecked")
  public <U extends T> RichIterable2<U> narrow(Class<U> clazz) {
    return (RichIterable2<U>) (Object) new RichIterableUsingIterator<>(
      () -> iterator().filter(y -> y.getClass() == clazz));
  }

  /**Computes and stores the result of iterable. Creates a new RichIterator based on this.*/
  public RichIterable2<T> memoizeVavr() {
    return RichIterable2.fromVavr(toList());
  }

  public RichIterable2<T> memoizeJava() {
    return RichIterable2.fromJava(toJavaList());
  }

  public RichIterable2<T> concat(Iterable<T> next) {
    return RichIterable2.concat(this, new RichIterableUsingIterator<>(next));
  }

  public RichIterable2<T> concat(RichIterable2<T> next) {
    return RichIterable2.concat(this, next);
  }

  @Deprecated //Try not to use this, call toStream() before for example. The iterable is a little to heavy to be used like this.
  public T get(int index) {
    return drop(index).head();
  }

  public <U extends Comparable<? super U>> T findMaxBy(Function<? super T, ? extends U> mapper) {
    return sortBy(mapper).last();
  }

  public RichIterableUsingIterator<T> sorted(Comparator<? super T> comparator) {
    //Do not use lazy as this will cache the value. The user should decide if he wants that via using memoize
    //Lazy<java.util.List<T>> sorted =Lazy.of(
    Supplier<java.util.List<T>> sorted = () -> {
      java.util.List<T> list = iterator().toJavaList();
      list.sort(comparator);
      return list;
    };
    return new RichIterableUsingIterator<>(() -> sorted.get().iterator());
  }

  public <U extends Comparable<? super U>> RichIterable2<T> sortBy(Function<? super T, ? extends U> mapper) {
    return sorted(comparator(mapper));
  }

  public <U extends Comparable<? super U>> RichIterable2<T> sortByReversed(Function<? super T, ? extends U> mapper) {
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

  public RichIterable2<T> reverse() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  /**********************************************************************/
  /** Next operations just delegate to `vavr.Iterator` */
  /**********************************************************************/
  @Override
  public T fold(T zero, BiFunction<? super T, ? super T, ? extends T> combine) {
    return iterator().fold(zero, combine);
  }

  @Override
  public T reduce(BiFunction<? super T, ? super T, ? extends T> op) {
    return iterator().reduce(op);
  }

  @Override
  public Option<T> reduceOption(BiFunction<? super T, ? super T, ? extends T> op) {
    return iterator().reduceOption(op);
  }

  @Override
  public <R, A> R collect(Collector<? super T, A, R> collector) {
    return iterator().collect(collector);
  }

  @Override
  public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
    return iterator().collect(supplier, accumulator, combiner);
  }

  @Override
  public boolean contains(T element) {
    return iterator().contains(element);
  }

  @Override
  public <U> boolean corresponds(Iterable<U> that, BiPredicate<? super T, ? super U> predicate) {
    return iterator().corresponds(that, predicate);
  }

  @Override
  public boolean eq(Object o) {
    return iterator().eq(o);
  }

  @Override
  public boolean exists(Predicate<? super T> predicate) {
    return iterator().exists(predicate);
  }

  @Override
  public boolean forAll(Predicate<? super T> predicate) {
    return iterator().forAll(predicate);
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    iterator().forEach(action);
  }

  @Override
  public T getOrElse(T other) {
    return iterator().getOrElse(other);
  }

  @Override
  public T getOrElse(Supplier<? extends T> supplier) {
    return iterator().getOrElse(supplier);
  }

  @Override
  public <X extends Throwable> T getOrElseThrow(Supplier<X> supplier) throws X {
    return iterator().getOrElseThrow(supplier);
  }

  @Override
  public T getOrElseTry(CheckedFunction0<? extends T> supplier) {
    return iterator().getOrElseTry(supplier);
  }

  @Override
  public T getOrNull() {
    return iterator().getOrNull();
  }

  @Override
  public void out(PrintStream out) {
    iterator().out(out);
  }

  @Override
  public void out(PrintWriter writer) {
    iterator().out(writer);
  }

  @Override
  public void stderr() {
    iterator().stderr();
  }

  @Override
  public void stdout() {
    iterator().stdout();
  }

  @Override
  public Array<T> toArray() {
    return iterator().toArray();
  }

  @Override
  public CharSeq toCharSeq() {
    return iterator().toCharSeq();
  }

  @Override
  public CompletableFuture<T> toCompletableFuture() {
    return iterator().toCompletableFuture();
  }

  @Override
  public Object[] toJavaArray() {
    return iterator().toJavaArray();
  }

  @Override
  public T[] toJavaArray(Class<T> componentType) {
    return iterator().toJavaArray(componentType);
  }

  @Override
  public T[] toJavaArray(IntFunction<T[]> arrayFactory) {
    return iterator().toJavaArray(arrayFactory);
  }

  @Override
  public <C extends Collection<T>> C toJavaCollection(Function<Integer, C> factory) {
    return iterator().toJavaCollection(factory);
  }

  @Override
  public java.util.List<T> toJavaList() {
    return iterator().toJavaList();
  }

  @Override
  public <LIST extends java.util.List<T>> LIST toJavaList(Function<Integer, LIST> factory) {
    return iterator().toJavaList(factory);
  }

  @Override
  public <K, V> java.util.Map<K, V> toJavaMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
    return iterator().toJavaMap(f);
  }

  @Override
  public <K, V, MAP extends java.util.Map<K, V>> MAP toJavaMap(Supplier<MAP> factory,
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper) {
    return iterator().toJavaMap(factory, keyMapper, valueMapper);
  }

  @Override
  public <K, V, MAP extends java.util.Map<K, V>> MAP toJavaMap(Supplier<MAP> factory,
      Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
    return iterator().toJavaMap(factory, f);
  }

  @Override
  public Optional<T> toJavaOptional() {
    return iterator().toJavaOptional();
  }

  @Override
  public java.util.Set<T> toJavaSet() {
    return iterator().toJavaSet();
  }

  @Override
  public <SET extends java.util.Set<T>> SET toJavaSet(Function<Integer, SET> factory) {
    return iterator().toJavaSet(factory);
  }

  @Override
  public Stream<T> toJavaStream() {
    return iterator().toJavaStream();
  }

  @Override
  public Stream<T> toJavaParallelStream() {
    return iterator().toJavaParallelStream();
  }

  @Override
  public io.vavr.collection.List<T> toList() {
    return iterator().toList();
  }

  @Override
  public <K, V> io.vavr.collection.Map<K, V> toMap(Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper) {
    return iterator().toMap(keyMapper, valueMapper);
  }

  @Override
  public <K, V> io.vavr.collection.Map<K, V> toMap(Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
    return iterator().toMap(f);
  }

  @Override
  public <K, V> io.vavr.collection.Map<K, V> toLinkedMap(Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper) {
    return iterator().toLinkedMap(keyMapper, valueMapper);
  }

  @Override
  public <K, V> io.vavr.collection.Map<K, V> toLinkedMap(
      Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
    return iterator().toLinkedMap(f);
  }

  @Override
  public <K extends Comparable<? super K>, V> SortedMap<K, V> toSortedMap(Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends V> valueMapper) {
    return iterator().toSortedMap(keyMapper, valueMapper);
  }

  @Override
  public <K extends Comparable<? super K>, V> SortedMap<K, V> toSortedMap(
      Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
    return iterator().toSortedMap(f);
  }

  @Override
  public <K, V> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator,
      Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
    return iterator().toSortedMap(comparator, keyMapper, valueMapper);
  }

  @Override
  public <K, V> SortedMap<K, V> toSortedMap(Comparator<? super K> comparator,
      Function<? super T, ? extends Tuple2<? extends K, ? extends V>> f) {
    return iterator().toSortedMap(comparator, f);
  }

  @Override
  public Option<T> toOption() {
    return iterator().toOption();
  }

  @Override
  public <L> Either<L, T> toEither(L left) {
    return iterator().toEither(left);
  }

  @Override
  public <L> Either<L, T> toEither(Supplier<? extends L> leftSupplier) {
    return iterator().toEither(leftSupplier);
  }

  @Override
  public <E> Validation<E, T> toValidation(E invalid) {
    return iterator().toValidation(invalid);
  }

  @Override
  public <E> Validation<E, T> toValidation(Supplier<? extends E> invalidSupplier) {
    return iterator().toValidation(invalidSupplier);
  }

  @Override
  public Queue<T> toQueue() {
    return iterator().toQueue();
  }

  @Override
  public PriorityQueue<T> toPriorityQueue() {
    return iterator().toPriorityQueue();
  }

  @Override
  public PriorityQueue<T> toPriorityQueue(Comparator<? super T> comparator) {
    return iterator().toPriorityQueue(comparator);
  }

  @Override
  public io.vavr.collection.Set<T> toSet() {
    return iterator().toSet();
  }

  @Override
  public io.vavr.collection.Set<T> toLinkedSet() {
    return iterator().toLinkedSet();
  }

  @Override
  public SortedSet<T> toSortedSet() throws ClassCastException {
    return iterator().toSortedSet();
  }

  @Override
  public SortedSet<T> toSortedSet(Comparator<? super T> comparator) {
    return iterator().toSortedSet(comparator);
  }

  @Override
  public io.vavr.collection.Stream<T> toStream() {
    return iterator().toStream();
  }

  @Override
  public Try<T> toTry() {
    return iterator().toTry();
  }

  @Override
  public Try<T> toTry(Supplier<? extends Throwable> ifEmpty) {
    return iterator().toTry(ifEmpty);
  }

  @Override
  public Tree<T> toTree() {
    return iterator().toTree();
  }

  @Override
  public <ID> io.vavr.collection.List<Node<T>> toTree(Function<? super T, ? extends ID> idMapper,
      Function<? super T, ? extends ID> parentMapper) {
    return iterator().toTree(idMapper, parentMapper);
  }

  @Override
  public Vector<T> toVector() {
    return iterator().toVector();
  }

  @Override
  public <K> Option<io.vavr.collection.Map<K, T>> arrangeBy(Function<? super T, ? extends K> getKey) {
    return iterator().arrangeBy(getKey);
  }

  @Override
  public Option<Double> average() {
    return iterator().average();
  }

  @Override
  public boolean containsAll(Iterable<? extends T> elements) {
    return iterator().containsAll(elements);
  }

  @Override
  public int count(Predicate<? super T> predicate) {
    return iterator().count(predicate);
  }

  @Override
  public boolean existsUnique(Predicate<? super T> predicate) {
    return iterator().existsUnique(predicate);
  }

  @Override
  public Option<T> find(Predicate<? super T> predicate) {
    return iterator().find(predicate);
  }

  @Override
  public <U> U foldLeft(U zero, BiFunction<? super U, ? super T, ? extends U> f) {
    return iterator().foldLeft(zero, f);
  }

  @Override
  public void forEachWithIndex(ObjIntConsumer<? super T> action) {
    iterator().forEachWithIndex(action);
  }

  @Override
  public Option<T> headOption() {
    return iterator().headOption();
  }

  @Override
  public boolean isDistinct() {
    return iterator().isDistinct();
  }

  @Override
  public boolean isOrdered() {
    return iterator().isOrdered();
  }

  @Override
  public boolean isSingleValued() {
    return iterator().isSingleValued();
  }

  @Override
  public Option<T> lastOption() {
    return iterator().lastOption();
  }

  @Override
  public Option<T> max() {
    return iterator().max();
  }

  @Override
  public Option<T> maxBy(Comparator<? super T> comparator) {
    return iterator().maxBy(comparator);
  }

  @Override
  public <U extends Comparable<? super U>> Option<T> maxBy(Function<? super T, ? extends U> f) {
    return iterator().maxBy(f);
  }

  @Override
  public Option<T> min() {
    return iterator().min();
  }

  @Override
  public Option<T> minBy(Comparator<? super T> comparator) {
    return iterator().minBy(comparator);
  }

  @Override
  public <U extends Comparable<? super U>> Option<T> minBy(Function<? super T, ? extends U> f) {
    return iterator().minBy(f);
  }

  @Override
  public CharSeq mkCharSeq() {
    return iterator().mkCharSeq();
  }

  @Override
  public CharSeq mkCharSeq(CharSequence delimiter) {
    return iterator().mkCharSeq(delimiter);
  }

  @Override
  public CharSeq mkCharSeq(CharSequence prefix, CharSequence delimiter, CharSequence suffix) {
    return iterator().mkCharSeq(prefix, delimiter, suffix);
  }

  @Override
  public String mkString() {
    return iterator().mkString();
  }

  @Override
  public String mkString(CharSequence delimiter) {
    return iterator().mkString(delimiter);
  }

  @Override
  public String mkString(CharSequence prefix, CharSequence delimiter, CharSequence suffix) {
    return iterator().mkString(prefix, delimiter, suffix);
  }

  @Override
  public boolean nonEmpty() {
    return iterator().nonEmpty();
  }

  @Override
  public Number product() {
    return iterator().product();
  }

  @Override
  public Option<T> reduceLeftOption(BiFunction<? super T, ? super T, ? extends T> op) {
    return iterator().reduceLeftOption(op);
  }

  @Override
  public Option<T> reduceRightOption(BiFunction<? super T, ? super T, ? extends T> op) {
    return iterator().reduceRightOption(op);
  }

  @Override
  public T single() {
    return iterator().single();
  }

  @Override
  public Option<T> singleOption() {
    return iterator().singleOption();
  }

  @Override
  public int size() {
    return iterator().size();
  }

  @Override
  public Spliterator<T> spliterator() {
    return iterator().spliterator();
  }

  @Override
  public Number sum() {
    return iterator().sum();
  }

  @Override
  public <R> RichIterable2<R> collect(PartialFunction<? super T, ? extends R> partialFunction) {
    return new RichIterableUsingIterator<>(() -> iterator().collect(partialFunction));
  }

  @Override
  public RichIterable2<T> concat(java.util.Iterator<? extends T> that) {
    return new RichIterableUsingIterator<>(() -> iterator().concat(that));
  }

  @Override
  public RichIterable2<T> intersperse(T element) {
    return new RichIterableUsingIterator<>(() -> iterator().intersperse(element));
  }

  @Override
  public <U> U transform(Function<? super Iterator<T>, ? extends U> f) {
    return iterator().transform(f);
  }

  @Override
  public <U> RichIterable2<Tuple2<T, U>> zip(Iterable<? extends U> that) {
    return new RichIterableUsingIterator<>(() -> iterator().zip(that));
  }

  @Override
  public <U, R> RichIterable2<R> zipWith(Iterable<? extends U> that,
      BiFunction<? super T, ? super U, ? extends R> mapper) {
    return new RichIterableUsingIterator<>(() -> iterator().zipWith(that, mapper));
  }

  @Override
  public <U> RichIterable2<Tuple2<T, U>> zipAll(Iterable<? extends U> that, T thisElem, U thatElem) {
    return new RichIterableUsingIterator<>(() -> iterator().zipAll(that, thisElem, thatElem));
  }

  @Override
  public RichIterable2<Tuple2<T, Integer>> zipWithIndex() {
    return new RichIterableUsingIterator<>(() -> iterator().zipWithIndex());
  }

  @Override
  public <U> RichIterable2<U> zipWithIndex(BiFunction<? super T, ? super Integer, ? extends U> mapper) {
    return new RichIterableUsingIterator<>(() -> iterator().zipWithIndex(mapper));
  }

  @Override
  @Deprecated //not implemented yet
  public <T1, T2> Tuple2<RichIterable2<T1>, RichIterable2<T2>> unzip(
      Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) {
    throw new RuntimeException("Not implemented yet!!!");
    //return new RichIterable3<>(() -> iterator().unzip(unzipper));
  }

  @Override
  @Deprecated //not implemented yet
  public <T1, T2, T3> Tuple3<RichIterable2<T1>, RichIterable2<T2>, RichIterable2<T3>> unzip3(
      Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
    throw new RuntimeException("Not implemented yet!!!");
    //return new RichIterable3<>(() -> iterator().unzip3(unzipper));
  }

  @Override
  public RichIterable2<T> distinct() {
    return new RichIterableUsingIterator<>(() -> iterator().distinct());
  }

  @Override
  public RichIterable2<T> distinctBy(Comparator<? super T> comparator) {
    return new RichIterableUsingIterator<>(() -> iterator().distinctBy(comparator));
  }

  @Override
  public <U> RichIterable2<T> distinctBy(Function<? super T, ? extends U> keyExtractor) {
    return new RichIterableUsingIterator<>(() -> iterator().distinctBy(keyExtractor));
  }

  @Override
  public RichIterable2<T> drop(int n) {
    return new RichIterableUsingIterator<>(() -> iterator().drop(n));
  }

  @Override
  public RichIterable2<T> dropRight(int n) {
    return new RichIterableUsingIterator<>(() -> iterator().dropRight(n));
  }

  @Override
  public RichIterable2<T> dropUntil(Predicate<? super T> predicate) {
    return new RichIterableUsingIterator<>(() -> iterator().dropUntil(predicate));
  }

  @Override
  public RichIterable2<T> dropWhile(Predicate<? super T> predicate) {
    return new RichIterableUsingIterator<>(() -> iterator().dropWhile(predicate));
  }

  @Override
  public RichIterable2<T> filter(Predicate<? super T> predicate) {
    return new RichIterableUsingIterator<>(() -> iterator().filter(predicate));
  }

  @Override
  public RichIterable2<T> reject(Predicate<? super T> predicate) {
    return new RichIterableUsingIterator<>(() -> iterator().reject(predicate));
  }

  @Override
  public Option<T> findLast(Predicate<? super T> predicate) {
    return iterator().findLast(predicate);
  }

  @Override
  public <U> RichIterable2<U> flatMap(Function<? super T, ? extends Iterable<? extends U>> mapper) {
    return new RichIterableUsingIterator<>(() -> iterator().flatMap(mapper));
  }

  @Override
  public <U> U foldRight(U zero, BiFunction<? super T, ? super U, ? extends U> f) {
    return iterator().foldRight(zero, f);
  }

  @Override
  public T get() {
    return iterator().get();
  }

  @Override
  public <C> io.vavr.collection.Map<C, Iterator<T>> groupBy(Function<? super T, ? extends C> classifier) {
    return iterator().groupBy(classifier);
  }

  @Override
  public RichIterable2<Seq<T>> grouped(int size) {
    return new RichIterableUsingIterator<>(() -> iterator().grouped(size));
  }

  @Override
  public boolean hasDefiniteSize() {
    return iterator().hasDefiniteSize();
  }

  @Override
  public T head() {
    return iterator().head();
  }

  @Override
  public RichIterable2<T> init() {
    return new RichIterableUsingIterator<>(() -> iterator().init());
  }

  @Override
  @Deprecated //not implemented yet
  public Option<RichIterable2<T>> initOption() {
    throw new RuntimeException("Not implemented yet!!!");
    //return new RichIterable3<>(() -> iterator().initOption());
  }

  @Override
  public boolean isAsync() {
    return iterator().isAsync();
  }

  @Override
  public boolean isEmpty() {
    return iterator().isEmpty();
  }

  @Override
  public boolean isLazy() {
    return iterator().isLazy();
  }

  @Override
  public boolean isTraversableAgain() {
    return iterator().isTraversableAgain();
  }

  @Override
  public boolean isSequential() {
    return iterator().isSequential();
  }

  @Override
  public T last() {
    return iterator().last();
  }

  @Override
  public int length() {
    return iterator().length();
  }

  @Override
  public <U> RichIterable2<U> map(Function<? super T, ? extends U> mapper) {
    return new RichIterableUsingIterator<>(() -> iterator().map(mapper));
  }

  @Override
  public RichIterable2<T> orElse(Iterable<? extends T> other) {
    return new RichIterableUsingIterator<>(() -> iterator().orElse(other));
  }

  @Override
  public RichIterable2<T> orElse(Supplier<? extends Iterable<? extends T>> supplier) {
    return new RichIterableUsingIterator<>(() -> iterator().orElse(supplier));
  }

  @Override
  @Deprecated //not implemented yet
  public Tuple2<RichIterable2<T>, RichIterable2<T>> partition(Predicate<? super T> predicate) {
    throw new RuntimeException("Not implemented yet!!!");
    //return new RichIterable3<>(() -> iterator().partition(predicate));
  }

  @Override
  public RichIterable2<T> peek(Consumer<? super T> action) {
    return new RichIterableUsingIterator<>(() -> iterator().peek(action));
  }

  @Override
  public T reduceLeft(BiFunction<? super T, ? super T, ? extends T> op) {
    return iterator().reduceLeft(op);
  }

  @Override
  public T reduceRight(BiFunction<? super T, ? super T, ? extends T> op) {
    return iterator().reduceRight(op);
  }

  @Override
  public RichIterable2<T> replace(T currentElement, T newElement) {
    return new RichIterableUsingIterator<>(() -> iterator().replace(currentElement, newElement));
  }

  @Override
  public RichIterable2<T> replaceAll(T currentElement, T newElement) {
    return new RichIterableUsingIterator<>(() -> iterator().replaceAll(currentElement, newElement));
  }

  @Override
  public RichIterable2<T> retainAll(Iterable<? extends T> elements) {
    return new RichIterableUsingIterator<>(() -> iterator().retainAll(elements));
  }

  @Override
  public Traversable<T> scan(T zero, BiFunction<? super T, ? super T, ? extends T> operation) {
    return iterator().scan(zero, operation);
  }

  @Override
  public <U> RichIterable2<U> scanLeft(U zero, BiFunction<? super U, ? super T, ? extends U> operation) {
    return new RichIterableUsingIterator<>(() -> iterator().scanLeft(zero, operation));
  }

  @Override
  public <U> RichIterable2<U> scanRight(U zero, BiFunction<? super T, ? super U, ? extends U> operation) {
    return new RichIterableUsingIterator<>(() -> iterator().scanRight(zero, operation));
  }

  @Override
  public RichIterable2<Seq<T>> slideBy(Function<? super T, ?> classifier) {
    return new RichIterableUsingIterator<>(() -> iterator().slideBy(classifier));
  }

  @Override
  public RichIterable2<Seq<T>> sliding(int size) {
    return new RichIterableUsingIterator<>(() -> iterator().sliding(size));
  }

  @Override
  public RichIterable2<Seq<T>> sliding(int size, int step) {
    return new RichIterableUsingIterator<>(() -> iterator().sliding(size, step));
  }

  @Override
  @Deprecated //not implemented yet
  public Tuple2<RichIterable2<T>, RichIterable2<T>> span(Predicate<? super T> predicate) {
    throw new RuntimeException("Not implemented yet!!!");
    //return new RichIterable3<>(() -> iterator().span(predicate));
  }

  @Override
  public String stringPrefix() {
    return iterator().stringPrefix();
  }

  @Override
  public RichIterable2<T> tail() {
    return new RichIterableUsingIterator<>(() -> iterator().tail());
  }

  @Override
  @Deprecated //not implemented yet
  public Option<RichIterable2<T>> tailOption() {
    throw new RuntimeException("Not implemented yet!!!");
    //return new RichIterable3<>(() -> iterator().tailOption());
  }

  @Override
  public RichIterable2<T> take(int n) {
    return new RichIterableUsingIterator<>(() -> iterator().take(n));
  }

  @Override
  public RichIterable2<T> takeRight(int n) {
    return new RichIterableUsingIterator<>(() -> iterator().takeRight(n));
  }

  @Override
  public RichIterable2<T> takeUntil(Predicate<? super T> predicate) {
    return new RichIterableUsingIterator<>(() -> iterator().takeUntil(predicate));
  }

  @Override
  public RichIterable2<T> takeWhile(Predicate<? super T> predicate) {
    return new RichIterableUsingIterator<>(() -> iterator().takeWhile(predicate));
  }
}
