package ru.yandex.vertis.general.gost.model.test

import java.time.Instant

import ru.yandex.vertis.general.gost.model.OfferStatus.{Inactive, Removed}
import ru.yandex.vertis.general.gost.model.Price.InCurrency
import ru.yandex.vertis.general.gost.model.counters.{Counter, CountersAggregate}
import ru.yandex.vertis.general.gost.model.inactive.InactiveReason.SellerRecall
import ru.yandex.vertis.general.gost.model.inactive.recall.SellerRecallReason.SoldOnYandex
import ru.yandex.vertis.general.gost.model.testkit.OfferGen
import zio.test._
import zio.test.Assertion._

object CountersAggregateSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CountersAggregate")(
      test("Sum counters") {
        val first = CountersAggregate(Map(Counter.Banned -> 2L, Counter.Active -> 1L))
        val second = CountersAggregate(Map(Counter.Sold -> 4L, Counter.Active -> 2L))
        val expected = CountersAggregate(Map(Counter.Banned -> 2L, Counter.Active -> 3L, Counter.Sold -> 4L))
        assert(first + second)(equalTo(expected))
      },
      test("Diff counters") {
        val first = CountersAggregate(Map(Counter.Banned -> 2L, Counter.Active -> 1L))
        val second = CountersAggregate(Map(Counter.Sold -> 4L, Counter.Active -> 2L))
        val expected = CountersAggregate(Map(Counter.Banned -> 2L, Counter.Active -> -1L, Counter.Sold -> -4L))
        assert(first - second)(equalTo(expected))
      },
      testM("Create counter for new offer") {
        check(OfferGen.anyActiveOffer) { offer =>
          val expected = CountersAggregate(Map(Counter.Active -> 1L))
          assert(CountersAggregate.forOffer(offer, None))(equalTo(expected))
        }
      },
      testM("Create counter for sold offer") {
        check(OfferGen.anyOffer) { offer =>
          val soldOffer =
            offer.copy(status = Inactive(SellerRecall(Instant.now, SoldOnYandex)), price = InCurrency(100L))
          val expected = CountersAggregate(Map(Counter.Sold -> 1L, Counter.MoneyEarned -> 100L))
          assert(CountersAggregate.forOffer(soldOffer, None))(equalTo(expected))
        }
      },
      testM("Do not change counters if status is not changed") {
        check(OfferGen.anyOffer) { offer =>
          val counters = CountersAggregate(Map(Counter.Active -> 22L, Counter.Banned -> 5L))
          val withCounters = offer.copy(counters = counters)
          assert(CountersAggregate.forOffer(offer, Some(withCounters)))(equalTo(counters))
        }
      },
      testM("Do not change counters if status changed from Inactive(SoldOnYandex) to Removed") {
        check(OfferGen.anyOffer) { offer =>
          val counters = CountersAggregate(Map(Counter.Sold -> 1L, Counter.Sold -> 17756L))
          val status = Inactive(SellerRecall(Instant.now, SoldOnYandex))
          val oldOffer = offer.copy(status = status, counters = counters)
          val newOffer = offer.copy(status = Removed(status))
          assert(CountersAggregate.forOffer(newOffer, Some(oldOffer)))(equalTo(counters))
        }
      }
    )
}
