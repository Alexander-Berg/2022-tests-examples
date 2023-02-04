package ru.yandex.auto.searcher.search.grouphandler

import org.junit.Ignore
import org.scalacheck.Gen
import org.scalacheck.Prop._
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.scalacheck.Checkers.check
import ru.yandex.auto.core.SortableAd
import ru.yandex.auto.core.model.CatboostAdWrapper
import ru.yandex.auto.extdata.AutoDataTypes
import ru.yandex.auto.log.Logging
import ru.yandex.auto.searcher.catboost.{NewRealtimeCatboostModel, RealtimeCatboostService, UsedRealtimeCatboostModel}
import ru.yandex.auto.searcher.configuration.{RichSearchConfiguration, SearchConfiguration}
import ru.yandex.auto.searcher.sort.comparator.SortUtils
import ru.yandex.auto.searcher.sort.extractor.PreComparator
import ru.yandex.auto.searcher.sort.extractor.sortableAd.{CbrRealTimeExt, FreshRelevance1Ext}
import ru.yandex.auto.searcher.utils.CommonFixtures
import ru.yandex.auto.sort.AdsFixture
import ru.yandex.auto.util.DistanceCalculator

import java.util
import java.util.Comparator
import java.util.concurrent.ForkJoinPool
import scala.collection.JavaConverters._
import scala.collection.parallel.mutable.ParArray
import scala.collection.parallel.{ExecutionContextTaskSupport, TaskSupport}
import scala.concurrent.ExecutionContext

@Ignore
class TestTwoPassWithLegacySpec extends WordSpec with Matchers with AdsFixture with CommonFixtures {

  private val bucketMax = 1000
  private val onlineHeadSize = 500
  private val sc = new SearchConfiguration()

  private val sortInNewComparator = PreComparator[SortableAd](FreshRelevance1Ext)
  private val onlineComparator = PreComparator[SortableAd](CbrRealTimeExt.desc)

  private val sorter =
    new TwoPassSorter(
      sc: SearchConfiguration,
      new SimpleRealtimeCatboostServiceFeatures(onlineHeadSize, UsedRealtimeCatboostModel(featureManager))
    )

  private val legacySorter =
    new LegacySort.TwoPassSorter(
      sc: SearchConfiguration,
      new SimpleRealtimeCatboostServiceFeatures(onlineHeadSize, NewRealtimeCatboostModel(featureManager)),
      true
    )

  private val privatesUsed: Gen[(util.List[SortableAd], Int, Int)] = for {
    bucketSize <- Gen.choose(0, bucketMax) :| "bucketSize"
    sortableAds: Gen[SortableAd] = genShortCarAd().map(_.asInstanceOf[SortableAd])
    offers ← Gen.listOfN(bucketSize, sortableAds) :| "offers"
    skip <- Gen.choose(0, bucketMax + 1) :| "skip"
    limit <- Gen.choose(0, bucketMax + 1) :| "limit"
  } yield (offers.toBuffer.asJava, skip, limit)

  "compare two sorts" in {
    check {
      forAllNoShrink(privatesUsed) {
        case (offers, skip, limit) =>
          sorter
            .sort(
              skip,
              limit,
              sortInNewComparator,
              onlineComparator,
              Array(offers),
              isLogSorting = false
            )
            .map(_.toSeq)
            .head ?=
            legacySorter
              .sort(skip, limit, sortInNewComparator.toComparator, onlineComparator.toComparator, Array(offers), true)
              .map(_.toSeq)
              .head
      }
    }
  }

}

object LegacySort {
  private val factor = Runtime.getRuntime.availableProcessors
  private val pool = new ForkJoinPool(factor)
  val sortingExecutionContext = new ExecutionContextTaskSupport(ExecutionContext.fromExecutor(pool))

  implicit class ParallelizableHelper[T](val arr: Array[T]) extends AnyVal {

    def parWith(ts: TaskSupport): ParArray[T] = {
      val p = arr.par
      p.tasksupport = ts
      p
    }
  }

  class TwoPassSorter(
      sc: SearchConfiguration,
      realtimeCatboostService: RealtimeCatboostService,
      runDistanceSorting: Boolean
  ) extends Logging {

    implicit private val richSearchConfiguration = new RichSearchConfiguration(sc)

    private def updateOnlineCatboostRelevance(list: Array[CatboostAdWrapper]): Unit = {
      if (list.nonEmpty) {
        realtimeCatboostService.updateRealtimeBatchRelevancePredicted(list)
      }
    }

    def sort(
        querySkip: Int,
        queryLimit: Int,
        firstPassComparator: Comparator[SortableAd],
        secondPassComparator: Comparator[SortableAd],
        buckets: Array[util.List[SortableAd]],
        sortByRealtimeCatboost: Boolean
    ): Array[Iterator[SortableAd]] = {

      buckets
        .parWith(sortingExecutionContext)
        .map { bucket =>
          val visibleTop = querySkip + queryLimit
          val sortedSize = bucket.size() min (visibleTop) // dropped realtimeCatboostService.headSize here

          SortUtils.partialQuickSort[SortableAd](bucket, 0, bucket.size() - 1, firstPassComparator, 0, sortedSize - 1)

          // partialQuickSort гарантирует только то, что в нужном месте соберутся нужные элементы, теперь их надо досортировать
          val array = bucket.iterator.asScala.take(sortedSize).map(CatboostAdWrapper(_)).toArray

          if (sc.isInfiniteList && runDistanceSorting) {
            DistanceCalculator.modifyDistance(array.iterator, sc.getRegionList.asScala.headOption)
          }

          util.Arrays.sort(array.asInstanceOf[Array[SortableAd]], firstPassComparator)
          updateOnlineCatboostRelevance(array)

          if (sortByRealtimeCatboost) {
            util.Arrays.sort(array.asInstanceOf[Array[SortableAd]], secondPassComparator)
            array.foreach(_.addModelMeta(("sortByRealtimeCatboost")))
          }

          array.iterator.take(array.length min (visibleTop max realtimeCatboostService.headSize)) // added realtimeCatboostService.headSize here
        }
        .toArray
    }

  }

}
