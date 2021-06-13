package org.raisercostin.jedio.op;

import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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
  @ToString.Exclude
  public final OperationListener operationListener = OperationListener.defaultListener;

  @Override
  public boolean acceptStreamAndMeta(StreamAndMeta streamAndMeta) {
    return this.acceptStreamAndMeta.test(streamAndMeta);
  }

  public SimpleCopyOptions filterHttpContentTypeHtml() {
    return withAcceptStreamAndMeta(x -> x.meta.httpResponseHeaderContentTypeIsHtml());
  }

  @Override
  public boolean throwOnError() {
    return this.throwIfIgnored;
  }

  @Override
  public boolean replaceExisting() {
    return this.replaceExisting;
  }

  public CopyOptions withDefaultReporting() {
    return withOperationListener(OperationListener.defaultListener);
  }

  @Override
  public boolean reportSteps() {
    return this.operationListener != null;
  }

  @Override
  public void reportOperationEvent(CopyEvent event, ExistingLocation src, ReferenceLocation dst, Object... args) {
    if (this.operationListener != null) {
      this.operationListener.reportOperationEvent(event, null, src, dst, args);
    }
  }

  @Override
  public void reportOperationEvent(CopyEvent event, Throwable exception, ExistingLocation src, ReferenceLocation dst,
      Object... args) {
    if (this.operationListener != null) {
      this.operationListener.reportOperationEvent(event, exception, src, dst, args);
    }
  }

  @Override
  public boolean copyMeta() {
    return this.copyMeta;
  }
}
