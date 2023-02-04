package ru.yandex.auto.searcher.search.grouphandler

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.SortableAd
import ru.yandex.auto.core.model.ShortCarAd
import ru.yandex.auto.extdata.AutoDataTypes
import ru.yandex.auto.searcher.ProportionsProvider.Proportion
import ru.yandex.auto.searcher.catboost.{NewRealtimeCatboostModel, UsedRealtimeCatboostModel}
import ru.yandex.auto.searcher.configuration.SearchConfiguration
import ru.yandex.auto.searcher.core.Experiments
import ru.yandex.auto.searcher.sort.comparator.CommonComparators
import ru.yandex.auto.searcher.sort.extractor.PreComparator
import ru.yandex.auto.searcher.sort.extractor.sortableAd.{CbrExt, CbrNewExt, FreshRelevance1Ext}
import ru.yandex.auto.searcher.utils.CommonFixtures

import java.util
import scala.jdk.CollectionConverters.asScalaBufferConverter

@RunWith(classOf[JUnitRunner])
class TestGroupHandle extends WordSpec with Matchers with CommonFixtures with CommonComparators {
  private val sc = new SearchConfiguration()
  private val comparator = PreComparator[SortableAd] {
    if (sc.hasExperiment(Experiments.SEARCHER_VS_1150_NEW_OFFLINE_MODEL))
      CbrNewExt
    else
      CbrExt
  }
  private val inNewComparator = PreComparator[SortableAd](FreshRelevance1Ext)
  private val groupingTypes: util.Set[GroupingType] = new util.HashSet[GroupingType]()
  private val usedCatboostService = new FixedRealtimeCatboostService(UsedRealtimeCatboostModel(featureManager))
  private val newCatboostService = new FixedRealtimeCatboostService(NewRealtimeCatboostModel(featureManager))
  private val proportion = Proportion.default

  def handler =
    new MixedSearchShortCarAdHandler(
      sc,
      comparator,
      comparator,
      groupingTypes,
      usedCatboostService,
      newCatboostService,
      false,
      true
    )

  val newAd = {
    val ad = new ShortCarAd()
    ad.setIsStateNew(true)
    ad
  }

  val usedAd = {
    val ad = new ShortCarAd()
    ad.setIsStateNew(false)
    ad
  }

  "should work with USED alone" in {
    val h = handler
    val totalFill = 5
    val ads = List.fill(totalFill)(usedAd)
    ads.foreach(h.handle)

    val page = h.getPage(
      0,
      10,
      comparator,
      inNewComparator,
      proportion
    )
    assert(h.getTotalCount == totalFill)
    assert(page.size() == totalFill)
  }

  "should match with last page tail" in {
    val h = handler
    val ads = List.fill(500)(usedAd) ++ List.fill(244)(newAd)
    ads.foreach(h.handle)

    val page = h.getPage(
      7 * 100,
      100,
      comparator,
      inNewComparator,
      proportion
    )
    assert(page.size() == 44)
  }

  "should work with in a Major mix" in {
    val h = handler
    val ads = List.fill(5)(usedAd) ++ List.fill(10)(newAd)
    ads.foreach(h.handle)

    val page = h.getPage(
      0,
      10,
      comparator,
      inNewComparator,
      proportion
    )
    assert(h.getTotalCount == ads.size)
    assert(page.size() == 10)
    assert(page.asScala.count(_.isStateNew) == 7)
  }

  "should work with in a Minor mix" in {
    val h = handler
    val ads = List.fill(14)(usedAd) ++ List.fill(7)(newAd)
    ads.foreach(h.handle)

    val page = h.getPage(
      0,
      10,
      comparator,
      inNewComparator,
      proportion
    )
    assert(h.getTotalCount == ads.size)
    assert(page.asScala.count(_.isStateNew) == 4)
  }

}
