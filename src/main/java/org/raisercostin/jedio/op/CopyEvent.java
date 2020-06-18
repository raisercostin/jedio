package org.raisercostin.jedio.op;

public enum CopyEvent {
  Unknown,
  CopyFileTriggered(
      "Copy file triggered."),
  CopyIgnoreSourceDoesNotExists,
  CopyIgnoreDestinationMetaExists,
  CopyIgnoreDestinationExists,
  CopyIgnoreContentType,
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
