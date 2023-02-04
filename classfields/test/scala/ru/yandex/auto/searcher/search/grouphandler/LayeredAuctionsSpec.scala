package ru.yandex.auto.searcher.search.grouphandler

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalacheck.Prop.{forAll, AnyOperators}
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.Checkers.check
import ru.yandex.auto.core.model.ShortCarAd
import ru.yandex.auto.core.region.{Region, RegionType}
import ru.yandex.auto.searcher.core.Experiments
import ru.yandex.auto.searcher.sort.comparator.{AuctionParticipantComparator, NewOffersRelevanceComparator}
import ru.yandex.auto.searcher.utils.CommonFixtures
import ru.yandex.auto.sort.AdsFixture

import java.util.Collections

@RunWith(classOf[JUnitRunner])
class LayeredAuctionsSpec extends WordSpecLike with Matchers with AdsFixture with CommonFixtures {
  private val auctionParticipantComparator = new AuctionParticipantComparator
  val bucket = 100

  private def provideTestCase(
      adsGen: Gen[ShortCarAd],
      exps: Set[String]
  ): Gen[(NewOffersRelevanceComparator, NewOffersRelevanceComparator, List[ShortCarAd])] =
    for {
      list <- Gen.listOfN(bucket, adsGen) :| "list"
      bools <- Gen.infiniteStream(Gen.oneOf(true, false)) :| "bool flags"
      ints <- Gen.infiniteStream(Gen.choose(0, 1000000)) :| "ints"
    } yield {
      val regionList = Collections.singletonList(new Region(213, "213", RegionType.CITY, null, 0.0d, 0.0d))

      val comp = new NewOffersRelevanceComparator(new DumbSearchConfiguration(bools, ints, experiments = exps) {
        override def getRegionList: java.util.List[Region] = regionList
      })
      val compWithoutExp = new NewOffersRelevanceComparator(new DumbSearchConfiguration(bools, ints) {
        override def getRegionList: java.util.List[Region] = regionList
      })
      (comp, compWithoutExp, list)
    }

//  "sort auction participants first" in {
//    check {
//      forAll(provideTestCase(caradsNew, Set(Experiments.SEARCHER_VS_1057_LAYERED_AUCTION))) {
//        case (comp, _, offers) =>
//          val result = offers.sortWith(comp.compare(_, _) < 0).map(_.getAuctionCallsRank > 0)
//          val testCase = offers.sortWith(auctionParticipantComparator.compare(_, _) > 0).map(_.getAuctionCallsRank > 0)
//
//          result ?= testCase
//      }
//    }
//  }

  "regular sort auction participants with NO experiment" in {
    check {
      forAll(provideTestCase(caradsNew, Set())) {
        case (comp, _, offers) =>
          val result = offers.sortWith(comp.compare(_, _) < 0).map(_.getAuctionCallsRank > 0)
          val testCase = offers.sortWith(auctionParticipantComparator.compare(_, _) > 0).map(_.getAuctionCallsRank > 0)

          result != testCase
      }
    }
  }

  "sort with experiments SEARCHER_VS_1191_AUCTION, home region and then auction" in {
    check {
      forAll(provideTestCase(caradsNew, Set(Experiments.SEARCHER_VS_1191_AUCTION))) {
        case (comp, _, offers) =>
          val result = offers
            .sortWith(comp.compare(_, _) < 0)
            .takeWhile(s => s.getRid == 213)

          val auctions: Seq[BigInt] = result.map(o => BigInt(o.getAuctionCallsRank))
          val result2 = if (auctions.size % 2 == 0) auctions else auctions.init

          result.size == offers.count(s => s.getRid == 213) && // check home
          result2.take(result2.size).sum > result2.drop(result2.size).sum // check auction
      }
    }
  }

  private def caradsNew: Gen[ShortCarAd] =
    for {
      ca <- genShortCarAd(isNew = Some(true))
      auctionPrice <- Gen.infiniteStream(Gen.oneOf[Long](0L, Gen.choose(1L, Long.MaxValue)))
      bool <- Gen.oneOf(true, false)
      regionCode <- Gen.oneOf(213, 214)
    } yield {
      val iterator = auctionPrice.iterator
      ca.setInStock(bool)
      ca.setAuctionBasePrice(iterator.next())
      ca.setAuctionBidPrice(iterator.next())
      ca.setAuctionCallsRank(ca.getAuctionBidPrice)
      ca.setRegionCode(regionCode)
      ca
    }

  // todo with distance calculator?

}
