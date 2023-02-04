package auto.carfax.common.utils.collections

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.Try

class GroupByIteratorTest extends AnyWordSpecLike with Matchers {

  "grouped iterator" should {
    "group by f with last single" in {
      val a = List(1, 1, 2, 2, 5)
      val it = a.iterator.groupBy(_ % 2)
      it.next()
      it.next()
      it.next()
      it.hasNext shouldBe false
      val res = a.iterator.groupBy(_ % 2).toList
      res shouldBe List(List(1, 1), List(2, 2), List(5))
    }

    "group by f with last group" in {
      val a = List(1, 1, 2, 4, 6, 5, 7, 8, 9, 9)
      val res = a.iterator.groupBy(_ % 2).toList
      res shouldBe List(List(1, 1), List(2, 4, 6), List(5, 7), List(8), List(9, 9))
    }

    "has next should be idempotent " in {
      val a = List(1, 2, 3, 4, 4, 3, 3, 10)

      val it = a.iterator.groupBy(_ % 2)
      1.to(1000).foreach { _ =>
        it.hasNext shouldBe true
      }
      val res = it.toList
      res shouldBe List(List(1), List(2), List(3), List(4, 4), List(3, 3), List(10))
      val exc = Try {
        it.next()
      }
      exc.failed.get.isInstanceOf[NoSuchElementException] shouldBe true
    }

    "empty next should throw exception" in {
      val it = Iterator.empty

      it.hasNext shouldBe false
      val res = Try {
        it.next()
      }
      res.failed.get.isInstanceOf[NoSuchElementException] shouldBe true
    }
  }
}
