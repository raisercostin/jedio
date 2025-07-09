package org.raisercostin.jedio;

import org.raisercostin.jedio.op.DeleteOptions;

public interface BasicFileLocation extends ExistingLocation {

  NonExistingLocation deleteFile();

  NonExistingLocation deleteFile(DeleteOptions options);

  <T extends WritableFileLocation> T rename(T writableFileLocation);

  /**Returns another name derived from current name with a counter at the end.*/
  default <T extends WritableFileLocation> T renamedIfExist() {
    if (!exists()) {
      return (T) this;
    }
    return (T) backupName();
  }

  /**Renames the existing file to a name with a counter at the end, and keeps the name.*/
  default <T extends WritableFileLocation> T backupIfExists() {
    if (!exists()) {
      return (T) this;
    }
    rename((T) backupName());
    return (T) this;
  }
}
