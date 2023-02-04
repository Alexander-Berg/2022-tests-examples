package ru.yandex.auto.searcher

trait SimpleAssertions {

  def assert[A](a: A, b: A): Unit = {
    if (a != b) throw new org.junit.ComparisonFailure("not equal", a.toString, b.toString)
  }
}
