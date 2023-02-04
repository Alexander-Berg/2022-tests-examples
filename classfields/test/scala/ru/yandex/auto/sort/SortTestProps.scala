package ru.yandex.auto.sort

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalacheck.Gen.frequency
import org.scalacheck.Prop.forAll
import org.scalatest.{FlatSpecLike, Matchers}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.Checkers.{check, _}
import java.{util => ju}
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class SortTestProps extends FlatSpecLike with Matchers {
  private val PARALLEL_SORT = new Sort
  private val comparator = new TestSortableAdComparator

  val testCase = for {
    list <- Gen.listOf(Gen.chooseNum(0d, 1d).map(TestSortableAd))
    skip <- frequency((1, Gen.choose(0, list.size + 1)), (5, Gen.choose(0, list.size / 2)))
    limit <- frequency((1, Gen.choose(0, list.size + 1)), (5, Gen.choose(5, 5 + list.size)))
  } yield (list.asJava, skip, limit)

  it should "lowestK and lowestKImproved should be equal" in {
    print(testCase.sample)

    check {
      forAll(testCase) {
        case (list, skip, limit) =>
          PARALLEL_SORT.lowestK(list, skip, limit, comparator) ==
            PARALLEL_SORT.lowestKImproved(list, skip, limit, comparator)
      }
    }
  }

}

case class TestSortableAd(catboostRelevance: Double)

class TestSortableAdComparator extends ju.Comparator[TestSortableAd] {
  override def compare(o1: TestSortableAd, o2: TestSortableAd): Int = {
    java.lang.Double.compare(o1.catboostRelevance, o2.catboostRelevance)
  }
}
