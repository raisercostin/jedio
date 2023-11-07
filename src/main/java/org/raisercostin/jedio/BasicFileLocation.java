package org.raisercostin.jedio;

import org.raisercostin.jedio.op.DeleteOptions;

public interface BasicFileLocation extends ExistingLocation {

  NonExistingLocation deleteFile();

  NonExistingLocation deleteFile(DeleteOptions options);

  <T extends WritableFileLocation> T rename(T writableFileLocation);

  default <T extends WritableFileLocation> T renamedIfExist() {
    return rename((T) backupName());
  }
}
