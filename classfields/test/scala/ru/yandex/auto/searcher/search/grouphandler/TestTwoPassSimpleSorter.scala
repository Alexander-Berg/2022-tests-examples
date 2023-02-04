package ru.yandex.auto.searcher.search.grouphandler

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.SortableAd
import ru.yandex.auto.core.model.ShortCarAd
import ru.yandex.auto.extdata.AutoDataTypes
import ru.yandex.auto.searcher.catboost.{NewRealtimeCatboostModel, UsedRealtimeCatboostModel}
import ru.yandex.auto.searcher.configuration.SearchConfiguration
import ru.yandex.auto.searcher.core.Experiments
import ru.yandex.auto.searcher.sort.comparator.CommonComparators
import ru.yandex.auto.searcher.sort.extractor.PreComparator
import ru.yandex.auto.searcher.sort.extractor.sortableAd.{CbrExt, CbrNewExt}
import ru.yandex.auto.searcher.utils.CommonFixtures

import java.util
import scala.collection.mutable.ArrayBuffer

@RunWith(classOf[JUnitRunner])
class TestTwoPassSimpleSorter extends WordSpec with Matchers with CommonFixtures with CommonComparators {
  private val sc = new SearchConfiguration()
  private val offlineComparator = PreComparator[SortableAd] {
    (if (sc.hasExperiment(Experiments.SEARCHER_VS_1150_NEW_OFFLINE_MODEL))
       CbrNewExt
     else
       CbrExt).desc
  }
  private val online = PreComparator[SortableAd](CbrNewExt.desc)

  val sorter =
    new TwoPassSorter(
      sc,
      new FixedRealtimeCatboostService(UsedRealtimeCatboostModel(featureManager))
    )

  def createAd(offlineRelevance: Double, onlineRelevance: Double = 1, state: Boolean = true): SortableAd = {
    val ad = new ShortCarAd()
    ad.setIsStateNew(state)
    ad.setCatboostRelevanceBoosted(offlineRelevance)
    ad.setCatboostRelevance(onlineRelevance)
    ad.asInstanceOf[SortableAd]
  }

  "should sort by offline relevance first then sort top 2 again by online relevance" in {
    val javaNewList: util.List[SortableAd] = {
      val l = new util.ArrayList[SortableAd]
      l.add(createAd(2, 1))
      l.add(createAd(1, 3))
      l.add(createAd(2, 3))
      l.add(createAd(1, 1))
      l.add(createAd(3, 7))
      l.add(createAd(5, 5))
      l
    }
    val javaOldList: util.List[SortableAd] = {
      val l = new util.ArrayList[SortableAd]
      l.add(createAd(6, 1))
      l.add(createAd(2, 1))
      l.add(createAd(3, 1))
      l.add(createAd(2, 1))
      l.add(createAd(4, 9))
      l.add(createAd(1, 19))
      l
    }

    val unsorted = Array(
      javaNewList,
      javaOldList
    )

    val sorted =
      sorter.sort(
        0,
        5,
        offlineComparator,
        online,
        unsorted,
        isLogSorting = sc.isLogSorting
      )

    val seq = sorted.toSeq.map(
      _.toList.map(ad => (ad.getCatboostRelevanceBoosted, ad.getCatboostRelevanceBoostedRealTime))
    )

    assert(
      seq == ArrayBuffer(
        List((3.0, 7.0), (5.0, 5.0), (2.0, 0.0), (2.0, 0.0), (1.0, 0.0)),
        List((4.0, 9.0), (6.0, 1.0), (3.0, 0.0), (2.0, 0.0), (2.0, 0.0))
      )
    )
  }

}
