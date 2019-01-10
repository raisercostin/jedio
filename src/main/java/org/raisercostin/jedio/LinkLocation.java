package org.raisercostin.jedio;

/**
 * A location that just points to other location. Could be created using
 * <li>hardlinks @see {@link java.nio.file.Files#createLink}
 * <li>symbolik links @see {@link java.nio.file.Files#createSymbolicLink}
 * <li>windows - mklink - Creates a file symbolic link.
 * <li>windows - mklink /D - Creates a directory symbolic link.
 * <li>windows - mklink /H - Creates a hard link instead of a symbolic link.
 * <li>windows - mklink /J - Creates a Directory Junction.
 * <li>shortcut (windows)
 * <li>redirect? etc.
 */
public interface LinkLocation extends ReferenceLocation {
  ReferenceLocation getTarget();
}
