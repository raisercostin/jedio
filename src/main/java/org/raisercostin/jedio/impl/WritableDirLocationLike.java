package org.raisercostin.jedio.impl;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.WritableDirLocation;
import org.raisercostin.jedio.op.DeleteOptions;

/** Location that is known to exist and that can have child locations. */
public interface WritableDirLocationLike<SELF extends @NonNull WritableDirLocationLike<SELF>>
    extends BasicDirLocationLike<SELF>, WritableDirLocation {
  @Override
  NonExistingLocation deleteDir(DeleteOptions options);
}
