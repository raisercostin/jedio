package org.raisercostin.jedio.op;

import java.time.Duration;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.With;
import org.raisercostin.jedio.ExistingLocation;
import org.raisercostin.jedio.MetaInfo.StreamAndMeta;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.WritableFileLocation;

public interface CopyOptions {
  org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CopyOptions.class);

  Duration timeoutOnItem = Duration.ofSeconds(1);
  Duration timeoutTotal = Duration.ofSeconds(60);
  boolean reportSteps = false;

  boolean replaceExisting();

  boolean copyMeta();

  default boolean makeDirIfNeeded() {
    return true;
  }

  @FunctionalInterface
  public interface OperationListener {
    void reportOperationEvent(CopyEvent event, Throwable exception, ExistingLocation<?> src, ReferenceLocation<?> dst,
        Object... args);
  }

  @Data
  @Getter(value = AccessLevel.NONE)
  @Setter(value = AccessLevel.NONE)
  @With
  public class SimpleCopyOptions implements CopyOptions {
    public final boolean replaceExisting;
    public final OperationListener operationListener;
    public final boolean copyMeta;

    @Override
    public boolean replaceExisting() {
      return replaceExisting;
    }

    public CopyOptions withDefaultReporting() {
      return this.withOperationListener((event, exception, src, dst, args) -> {
        if (exception != null) {
          log.warn("copy {}: {} -> {} details:{}. Enable debug for stacktrace.", event, src, dst, args);
          log.debug("copy {}: {} -> {} details:{}. Error with stacktrace.", event, src, dst, args, exception);
        } else {
          log.info("copy {}: {} -> {} details:{}.", event, src, dst, args);
        }
      });
    }

    @Override
    public boolean reportSteps() {
      return operationListener != null;
    }

    @Override
    public void reportOperationEvent(CopyEvent event, ExistingLocation<?> src, ReferenceLocation<?> dst,
        Object... args) {
      operationListener.reportOperationEvent(event, null, src, dst, args);
    }

    @Override
    public void reportOperationEvent(CopyEvent event, Throwable exception, ExistingLocation<?> src,
        ReferenceLocation<?> dst, Object... args) {
      operationListener.reportOperationEvent(event, exception, src, dst, args);
    }

    @Override
    public boolean copyMeta() {
      return copyMeta;
    }
  }
  // case class CopyOptions(overwriteIfAlreadyExists: Boolean = false, copyMeta:
  // Boolean, optionalMeta: Boolean, monitor: OperationMonitor =
  // LoggingOperationMonitor)

  static SimpleCopyOptions copyDefault() {
    return copyDoNotOverwrite();
  }

  static SimpleCopyOptions copyDoNotOverwrite() {
    return new SimpleCopyOptions(false, null, true);
  }

  static SimpleCopyOptions copyOverwrite() {
    return new SimpleCopyOptions(true, null, true);
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

  public enum CopyEvent {
    Unknown,
    CopyFileTriggered(
        "Copy file triggered."),
    IgnoreSourceDoesNotExists,
    IgnoreDestinationMetaExists,
    IgnoreDestinationExists,
    IgnoreContentType,
    CopyFileStarted,
    CopyReplacing(
        "A replace of content started"),
    CopyFileFinished,
    CopyFailed,
    CopyDirStarted,
    CopyDirFinished,
    CopyMeta(
        "Copy metadata. For http you will get the request and response: headers and other details. For all will get the exception and the source.")
    //
    ;

    String description;

    CopyEvent() {
      this.description = name();
    }

    CopyEvent(String description) {
      this.description = description;
    }
  }

  default void reportOperationEvent(CopyEvent event, Throwable e, ExistingLocation<?> src, ReferenceLocation<?> dst,
      Object... args) {
  }

  default void reportOperationEvent(CopyEvent event, ExistingLocation<?> src, ReferenceLocation<?> dst,
      Object... args) {
  }

  /** Destination can be changed based on the input and metadata. */
  @SuppressWarnings("unchecked")
  default <T extends WritableFileLocation<?>> T amend(T dest, StreamAndMeta streamAndMeta) {
    int code = streamAndMeta.meta.httpMetaResponseStatusCode().get();
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
  static <T extends ReferenceLocation<T>> T meta(T referenceLocation, String meta, String extension) {
    return referenceLocation.parent()
      .get()
      .child("." + meta)
      .mkdirIfNecessary()
      .child(referenceLocation.withExtension(originalExtension -> originalExtension + "#meta-" + meta + "-" + extension)
        .filename());
  }
}
