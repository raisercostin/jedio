package org.raisercostin.jedio;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter(value = AccessLevel.NONE)
@Setter(value = AccessLevel.NONE)
public class FileAltered {
  public final Path dir;
  public final WatchEvent<?> event;

  public ReferenceLocation location() {
    return Locations.dir(dir.resolve((Path) event.context()));
  }

  public String toString() {
    return location() + " " + event.kind().name() + "#" + event.count();
  }
}
// FileCreated, FileChanged, FileDeleted, DirectoryCreated,
// /**
// * The Changed event is raised when changes are made to the size, system
// attributes, last write time, last access time, or security permissions of a
// file or directory in the directory being monitored.
// * @see
// https://msdn.microsoft.com/en-us/library/system.io.filesystemwatcher.changed(v=vs.110).aspx
// */
// DirectoryChanged, DirectoryDeleted
// }
// enui
// abstract class FileAltered {
// //lazy val location: FileLocation = Locations.file(file)
// //protected def file: File
// }
// class FileCreated extends FileAltered
// class FileChanged extends FileAltered
// class FileDeleted extends FileAltered
// class DirectoryCreated extends FileAltered
// class DirectoryChanged extends FileAltered
// class DirectoryDeleted extends FileAltered