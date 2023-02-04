package ru.yandex.realty.util.iterators

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
  * author: rmuzhikov
  */
@RunWith(classOf[JUnitRunner])
class RoundRobinIteratorTest extends FlatSpec with Matchers {

  behavior.of("RoundRobinIterator")

  it should "be empty for empty iterators" in {
    val iter = new RoundRobinIterator[Int](List(List.empty.iterator, List.empty.iterator, List.empty.iterator))
    iter.hasNext should be(false)
    assertThrows[Exception](iter.next())
  }

  it should "be non empty for non empty iterators" in {
    val iter = new RoundRobinIterator[Int](List(List(1, 3, 5).iterator, List.empty.iterator, List(2, 4, 6, 7).iterator))
    iter.toList should be(List(1, 2, 3, 4, 5, 6, 7))
  }
}
