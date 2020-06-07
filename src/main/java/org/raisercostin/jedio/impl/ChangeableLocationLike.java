package org.raisercostin.jedio.impl;

import org.raisercostin.jedio.ChangeableLocation;

public interface ChangeableLocationLike<SELF extends ChangeableLocationLike<SELF>>
    extends BasicFileLocationLike<SELF>, DirLocationLike<SELF>, ChangeableLocation {
}
