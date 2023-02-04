package ru.yandex.auto.searcher.search.grouphandler

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalacheck.Prop.{forAll, AnyOperators}
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.Checkers.check
import ru.yandex.auto.core.SortableAd
import ru.yandex.auto.core.model.{CatboostAdWrapper, ShortCarAd}
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.core.region.Region
import ru.yandex.auto.core.search2.YoctoSearchTemplate
import ru.yandex.auto.searcher.configuration.{SearchConfiguration, Sort}
import ru.yandex.auto.searcher.core.Experiments
import ru.yandex.auto.searcher.sort.{Order, SortType}
import ru.yandex.auto.searcher.utils.CommonFixtures
import ru.yandex.auto.sort.AdsFixture
import utils.LayeredDealersCalculator

import java.time.Instant
import java.util
import java.util.Collections
import scala.collection.mutable
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class LayeredDealersShortSpec extends WordSpecLike with Matchers with AdsFixture with CommonFixtures {
  val bucket = 30

  def auctionGenShortCarAd(): Gen[ShortCarAd] =
    for {
      carAd <- genShortCarAd(Some(false))
      hasDealer <- Gen.oneOf(true, false)
      hasBoost <- Gen.oneOf(true, false)
      userId <- Gen.choose(1L, Long.MaxValue)
      freshInterval <- Gen.oneOf(0, 2, 5, 35).map(_.days.toMillis)
    } yield {
      if (hasDealer) carAd.setDealerIds(Array(userId)) else carAd.setDealerIds(Array())

      val curTime = Instant.now().toEpochMilli
      carAd.setFreshDateMillis(curTime - freshInterval)
      carAd.setUpdateDateMillis(curTime - freshInterval)
      carAd.setCreationDateMillis(curTime - freshInterval - (if (hasBoost) 100000 else 0))

      carAd.setDealerBoostFactor(LayeredDealersCalculator.calculate(carAd))
      carAd
    }

  private def provideTestCase(
      adsGen: Gen[ShortCarAd]
  ): Gen[(YoctoSearchTemplate[ShortCarAd], DumbSearchConfiguration, Seq[ShortCarAd], Boolean)] =
    for {
      list <- Gen.listOfN(bucket, adsGen) :| "list"
      bools <- Gen.infiniteStream(Gen.oneOf(true, false)) :| "bool flags"
      ints <- Gen.infiniteStream(Gen.choose(0, 1000000)) :| "ints"
    } yield {
      val sc =
        new DumbSearchConfiguration(
          bools,
          ints,
          withBoostDealers = Some(true),
          withGrouping = Some(false),
          states = Seq(State.Search.USED)
        )
      val template = TestAdHandlerSpec.template(list)
      (template, sc, list, bools.iterator.next())
    }

  "it looks like sorting by boost factor only" in {
    check {
      forAll(provideTestCase(auctionGenShortCarAd())) {
        case (template, sc, offers, withRealtimeSort) =>
          val handler = buildHandler(sc, withRealtimeSort)
          template.search(null, handler, null)
          val res = getPage(handler, sc, bucket)
          val testRes = offers.sortBy(LayeredDealersCalculator.calculate).toList

          val resFactors = res.map(_.getDealerBoostFactor())
          val testFactors = testRes.map(_.getDealerBoostFactor)

          resFactors ?= testFactors
      }
    }
  }

  "the experiment matters" in {
    check {
      forAll(provideTestCase(auctionGenShortCarAd())) {
        case (template, _, _, withRealtimeSort) =>
          val sc = new LayeredDealersSearchConfiguration()

          val withoutExp = {
            val handler = buildHandler(sc, withRealtimeSort)
            template.search(null, handler, null)
            getPage(handler, sc, bucket)
          }

          val withExp = {
            sc.experiments.add(Experiments.SEARCHER_VS_1084_LAYERED_DEALERS)
            val handler = buildHandler(sc, withRealtimeSort)
            template.search(null, handler, null)
            getPage(handler, sc, bucket)
          }

          withoutExp != withExp
      }
    }
  }

}

class LayeredDealersSearchConfiguration() extends SearchConfiguration {

  val experiments = mutable.Set[String]()

  override val getSortOffers: Sort = new Sort(SortType.FRESH_RELEVANCE_1, Order.DESC)

  override def getRegionList: util.List[Region] = Collections.emptyList()

  override def hasExperiment(experimentId: String): Boolean = experiments.contains(experimentId)

  override def hasOnlyUsedState: Boolean = true
}
