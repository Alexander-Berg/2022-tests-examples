package ru.yandex.auto.searcher.search.grouphandler

import com.yandex.yoctodb.immutable.Database
import com.yandex.yoctodb.query.DocumentProcessor
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalacheck.Prop.{forAll, AnyOperators}
import org.scalatest.{Ignore, Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.Checkers.check
import ru.yandex.auto.core.SortableAd
import ru.yandex.auto.core.model.ShortCarAd
import ru.yandex.auto.core.search2.{YoctoIndexSearcherHolder, YoctoSearchTemplate}
import ru.yandex.auto.searcher.configuration.SearchConfiguration
import ru.yandex.auto.searcher.sort.comparator.NewOffersRelevanceComparator
import ru.yandex.auto.searcher.utils.CommonFixtures
import ru.yandex.auto.sort.AdsFixture

import java.time.Instant
import java.util
import java.util.Comparator
import scala.compat.java8.FunctionConverters.asJavaBiFunction
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
@Ignore // see https://st.yandex-team.ru/VS-1298
class AuctionTopShortCarAdHandlerSpec extends WordSpecLike with Matchers with AdsFixture with CommonFixtures {
  val bucket = 30
  val auctionSort: SearchConfiguration => Comparator[SortableAd] = sc => new NewOffersRelevanceComparator(sc)

  val priceSort: SearchConfiguration => Comparator[SortableAd] = sc =>
    NewOffersRelevanceComparator.cheapestComparator(sc)

  def auctionGenShortCarAd(): Gen[ShortCarAd] =
    for {
      carAd <- genShortCarAd(Some(true))
      dealerId <- Gen.oneOf(1, 2, 3, 4, 5, 6)
    } yield {
      carAd.setDealerId(dealerId)
      carAd
    }

  def auctionNotFreshGenShortCarAd(): Gen[ShortCarAd] =
    for {
      carAd <- auctionGenShortCarAd()
      freshDate <- Gen.frequency(
        (1, Instant.now().toEpochMilli - 13.hours.toMillis)
      )
    } yield {
      carAd.setFreshDateMillis(freshDate)
      carAd
    }

  private def provideTestCase(
      value1: Gen[ShortCarAd]
  ): Gen[(YoctoSearchTemplate[ShortCarAd], DumbSearchConfiguration)] =
    for {
      list <- Gen.listOfN(bucket, value1) :| "list"
      bools <- Gen.infiniteStream(Gen.oneOf(true, false)) :| "bool flags"
      ints <- Gen.infiniteStream(Gen.choose(0, 1000000)) :| "ints"
      sc = new DumbSearchConfiguration(bools, ints, withGrouping = Some(false), withAuctionSorting = Some(true))
      template = TestAdHandlerSpec.template(list)
    } yield (template, sc)

  "should handle it similarly if fresh" in {
    check {
      forAll(provideTestCase(auctionGenShortCarAd())) {
        case (template, sc) =>
          val handler = new AuctionTopShortCarAdHandler(sc)
          val test = new AuctionTopShortCarAdHandlerTestCase(null, template, auctionSort(sc))
          process(handler).sortedIterator.toList ?= process(test).sortedIterator.toList
      }
    }
  }

  "should sort by AuctionPosition same way if NOT fresh" in {
    check {
      forAll(provideTestCase(auctionNotFreshGenShortCarAd())) {
        case (template, sc) =>
          val handler = new AuctionTopShortCarAdHandler(sc)
          val list = process(handler).sortedIterator.map(_.getAuctionCallsRank).toList
          list ?= list.sorted.reverse
      }
    }
  }

  "should sort by Cheapest if NOT fresh" in {
    check {
      forAll(provideTestCase(auctionNotFreshGenShortCarAd())) {
        case (template, sc) =>
          val handler = new AuctionTopShortCarAdHandler(sc)
          val test = new AuctionTopShortCarAdHandlerCheapestTestCase(null, template, sc)
          process(handler).sortedIterator.toList ?= process(test).sortedIterator.toList
      }
    }
  }

  private def process[T <: DocumentProcessor](d: T): T = {
    (0 until bucket).foreach(i => {
      d.process(i, null)
    })
    d
  }

}

class AuctionTopShortCarAdHandlerTestCase(
    holder: YoctoIndexSearcherHolder,
    shortCarAdSearchTemplate: YoctoSearchTemplate[ShortCarAd],
    comparator: Comparator[SortableAd]
) extends DocumentProcessor {
  private val grouped = new Long2ObjectOpenHashMap[ShortCarAd]()
  private val ord: Ordering[ShortCarAd] = Ordering.comparatorToOrdering(comparator).on[ShortCarAd](x => x)

  override def process(i: Int, database: Database): Boolean = {
    val ad = shortCarAdSearchTemplate.getEntity(holder, database, i)
    grouped.merge(ad.getDealerId, ad, asJavaBiFunction[ShortCarAd, ShortCarAd, ShortCarAd]((l, r) => ord.min(l, r)))

    true
  }

  def sortedIterator: Iterator[ShortCarAd] = {
    val array = grouped.values().toArray(new Array[ShortCarAd](0))
    util.Arrays.sort(array, ord)
    array.toIterator
  }
}

class AuctionTopShortCarAdHandlerCheapestTestCase(
    holder: YoctoIndexSearcherHolder,
    shortCarAdSearchTemplate: YoctoSearchTemplate[ShortCarAd],
    sc: SearchConfiguration
) extends DocumentProcessor {
  private val grouped = new Long2ObjectOpenHashMap[ShortCarAd]()

  private val freshestOrd: Ordering[ShortCarAd] =
    Ordering.comparatorToOrdering(new NewOffersRelevanceComparator(sc)).on[ShortCarAd](identity)

  private val cheapestOrd: Ordering[ShortCarAd] = Ordering
    .comparatorToOrdering(NewOffersRelevanceComparator.cheapestComparator(sc))
    .on[ShortCarAd](identity)

  override def process(i: Int, database: Database): Boolean = {
    val ad = shortCarAdSearchTemplate.getEntity(holder, database, i)
    grouped.merge(
      ad.getDealerId,
      ad,
      asJavaBiFunction[ShortCarAd, ShortCarAd, ShortCarAd]((l, r) => cheapestOrd.min(l, r))
    )
    true
  }

  def sortedIterator: Iterator[ShortCarAd] = {
    val array = grouped.values().toArray(new Array[ShortCarAd](0))
    util.Arrays.sort(array, freshestOrd)
    array.toIterator
  }
}
