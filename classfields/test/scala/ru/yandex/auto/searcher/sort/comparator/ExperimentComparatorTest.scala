package ru.yandex.auto.searcher.sort.comparator

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.model.ShortCarAd
import ru.yandex.auto.searcher.utils.CommonFixtures
import ru.yandex.auto.sort.AdsFixture
import org.scalacheck.Prop.forAll
import org.scalatestplus.scalacheck.Checkers.check
import ru.yandex.auto.core.SortableAd
import ru.yandex.auto.searcher.core.Experiments
import ru.yandex.auto.searcher.sort.Order

/**
  * Тесты для экспериментов:
  * - Пессимизация
  * - Звонки
  */
@RunWith(classOf[JUnitRunner])
class ExperimentComparatorTest extends WordSpecLike with Matchers with AdsFixture with CommonFixtures {

  val total = 200
  val bucket = 200

  def genShortCarAd(beaten: Seq[Boolean]): Gen[ShortCarAd] =
    for {
      carAd <- genShortCarAd(Some(false))
      isBeaten <- Gen.oneOf(beaten)
      isCustomCleared <- Gen.oneOf(true, false)
    } yield {
      carAd.setBeaten(isBeaten)
      carAd.setCustomCleared(isCustomCleared)
      carAd
    }

  private def provideTestCase(
      adsGen: Gen[ShortCarAd],
      abt: Set[String] = Set(),
      experiments: Set[String] = Set()
  ): Gen[(UsedOffersRelevanceComparator, UsedOffersRelevanceComparator, List[ShortCarAd])] =
    for {
      list <- Gen.listOfN(bucket, adsGen) :| "list"
      bools <- Gen.infiniteStream(Gen.oneOf(true, false)) :| "bool flags"
      ints <- Gen.infiniteStream(Gen.choose(0, 1000000)) :| "ints"
    } yield {
      val comp =
        new UsedOffersRelevanceComparator(
          Order.DESC,
          new DumbSearchConfiguration(bools, ints, abtexp = abt, experiments = experiments)
        )
      val compWithoutExp =
        new UsedOffersRelevanceComparator(Order.DESC, new DumbSearchConfiguration(bools, ints))
      (comp, compWithoutExp, list)
    }

  "not beaten and customCleared should be at the beginning" in {
    check {
      forAll(provideTestCase(genShortCarAd(Seq(false)), Set(Experiments.ABT_VS_678_PESSIMIZATION_BEATEN))) {
        case (comp, compWithoutExp, offers) =>
          val size = offers.size / 2

          val testOrdering = new Ordering[SortableAd] {
            override def compare(x: SortableAd, y: SortableAd): Int = comp.compare(x, y)
          }

          val testCase = offers
            .sorted(testOrdering)
            .take(size)
            .count(s => !s.isBeaten && s.isCustomCleared)

          val basicOrdering = new Ordering[SortableAd] {
            override def compare(x: SortableAd, y: SortableAd): Int = compWithoutExp.compare(x, y)
          }

          val basicCase = offers
            .sorted(basicOrdering)
            .take(size)
            .count(s => !s.isBeaten && s.isCustomCleared)

          testCase > basicCase
      }
    }
  }

  "beaten and not customCleared should be at the end" in {
    check {
      forAll(provideTestCase(genShortCarAd(Seq(false, true)), Set(Experiments.ABT_VS_678_PESSIMIZATION_BEATEN))) {
        case (comp, compWithoutExp, offers) =>
          val size = offers.size / 2

          val testOrdering = new Ordering[SortableAd] {
            override def compare(x: SortableAd, y: SortableAd): Int = comp.compare(x, y)
          }

          val testCase = offers
            .sorted(testOrdering)
            .drop(size)

          val basicOrdering = new Ordering[SortableAd] {
            override def compare(x: SortableAd, y: SortableAd): Int = compWithoutExp.compare(x, y)
          }

          val basicCase = offers
            .sorted(basicOrdering)
            .drop(size)
            .count(s => s.isBeaten && !s.isCustomCleared)

          testCase.count(s => s.isBeaten && !s.isCustomCleared) > basicCase &&
          testCase.takeRight(5).forall(s => s.isBeaten && !s.isCustomCleared)
      }
    }
  }

  "test experiment mixin phoneCalls" in {
    check {
      forAll(
        provideTestCase(genShortCarAd(Seq(false, true)), Set(Experiments.SEARCHER_VS_1277_MIXIN_PHONE_CALLS))
      ) {
        case (comp, compWithoutExp, offers) =>
          val size = offers.size / 2

          val testOrdering = new Ordering[SortableAd] {
            override def compare(x: SortableAd, y: SortableAd): Int = comp.compare(x, y)
          }

          val testCase = offers
            .sorted(testOrdering)
            .take(size) // в начале должны скопиться оферы со звонками
            .map(_.getLastWeekPhoneCalls)
            .sum

          val basicOrdering = new Ordering[SortableAd] {
            override def compare(x: SortableAd, y: SortableAd): Int = compWithoutExp.compare(x, y)
          }

          val basicCase = offers
            .sorted(basicOrdering)
            .take(size)
            .map(_.getLastWeekPhoneCalls)
            .sum

          testCase > basicCase
      }
    }
  }

}
