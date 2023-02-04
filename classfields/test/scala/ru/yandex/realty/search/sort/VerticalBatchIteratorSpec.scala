package ru.yandex.realty.search.sort

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import ru.yandex.realty.search.sort.ShuffleSort.VerticalBatchIterator

import scala.collection.mutable.ArrayBuffer

@RunWith(classOf[JUnitRunner])
class VerticalBatchIteratorSpec extends WordSpec with MockFactory with Matchers with OneInstancePerTest {

  "VerticalBatchIterator" should {
    "correctly return result for no iterators" in {
      baseSpec() shouldBe empty
    }

    "correctly return result for single empty iterator" in {
      baseSpec(Iterator.empty) shouldBe empty
    }

    "correctly return for single non-empty iterator" in {
      baseSpec(Iterator(1, 2, 3)) shouldBe Seq(Seq(1), Seq(2), Seq(3))
    }

    /**
      * 1 2 3
      * 4 5 6
      *
      * [1 4] [2 5] [3 6]
      */
    "correctly return for 2 non-empty iterators with same sizes" in {
      baseSpec(Iterator(1, 2, 3), Iterator(4, 5, 6)) shouldBe Seq(Seq(1, 4), Seq(2, 5), Seq(3, 6))
    }

    /**
      * 1 2 3 4
      * 4 5
      *
      * [1 4] [2 5] [3] [4]
      */
    "correctly return for 2 non-empty iterators with different sizes" in {
      baseSpec(Iterator(1, 2, 3, 4), Iterator(4, 5)) shouldBe Seq(Seq(1, 4), Seq(2, 5), Seq(3), Seq(4))
    }

    "correctly work with special case" in {
      baseSpec(
        Iterator(1, 2, 3, 4),
        Iterator(5, 6),
        Iterator(7, 8, 9, 10, 11)
      ) shouldBe Seq(
        Seq(1, 5, 7),
        Seq(2, 6, 8),
        Seq(3, 9),
        Seq(4, 10),
        Seq(11)
      )
    }
  }

  private def toSeqForce[A](it: Seq[A]): Seq[A] = {
    val b = ArrayBuffer[A]()

    it.foreach(b += _)
    b.result()
  }

  private def baseSpec[A](iterators: Iterator[A]*): Seq[Seq[A]] =
    toSeqForce(new VerticalBatchIterator(iterators: _*).toSeq)
}
