package ru.yandex.auto.searcher.search.grouphandler

import org.apache.commons.lang3.ArrayUtils
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.SortableAd
import ru.yandex.auto.extdata.AutoDataTypes
import ru.yandex.auto.searcher.catboost.{NewRealtimeCatboostModel, UsedRealtimeCatboostModel}
import ru.yandex.auto.searcher.configuration.SearchConfiguration
import ru.yandex.auto.searcher.search.experiments.In909Experiment
import ru.yandex.auto.searcher.sort.extractor.PreComparator
import ru.yandex.auto.searcher.sort.extractor.sortableAd.{CbrExt, FreshRelevance1Ext}
import ru.yandex.auto.searcher.utils.CommonFixtures
import ru.yandex.auto.sort.AdsFixture

import java.util
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class TestTwoPassSpec extends WordSpec with Matchers with AdsFixture with CommonFixtures {

  private val bucketSize = 1000
  private val queryLimit = 40
  private val onlineHeadSize = 100
  private val sc = new SearchConfiguration()

  val sortInNewComparator = PreComparator[SortableAd](FreshRelevance1Ext)
  val onlineComparator = PreComparator[SortableAd](CbrExt.desc)

  val twoPassSorterFeaturesModel =
    new TwoPassSorter(
      sc: SearchConfiguration,
      new SimpleRealtimeCatboostServiceFeatures(onlineHeadSize, UsedRealtimeCatboostModel(featureManager))
    )

  def genList[T](g: Gen[T]): util.List[T] = {
    Gen.listOfN(bucketSize, g).map(_.toBuffer.asJava).sample.get
  }

  private val sortableAds: Gen[SortableAd] = genShortCarAd().map(_.asInstanceOf[SortableAd])
  val privatesUsed = genList(sortableAds)
  val resellersUsed = genList(sortableAds)
  val dealersUsed = genList(sortableAds)

  "should sort by offline relevance first then sort top 2 again by online relevance" in {
    val result: Array[List[SortableAd]] = twoPassSorterFeaturesModel
      .sort(
        0,
        queryLimit,
        sortInNewComparator,
        onlineComparator,
        Array(privatesUsed, resellersUsed, dealersUsed),
        isLogSorting = false
      )
      .map(_.toList)

    println(
      result.head
        .take(10)
        .map(_.getCatboostRelevanceRealTime)
        .mkString("getCatboostRelevanceRealTime(", ", ", ")")
    )

    println(
      result.head
        .take(3)
        .map(_.getCatboostCategorical.map.asScala.mkString(","))
        .mkString("top3 getCatboostCategoryFeatures(", "\n ", ")")
    )

    println(
      result.head
        .take(3)
        .map(_.getCatboostNumerical.map.asScala.mkString(","))
        .mkString("top3 getCatboostNumericalFeatures(", "\n ", ")")
    )

    assert(result.forall(_.size == queryLimit))

    assert(result.forall(list => {
      val array = list.take(onlineHeadSize).toArray
      ArrayUtils.isSorted(array, onlineComparator.toComparator)
    }))

    assert(result.forall(list => {
      val array = list.drop(onlineHeadSize).toArray
      ArrayUtils.isSorted(array, sortInNewComparator.toComparator)
    }))

    assert(result.forall(list => {
      val array = list.take(onlineHeadSize)
      array.map(_.getCatboostRelevanceRealTime).forall(_ > 0)
    }))

    assert(result.forall(list => {
      val array = list.take(onlineHeadSize)
      array.map(_.getCatboostRelevanceBoostedRealTime).forall(_ > 0)
    }))

    assert(result.forall(list => {
      val array = list.take(onlineHeadSize)
      array.map(_.getCatboostRelevanceBoostedRealTime).toSet.size > 1
    }))

  }

}
