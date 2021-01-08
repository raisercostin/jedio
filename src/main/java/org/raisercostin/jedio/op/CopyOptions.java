package org.raisercostin.jedio.op;

import java.time.Duration;

import com.google.common.base.Predicates;
import org.raisercostin.jedio.ExistingLocation;
import org.raisercostin.jedio.MetaInfo.StreamAndMeta;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.impl.ReferenceLocationLike;

public interface CopyOptions {
  // case class CopyOptions(overwriteIfAlreadyExists: Boolean = false, copyMeta:
  // Boolean, optionalMeta: Boolean, monitor: OperationMonitor =
  // LoggingOperationMonitor)

  /**
   * Default copy: Report each step in log, do not overwrite but throw exception.
   */
  static SimpleCopyOptions copyDefault() {
    return copyDoNotOverwriteAndThrow();
  }

  static SimpleCopyOptions copyDoNotOverwriteAndThrow() {
    return new SimpleCopyOptions(false, false, true, Predicates.alwaysTrue(), OperationListener.defaultListener);
  }

  static SimpleCopyOptions copyDoNotOverwriteButIgnore() {
    return new SimpleCopyOptions(false, false, false, Predicates.alwaysTrue(), OperationListener.defaultListener);
  }

  static SimpleCopyOptions copyOverwrite() {
    return new SimpleCopyOptions(true, false, true, Predicates.alwaysTrue(), OperationListener.defaultListener);
  }

  Duration timeoutOnItem = Duration.ofSeconds(1);
  Duration timeoutTotal = Duration.ofSeconds(60);
  boolean reportSteps = false;

  boolean replaceExisting();

  boolean throwOnError();

  boolean copyMeta();

  boolean acceptStreamAndMeta(StreamAndMeta streamAndMeta);

  default boolean makeDirIfNeeded() {
    return true;
  }

  default void reportOperationEvent(CopyEvent event, Throwable e, ExistingLocation src, ReferenceLocation dst,
      Object... args) {
  }

  default void reportOperationEvent(CopyEvent event, ExistingLocation src, ReferenceLocation dst, Object... args) {
  }

  default Duration timeoutOnItem() {
    return timeoutOnItem;
  }

  default Duration timeoutTotal() {
    return timeoutTotal;
  }

  default boolean reportSteps() {
    return reportSteps;
  }

  /** Destination can be changed based on the input and metadata. */
  @SuppressWarnings("unchecked")
  default <T extends WritableFileLocation> T amend(T dest, StreamAndMeta streamAndMeta) {
    // if there is no http response status code might be a simple copy from other sources
    int code = streamAndMeta.meta.httpMetaResponseStatusCode().getOrElse(200);
    if (code == 200) {
      return dest;
    } else {
      return (T) dest.meta("http" + code, "html");
    }
  }

  /**
   * This way we get the following advantages: - original file is a prefix of the #meta (easy to find all meta files
   * related to a file) - multiple metas meta-http, meta-links, etc - in totalcmder meta comes after file (with some
   * minor exceptions) - it works for empty exceptions too cons - the extension is not a usual extension (but the final
   * part is `-json`)
   *
   * com_darzar_www--http com_darzar_www--http-- com_darzar_www--http.#meta-http-json
   * com_darzar_www--http--.#meta-http-json com_darzar_www--http--favicon.ico
   * com_darzar_www--http--favicon.ico#meta-http-json com_darzar_www--http--robots.txt
   * com_darzar_www--http--robots.txt#meta-http-json com_darzar_www--http--sitemap.gz
   * com_darzar_www--http--sitemap.gz#meta-http-json com_darzar_www--http--sitemap.xml
   * com_darzar_www--http--sitemap.xml#meta-http-json com_darzar_www--http--sitemap.xml.gz
   * com_darzar_www--http--sitemap.xml.gz#meta-http-json
   */
  static <T extends ReferenceLocationLike<T>> T meta(T referenceLocation, String meta, String extension) {
    return referenceLocation.parent()
      .get()
      .child("." + meta)
      .child(referenceLocation
        .withExtension(originalExtension -> originalExtension + "#meta-" + meta + "-" + extension)
        .filename());
  }
}
