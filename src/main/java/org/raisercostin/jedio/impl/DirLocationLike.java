package org.raisercostin.jedio.impl;

import org.raisercostin.jedio.DirLocation;

public interface DirLocationLike<SELF extends DirLocationLike<SELF>>
    extends DirLocation, ReadableDirLocationLike<SELF>, WritableDirLocationLike<SELF> {
}
