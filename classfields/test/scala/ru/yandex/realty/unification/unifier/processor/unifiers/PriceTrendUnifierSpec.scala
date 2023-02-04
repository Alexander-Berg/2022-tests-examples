package ru.yandex.realty.unification.unifier.processor.unifiers

import org.joda.time.{Duration, Instant}
import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.history.PriceTrend._
import ru.yandex.realty.model.history._
import ru.yandex.realty.model.offer.{Money, Offer, OfferType, Transaction}
import ru.yandex.realty.model.raw.RawOfferImpl
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class PriceTrendUnifierSpec extends AsyncSpecBase with Matchers {

  import PriceTrendUnifierSpec._

  private val unifier = new PriceTrendUnifier

  implicit val trace: Traced = Traced.empty

  private def runUnifier(
    priceHistory: PriceHistory,
    newPrice: Long,
    offerType: OfferType = OfferType.SELL
  ): Transaction = {
    val transaction = new Transaction
    transaction.setWholeInRubles(getMoney(newPrice))

    val offer = new Offer
    offer.setOfferType(offerType)
    offer.setTransaction(transaction)

    val offerHistory = OfferHistory.build(
      false,
      getPastTime(10),
      priceHistory,
      RgidHistory.EMPTY
    )

    unifier.process(new RawOfferImpl, offer, offerHistory).futureValue
    transaction
  }

  private def assertValues(
    transaction: Transaction,
    trend: PriceTrend,
    previousPrice: Money,
    priceUpdated: Boolean,
    hasPriceHistory: Boolean = true
  ) {
    transaction.getTrend shouldEqual trend
    transaction.isHasPriceHistory shouldEqual hasPriceHistory
    transaction.getPreviousPrice shouldEqual previousPrice
    transaction.getPriceUpdated shouldEqual priceUpdated
  }

  "PriceTrendUnifier" should {
    "set Unchanged trend for history with one price" in {
      val priceHistory = PriceHistory.EMPTY.append(getMoney(5000000))
      val t = runUnifier(priceHistory, 5000000)
      assertValues(t, UNCHANGED, priceUpdated = false, previousPrice = null, hasPriceHistory = false)
    }

    "set Increased trend when price increased" in {
      val priceHistory = DefaultHistory.append(getMoney(12000000))
      val t = runUnifier(priceHistory, 12000000)
      assertValues(t, INCREASED, priceUpdated = false, previousPrice = getMoney(10000000))
    }

    "set Decreased trend when price decreased" in {
      val t = runUnifier(DefaultHistory, 10000000)
      assertValues(t, DECREASED, priceUpdated = false, previousPrice = getMoney(11000000))
    }

    "not set priceUpdated when price was changed insignificantly for Sell offer" in {
      var t = runUnifier(DefaultHistory, 10050000)
      assertValues(t, INCREASED, priceUpdated = false, previousPrice = getMoney(10000000))

      val priceHistory = DefaultHistory.append(getMoney(10040000))
      t = runUnifier(priceHistory, 10050000)
      assertValues(t, INCREASED, priceUpdated = false, previousPrice = getMoney(10000000))
    }

    "not set priceUpdated when price was changed insignificantly for Rent offer" in {
      var t = runUnifier(DefaultHistory, 10150000, OfferType.RENT)
      assertValues(t, INCREASED, priceUpdated = false, previousPrice = getMoney(10000000))

      val priceHistory = DefaultHistory.append(getMoney(10090000))
      t = runUnifier(priceHistory, 10180000, OfferType.RENT)
      assertValues(t, INCREASED, priceUpdated = false, previousPrice = getMoney(10000000))
    }

    "set priceUpdated when price was changed significantly for Sell offer" in {
      var t = runUnifier(DefaultHistory, 10150000)
      assertValues(t, INCREASED, priceUpdated = true, previousPrice = getMoney(10000000))

      val priceHistory = DefaultHistory.append(getMoney(10001000))
      t = runUnifier(priceHistory, 10150000)
      assertValues(t, INCREASED, priceUpdated = true, previousPrice = getMoney(10001000))
    }

    "set priceUpdated when price was changed significantly for Rent offer" in {
      var t = runUnifier(DefaultHistory, 10350000, OfferType.RENT)
      assertValues(t, INCREASED, priceUpdated = true, previousPrice = getMoney(10000000))

      val priceHistory = DefaultHistory.append(getMoney(10040000))
      t = runUnifier(priceHistory, 10350000, OfferType.RENT)
      assertValues(t, INCREASED, priceUpdated = true, previousPrice = getMoney(10040000))
    }

    "not set priceUpdated when last price change was neutralised" in {
      val priceHistory = DefaultHistory.append(getMoney(10050000))
      val t = runUnifier(priceHistory, 10000000)
      assertValues(t, DECREASED, priceUpdated = false, previousPrice = getMoney(11000000))
    }
  }
}

object PriceTrendUnifierSpec {

  private val DefaultHistory = PriceHistory.EMPTY
    .append(getPastTime(10), getMoney(11000000))
    .append(getPastTime(8), getMoney(10000000))

  private def getPastTime(days: Int): Instant =
    new Instant().minus(Duration.standardDays(days))

  private def getMoney(scaledValue: Long): Money =
    Money.scaledOf(Currency.RUR, scaledValue)
}
