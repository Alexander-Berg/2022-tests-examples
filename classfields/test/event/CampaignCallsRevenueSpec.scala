package ru.yandex.vertis.billing.event

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.event.CampaignCallsRevenueSpec.FirstPriceAuctionStartDate
import ru.yandex.vertis.billing.event.call.{CampaignCallRevenueBaseSpec, RealtyCallFactAnalyzer}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.event.EventContext
import ru.yandex.vertis.billing.settings.RealtyComponents
import ru.yandex.vertis.billing.util.{DateTimeInterval, DateTimeUtils}

import scala.collection._

/**
  * Specs for calls revenue operating
  *
  * @author alesavin
  */
class CampaignCallsRevenueSpec extends AnyWordSpec with CampaignCallRevenueBaseSpec with Matchers with AsyncSpecBase {

  protected val TestIterations = 50

  private val Analyzer = new RealtyCallFactAnalyzer(RealtyComponents)

  def test(events: Iterable[CampaignEvents], payload: Funds) = {
    events.size should be > 0
    events.find(_.stat.eventType != EventTypes.CallsRevenue) should be(None)
    val expectedSum = events
      .filter(_.stat.value != 0)
      .map(e => RealtyComponents.callPayloadCorrection(e.snapshot, payload))
    events.map(_.stat.value).sum should be(expectedSum.sum)
  }

  "callsRevenueEventsReader" should {
    "get CampaignEvents with product bid" in {
      val res =
        processEvents(inputBaggage(0L, 50000L), ClearCallFacts, Analyzer, DateTimeInterval.currentDay).futureValue
      test(res, 0L)
    }

    "get CampaignEvents with product bid with suspicious calls" in {
      (1 to TestIterations).foreach { _ =>
        val withSuspiciousCalls = randomCallsWithStatus
        val res =
          processEvents(
            inputBaggage(0L, 50000L),
            withSuspiciousCalls,
            Analyzer,
            DateTimeInterval.currentDay
          ).futureValue
        test(res, 0L)
      }
    }

    "get CampaignEvents with call revenue bid" in {
      val res =
        processEvents(inputBaggage(60000L, 70000L), ClearCallFacts, Analyzer, DateTimeInterval.currentDay).futureValue
      test(res, 60000L)
    }

    "get CampaignEvents with call revenue bid with suspicious calls" in {
      (1 to TestIterations).foreach { _ =>
        val withSuspiciousCalls = randomCallsWithStatus
        val res = processEvents(
          inputBaggage(60000L, 70000L),
          withSuspiciousCalls,
          Analyzer,
          DateTimeInterval.currentDay
        ).futureValue
        test(res, 60000L)
      }
    }

    "get CampaignEvents with call revenue bid more than product" in {
      val res =
        processEvents(inputBaggage(80000L, 70000L), ClearCallFacts, Analyzer, DateTimeInterval.currentDay).futureValue
      test(res, 80000L)
    }

    "get CampaignEvents with call revenue bid more than product with suspicious calls" in {
      (1 to TestIterations).foreach { _ =>
        val withSuspiciousCalls = randomCallsWithStatus
        val res = processEvents(
          inputBaggage(80000L, 70000L),
          withSuspiciousCalls,
          Analyzer,
          DateTimeInterval.currentDay
        ).futureValue
        test(res, 80000L)
      }
    }

    "get calls after first auction start date " in {
      val records = input(80000L, 70000L)
        .map(withDateTime(afterFirstAuctionDate(DateTime.now()).minusDays(1)))
        .map(withDeadline(DateTime.now().plusMonths(2)))
        .map(e => extractor.extract(e).get)
      val interval = DateTimeUtils.wholeDay(afterFirstAuctionDate(DateTime.now()))
      val callFacts =
        ClearCallFacts.map(ec => ec.copy(call = ec.call.withTimestamp(afterFirstAuctionDate(ec.call.timestamp))))
      val res = processEvents(records, callFacts, Analyzer, interval).futureValue
      test(res, 80000L)
      res.map(_.stat.value).sum shouldBe res.size * 70000L
    }
  }

  def afterFirstAuctionDate(time: DateTime) = {
    val millisOfDay = time.getMillisOfDay
    FirstPriceAuctionStartDate.plus(millisOfDay)
  }

  override protected def name: String = "CampaignCallsRevenueSpec"

}

object CampaignCallsRevenueSpec {

  private val FirstPriceAuctionStartDate =
    new DateTime("2017-07-01T00:00:00.000+03:00")
}
