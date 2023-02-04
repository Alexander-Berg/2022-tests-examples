package auto.carfax.common.utils.collections

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.Try

class GroupedIteratorTest extends AnyWordSpecLike with Matchers {

  "grouped iterator" should {
    "group by f" in {
      val a = List(1, 2, 3, 4, 5)

      val it = a.iterator.groupedBy(_.sum >= 4)
      val res = a.iterator.groupedBy(_.sum >= 4).toList
      it.hasNext
      it.next()
      it.hasNext
      it.next()
      it.hasNext
      it.next()
      it.hasNext shouldBe false
      res shouldBe List(List(1, 2, 3), List(4), List(5))
    }

    "group by f and return tail" in {
      val a = List(1, 2, 3, 1)

      val it = a.iterator.groupedBy(_.sum >= 4)
      val res = a.iterator.groupedBy(_.sum >= 4).toList
      it.hasNext
      it.next()
      it.hasNext
      it.next()
      it.hasNext shouldBe false
      res shouldBe List(List(1, 2, 3), List(1))
    }

    "has next should be idempotent " in {
      val a = List(1, 2, 3, 4, 4, 3, 3, 10)

      val it = a.iterator.groupedBy(_.sum >= 4)
      1.to(1000).foreach { _ =>
        it.hasNext shouldBe true
      }
      val res = it.toList
      res shouldBe List(List(1, 2, 3), List(4), List(4), List(3, 3), List(10))
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
