package org.raisercostin.jedio.op;

import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.With;
import org.raisercostin.jedio.ExistingLocation;
import org.raisercostin.jedio.MetaInfo.StreamAndMeta;
import org.raisercostin.jedio.ReferenceLocation;

@Data
@Getter(value = AccessLevel.NONE)
@Setter(value = AccessLevel.NONE)
@With
@Builder
public class SimpleCopyOptions implements CopyOptions {
  public final boolean replaceExisting;
  public final boolean copyMeta;
  public final boolean throwIfIgnored;
  public final Predicate<StreamAndMeta> acceptStreamAndMeta;
  @Builder.Default
  public final OperationListener operationListener = OperationListener.defaultListener;

  @Override
  public boolean acceptStreamAndMeta(StreamAndMeta streamAndMeta) {
    return acceptStreamAndMeta.test(streamAndMeta);
  }

  public SimpleCopyOptions filterHttpContentTypeHtml() {
    return withAcceptStreamAndMeta(x -> x.meta.httpResponseHeaderContentTypeIsHtml());
  }

  @Override
  public boolean throwOnError() {
    return throwIfIgnored;
  }

  @Override
  public boolean replaceExisting() {
    return replaceExisting;
  }

  public CopyOptions withDefaultReporting() {
    return withOperationListener(OperationListener.defaultListener);
  }

  @Override
  public boolean reportSteps() {
    return operationListener != null;
  }

  @Override
  public void reportOperationEvent(CopyEvent event, ExistingLocation src, ReferenceLocation dst, Object... args) {
    if (operationListener != null) {
      operationListener.reportOperationEvent(event, null, src, dst, args);
    }
  }

  @Override
  public void reportOperationEvent(CopyEvent event, Throwable exception, ExistingLocation src, ReferenceLocation dst,
      Object... args) {
    if (operationListener != null) {
      operationListener.reportOperationEvent(event, exception, src, dst, args);
    }
  }

  @Override
  public boolean copyMeta() {
    return copyMeta;
  }
}