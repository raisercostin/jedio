package org.raisercostin.jedio;

public interface DirLocation<SELF extends DirLocation<SELF>>
    extends ReadableDirLocation<SELF>, WritableDirLocation<SELF> {
}
