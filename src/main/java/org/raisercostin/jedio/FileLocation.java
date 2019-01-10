package org.raisercostin.jedio;

public interface FileLocation extends ExistingLocation {
  NonExistingLocation deleteFile(DeleteOptions options);

  void rename(FileLocation asWritableFile);
}
