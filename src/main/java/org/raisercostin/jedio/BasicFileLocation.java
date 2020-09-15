package org.raisercostin.jedio;

import org.raisercostin.jedio.op.DeleteOptions;

public interface BasicFileLocation extends ExistingLocation {

  NonExistingLocation deleteFile();

  NonExistingLocation deleteFile(DeleteOptions options);

  void rename(WritableFileLocation writableFileLocation);
}
