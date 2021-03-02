package org.raisercostin.jedio.impl;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.raisercostin.jedio.ChangeableLocation;

public interface ChangeableLocationLike<SELF extends @NonNull ChangeableLocationLike<@NonNull SELF>>
    extends BasicFileLocationLike<@NonNull SELF>, DirLocationLike<@NonNull SELF>, ChangeableLocation {
}
