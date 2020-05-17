package org.raisercostin.jedio;

import org.raisercostin.jedio.op.DeleteOptions;
import org.raisercostin.util.sugar;

public interface FileLocation<SELF extends FileLocation<SELF>> extends ExistingLocation<SELF> {
  interface FinalFileLocation extends FileLocation<FinalFileLocation> {
  }

  @sugar
  default NonExistingLocation<?> deleteFile() {
    return deleteFile(DeleteOptions.deleteDefault());
  }

  default NonExistingLocation<?> deleteFile(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  default void rename(FileLocation<?> asWritableFile) {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
