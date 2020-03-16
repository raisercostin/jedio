package org.raisercostin.jedio;

import org.raisercostin.jedio.op.DeleteOptions;

/** Location that is known to exist and that can have child locations. */
public interface WritableDirLocation extends BasicDirLocation {
  NonExistingLocation deleteDir(DeleteOptions options);
}
