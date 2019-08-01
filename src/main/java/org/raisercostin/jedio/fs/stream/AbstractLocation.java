package org.raisercostin.jedio.fs.stream;

import java.io.InputStream;
import java.util.function.Function;

import io.vavr.control.Option;
import org.raisercostin.jedio.ChangeableLocation;
import org.raisercostin.jedio.DirLocation;
import org.raisercostin.jedio.ExistingLocation;
import org.raisercostin.jedio.FileLocation;
import org.raisercostin.jedio.LinkLocation;
import org.raisercostin.jedio.NonExistingLocation;
import org.raisercostin.jedio.ReadableFileLocation;
import org.raisercostin.jedio.ReferenceLocation;
import org.raisercostin.jedio.RelativeLocation;
import org.raisercostin.jedio.WritableFileLocation;
import org.raisercostin.jedio.find.FileTraversal2;
import org.raisercostin.jedio.find.PathWithAttributes;
import org.raisercostin.jedio.op.DeleteOptions;
import org.raisercostin.jedio.path.PathLocation;
import reactor.core.publisher.Flux;

public abstract class AbstractLocation implements DirLocation, ExistingLocation, ReferenceLocation, ReadableFileLocation {
  @Override
  public NonExistingLocation deleteFile(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public void rename(FileLocation asWritableFile) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation delete(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public ReferenceLocation child(RelativeLocation path) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String absolute() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String normalized() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String canonical() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String absoluteAndNormalized() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String real() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String getName() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<RelativeLocation> stripAncestor(DirLocation x) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<ReferenceLocation> findAncestor(Function<ReferenceLocation, Boolean> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public PathLocation makeDirOnParentIfNeeded() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<? extends ReferenceLocation> parent() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<DirLocation> existing() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<NonExistingLocation> nonExisting() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation nonExistingOrElse(Function<DirLocation, NonExistingLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public DirLocation existingOrElse(Function<NonExistingLocation, DirLocation> fn) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean exists() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public WritableFileLocation asWritableFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public ReadableFileLocation asReadableFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean isDir() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean isFile() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public void symlinkTo(ReferenceLocation parent) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public void junctionTo(ReferenceLocation parent) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<LinkLocation> asSymlink() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public boolean isSymlink() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<PathWithAttributes> find(FileTraversal2 traversal, String filter, boolean recursive, String gitIgnore, boolean dirsFirst) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public ReferenceLocation create(String path) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public DirLocation asDir() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public long length() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Option<String> readIfExists() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public InputStream unsafeInputStream() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public String readContent() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public NonExistingLocation deleteDir(DeleteOptions options) {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public ChangeableLocation asChangableLocation() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<ExistingLocation> findFilesAndDirs() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<FileLocation> findFiles() {
    throw new RuntimeException("Not implemented yet!!!");
  }

  @Override
  public Flux<DirLocation> findDirs() {
    throw new RuntimeException("Not implemented yet!!!");
  }
}
