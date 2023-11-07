package org.raisercostin.jedio;

import org.raisercostin.jedio.op.DeleteOptions;

public interface BasicFileLocation extends ExistingLocation {

  NonExistingLocation deleteFile();

  NonExistingLocation deleteFile(DeleteOptions options);

  <T extends WritableFileLocation> T rename(T writableFileLocation);

  default <T extends WritableFileLocation> T renamedIfExist() {
    if (!exists()) {
      return (T) this;
    }
    return rename((T) backupName());
  }
}
