package org.jedio.struct;

import java.io.Closeable;
import java.util.Iterator;

/**
 * A {@link CloseableIterator} serves as a bridging data structure for the underlying data store specific results that
 * can be wrapped in a Java 8 {@link java.util.stream.Stream}. This allows implementations to clean up any resources
 * they need to keep open to iterate over elements.
 *
 * @author Thomas Darimont
 * @param <T>
 * @since 1.10
 */
public interface CloseableIterator<T> extends Iterator<T>, Closeable {

  /*
   * (non-Javadoc)
   * @see java.io.Closeable#close()
   */
  @Override
  void close();
}
