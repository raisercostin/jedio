package org.raisercostin.jedio;

import org.raisercostin.jedio.op.DeleteOptions;

public interface WritableDirLocation extends BasicDirLocation {
  NonExistingLocation deleteDir(DeleteOptions options);
}
