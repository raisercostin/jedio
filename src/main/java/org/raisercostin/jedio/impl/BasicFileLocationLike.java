package org.raisercostin.jedio.impl;

import org.jedio.sugar;
import org.raisercostin.jedio.BasicFileLocation;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.op.DeleteOptions;

public interface BasicFileLocationLike<SELF extends BasicFileLocationLike<SELF>>
    extends ExistingLocationLike<SELF>, BasicFileLocation {

  @Override
  @sugar
  default NonExistingLocation deleteFile() {
    return deleteFile(DeleteOptions.deleteDefault());
  }

  @Override
  default NonExistingLocation deleteFile(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  default void rename(WritableFileLocation writableFileLocation) {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
