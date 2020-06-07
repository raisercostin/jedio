package org.raisercostin.jedio;

import org.raisercostin.jedio.impl.NonExistingLocationLike;
import org.raisercostin.jedio.op.DeleteOptions;

public interface WritableDirLocation extends BasicDirLocation {
  NonExistingLocationLike deleteDir(DeleteOptions options);
}