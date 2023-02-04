package ru.yandex.auto.searcher.search.grouphandler

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.WordSpecLike
import ru.yandex.auto.searcher.ProportionsProvider.Proportion
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.searcher.configuration.SearchConfiguration
import ru.yandex.auto.searcher.sort.comparator.{CommonComparators, UsedOffersRelevanceComparator}
import ru.yandex.auto.searcher.utils.CommonFixtures
import ru.yandex.auto.searcher.sort.SortType

import scala.jdk.CollectionConverters.asScalaBufferConverter
import ru.yandex.auto.searcher.sort.Order
import ru.yandex.auto.sort.AdsFixture
import org.scalatestplus.scalacheck.Checkers.{check, _}
import ru.yandex.auto.core.region.Region
import org.scalacheck.Prop.forAll
import ru.yandex.auto.extdata.AutoDataTypes
import ru.yandex.auto.searcher.catboost.{NewRealtimeCatboostModel, UsedRealtimeCatboostModel}
import ru.yandex.auto.searcher.sort.extractor.sortableAd.FirstPassInUsed

import java.util.Collections

@RunWith(classOf[JUnitRunner])
class DecayDealer extends CommonFixtures with CommonComparators with WordSpecLike with AdsFixture {

  private val limit = 10
  private val now = System.currentTimeMillis()

  private def generateSearchConfiguration(dealerBoostCoef: Option[Float]): SearchConfiguration = {
    val sc = new SearchConfiguration() {
      override def getRegionList: java.util.List[Region] = Collections.emptyList()
    }
    sc.setSortOffers(new ru.yandex.auto.searcher.configuration.Sort(SortType.FRESH_RELEVANCE_1, Order.DESC))
    dealerBoostCoef.foreach(sc.setDealerBoostCoefficient)
    sc
  }

  private def generateInstruments(dealerBoostCoef: Option[Float]) =
    for {
      sc <- Gen.const(generateSearchConfiguration(dealerBoostCoef))
      groupingTypes = new java.util.HashSet[GroupingType]()
      newCatboostService = new FixedRealtimeCatboostService(NewRealtimeCatboostModel(featureManager))
      usedCatboostService = new FixedRealtimeCatboostService(UsedRealtimeCatboostModel(featureManager))
      comparator <- Gen.const(new FirstPassInUsed(sc))
      handler <- Gen.const(
        new MixedSearchShortCarAdHandler(
          sc,
          comparator,
          comparator,
          groupingTypes,
          usedCatboostService,
          newCatboostService,
          false,
          false
        )
      )
    } yield (comparator, handler)

  "should be only privates" in {
    check {
      forAll(
        Gen.listOfN(limit, generateAlmostEqualsPairsShortAdds(now)),
        generateInstruments(Some(1000.0f)) // strong decay dealers
      ) {
        case (list, (comparator, handler)) =>
          list.flatten.foreach(handler.handle)
          val result = handler
            .getPage(
              0,
              limit,
              comparator,
              comparator,
              Proportion.default
            )
            .asScala
            .toList

          result.filterNot(_.hasDealer).size == 10
      }
    }
  }

  "should be only dealers" in {
    check {
      forAll(
        Gen.listOfN(limit, generateAlmostEqualsPairsShortAdds(now)),
        generateInstruments(Some(1.0f)) // weak decay dealers
      ) {
        case (list, (comparator, handler)) =>
          list.flatten.foreach(handler.handle)
          val result = handler
            .getPage(
              0,
              limit,
              comparator,
              comparator,
              Proportion.default
            )
            .asScala
            .toList

          result.count(_.hasDealer) == 10
      }
    }
  }
}
