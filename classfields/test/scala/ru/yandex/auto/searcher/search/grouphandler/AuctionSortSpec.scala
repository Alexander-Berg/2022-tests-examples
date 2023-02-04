package ru.yandex.auto.searcher.search.grouphandler

import org.junit.runner.RunWith
import org.scalacheck.Prop.{forAll, AnyOperators}
import org.scalacheck.{Gen, Prop}
import org.scalatest.{Ignore, Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.Checkers.{check, _}
import ru.yandex.auto.core.SortableAd
import ru.yandex.auto.core.model.ShortCarAd
import ru.yandex.auto.searcher.ProportionsProvider.Proportion
import ru.yandex.auto.searcher.catboost.{NewRealtimeCatboostModel, UsedRealtimeCatboostModel}
import ru.yandex.auto.searcher.configuration.SearchConfiguration
import ru.yandex.auto.searcher.sort.extractor.PreComparator
import ru.yandex.auto.searcher.sort.extractor.sortableAd.{FirstPassInNew, FirstPassInUsed, FreshRelevance1Ext, IdExt}
import ru.yandex.auto.searcher.utils.CommonFixtures
import ru.yandex.auto.sort.AdsFixture
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
@Ignore
class AuctionSortSpec extends WordSpecLike with Matchers with AdsFixture with CommonFixtures with MockitoSupport {

  private val groupComparator = PreComparator(List(FreshRelevance1Ext.desc, IdExt.desc))
  private val usedCatboostService =
    new SimpleRealtimeCatboostServiceFeatures(10, UsedRealtimeCatboostModel(featureManager))
  private val newCatboostService =
    new SimpleRealtimeCatboostServiceFeatures(10, NewRealtimeCatboostModel(featureManager))
  private def provideMixed(sc: SearchConfiguration) =
    new MixedSearchShortCarAdHandler(
      sc,
      groupComparator,
      groupComparator,
      sc.createGroupingTypes,
      usedCatboostService,
      newCatboostService,
      false,
      true
    )

  private def provideTestCase =
    for {
      bucketSize <- Gen.choose(0, 1000)
      list <- Gen.listOfN(bucketSize, genShortCarAd(Some(true)))
      skip <- Gen.choose(0, list.size + 1)
      limit <- Gen.choose(0, list.size + 1)
      bools <- Gen.infiniteStream(Gen.oneOf(true, false))
      ints <- Gen.infiniteStream(Gen.choose(0, 1000000))
      sc = new DumbSearchConfiguration(bools, ints, withGrouping = Some(false), withAuctionSorting = Some(false))
      mixed = provideMixed(sc)
    } yield (list, skip, limit, sc, mixed)

  "should handle it similarly" in {
    check {
      forAll(provideTestCase) {
        case (list, skip, limit, sc, mixed) =>
          handle(list, skip, limit, sc, mixed)
      }
    }
  }

  private def handle(
      list: List[ShortCarAd],
      skip: Int,
      limit: Int,
      sc: SearchConfiguration,
      mixed: GroupingHandle
  ): Prop = {
    val proportion = Proportion.legacy2bucket(sc)
    val mixedComparator = new FirstPassInUsed(sc)
    val auctionSorting = new FirstPassInNew(sc)

    list.foreach(ad => {
      mixed.handle(ad)
    })

    list
      .sortWith(auctionSorting.toComparator.compare(_, _) < 0)
      .slice(skip, skip + limit)
      .map(_.asInstanceOf[SortableAd]) ?=
      mixed.getPage(skip, limit, mixedComparator, auctionSorting, proportion).asScala.toList
  }

}
