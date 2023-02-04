package ru.yandex.auto.searcher.search.grouphandler

import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.auto.core.SortableAd
import ru.yandex.auto.searcher.sort.comparator.RandomSortableAdComparator
import ru.yandex.auto.sort.AdsFixture

import scala.collection.JavaConverters._
import java.util

class RandomSortableAdComparatorSpec extends WordSpec with Matchers with AdsFixture {
  private val bucketSize = 5
  val asc = ru.yandex.auto.searcher.sort.Order.ASC

  def genList[T](g: Gen[T]): util.List[T] = {
    Gen.listOfN(bucketSize, g).map(_.toBuffer.asJava).sample.get
  }

  private val sortableAds: Gen[SortableAd] = genShortCarAd().map(_.asInstanceOf[SortableAd])
  val list: util.List[SortableAd] = genList(sortableAds)

  "test me" in {
    val rand = new RandomSortableAdComparator(asc, 100)
    println(list)
    list.sort(rand)
    println(list)
  }

}
