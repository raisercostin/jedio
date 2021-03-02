package org.raisercostin.jedio.impl;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.raisercostin.jedio.DirLocation;

public interface DirLocationLike<SELF extends @NonNull DirLocationLike<@NonNull SELF>>
    extends DirLocation, ReadableDirLocationLike<@NonNull SELF>, WritableDirLocationLike<@NonNull SELF> {
}
