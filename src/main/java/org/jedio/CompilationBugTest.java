package org.jedio;

import java.util.List;

public class CompilationBugTest {
  public interface Foo {
    <T extends Foo> List<T> existing();
  }

  public interface FooLike<SELF extends FooLike<SELF>> extends Foo {
    @SuppressWarnings("unchecked")
    // eclipse warning:
    // Type safety: The return type List<SELF> for existing() from the type CompilationBugTest.FooLike<SELF> needs
    // unchecked conversion to conform to List<CompilationBugTest.Foo> from the type CompilationBugTest.Foo
    @Override
    default List<SELF> existing() {
      throw new RuntimeException("Not implemented yet!!!");
    }
  }

  /**
   * - eclipse - Version: 2019-09 R (4.13.0) - Build id: 20190917-1200 - no warning or error - maven - using default 1.8
   * compiler, maven-compiler-plugin:3.8.1 - error: types Foo and FooLike<Bar> are incompatible; both define existing(),
   * but with unrelated return types
   */
  public interface Bar extends FooLike<Bar>, Foo {
    @Override
    default List<Bar> existing() {
      return FooLike.super.existing();
    }
  }
}
