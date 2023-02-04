package ru.yandex.auto.searcher.search.grouphandler

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner

import scala.util.Random

@RunWith(classOf[JUnitRunner])
class TestProportions extends WordSpec with Matchers {
  implicit val rnd = new Random()

  "should be equal for the same seed" in {
    implicit def rnd = new Random(123)
    def proportionalIterator: Iterator[Int] = ProportionalIterator[Int](
      Array(
        Iterator.range(1, 10).buffered,
        Iterator.range(100, 110).buffered
      ),
      Array(2, 2)
    )

    val random1 = proportionalIterator
    val random2 = proportionalIterator

    assert(random1.toList == random2.toList)
  }

  "should work with 2 iters" in {
    val i = ProportionalIterator[Int](
      Array(
        Iterator.range(1, 10).buffered,
        Iterator.range(100, 110).buffered
      ),
      Array(2, 2)
    )

    val sorted = List(1, 2, 100, 101, 3, 4, 102, 103, 5, 6, 104, 105, 7, 8, 106, 107, 9, 108, 109)
    assertGroupsAreEqual(i.toList, sorted, 4)
  }

  "should work with 4 iters" in {
    val shares = Array(3, 1, 2, 4)
    val shuffled = ProportionalIterator[Int](
      Array(
        Iterator.range(1000, 1015).buffered,
        Iterator.range(100, 110).buffered,
        Iterator.range(1, 5).buffered,
        Iterator.range(10000, 10007).buffered
      ),
      shares
    )

    val sorted = List(1, 2, 100, 1000, 1001, 1002, 10000, 10001, 10002, 10003, 3, 4, 101, 1003, 1004, 1005, 10004,
      10005, 10006, 102, 1006, 1007, 1008, 103, 1009, 1010, 1011, 104, 1012, 1013, 1014, 105, 106, 107, 108, 109)

    assert(shuffled.toSet == sorted.toSet)
  }

  private def assertGroupsAreEqual[T](shuffled: List[T], sortedResult: List[T], group: Int): Unit = {
    val tuples = shuffled
      .grouped(group)
      .zip(sortedResult.grouped(group))

    val list = tuples.toList
    println(list)
    assert(shuffled.length == sortedResult.length)
    assert(list.forall { case (l1, l2) => l1.toSet == l2.toSet })
  }

  "should merge 2 levels of iterators" in {
    val i = ProportionalIterator[Int](
      Array(
        ProportionalIterator[Int](
          Array(
            List(1, 5, 9).iterator.buffered,
            List(2, 8, 16, 32, 64, 128, 256).iterator.buffered
          ),
          Array(3, 1)
        ),
        List(3, 33, 333).iterator.buffered
      ),
      Array(1, 1)
    )

    val sorted = List(1, 3, 2, 33, 5, 333, 9, 8, 16, 32, 64, 128, 256)
    assert(i.toSet == sorted.toSet)
  }

  "should merge with nested empty iterator" in {
    val i = ProportionalIterator[Int](
      Array(
        ProportionalIterator[Int](
          Array(
            Iterator.empty.buffered,
            List(2, 8, 16, 32, 64, 128, 256).iterator.buffered
          ),
          Array(1, 1)
        ),
        List(3, 33, 333).iterator.buffered
      ),
      Array(1, 1)
    )

    val list = i.toList
    val sorted = List(2, 3, 8, 33, 16, 333, 32, 64, 128, 256)
    assert(list.toSet == sorted.toSet)
  }

  "should merge" in {
    val i = ProportionalIterator.merged[Int](
      Array(
        Iterator.range(1, 3).buffered,
        Iterator.range(100, 110).buffered,
        Iterator.range(10000, 10007).buffered
      )
    )

    val sorted =
      List(1, 100, 10000, 2, 101, 10001, 102, 10002, 103, 10003, 104, 10004, 105, 10005, 106, 10006, 107, 108, 109)

    assert(i.toSet == sorted.toSet)
  }
}
