package org.raisercostin.jedio;

import org.raisercostin.jedio.op.DeleteOptions;
import org.raisercostin.util.sugar;

public interface FileLocation extends ExistingLocation {
  @sugar
  default NonExistingLocation deleteFile() {
    return deleteFile(DeleteOptions.deleteDefault());
  }

  NonExistingLocation deleteFile(DeleteOptions options);

  void rename(FileLocation asWritableFile);
}
