package ru.yandex.vertis.billing.event

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.DetailsOperator.RawEventDetailsOperator
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.model_core.BaggagePayload.toPayloadQuantity
import ru.yandex.vertis.billing.model_core.gens.{BaggageGen, Producer}
import ru.yandex.vertis.billing.model_core.{Baggage, BaggagePayload, CampaignEvents, EventStat, EventTypes}

class CampaignEventsWithBaggageAggregatorSpec
  extends AnyWordSpec
  with Matchers
  with EventsProviders
  with AsyncSpecBase {

  private val aggregator = new CampaignEventsWithBaggageAggregator(
    EventTypes.CallsRevenue,
    CampaignEventsWithBaggageAggregator.sum,
    RawEventDetailsOperator
  )

  "CampaignEventsWithBaggageAggregator" should {
    "process empty list of baggages" in {
      aggregator.aggregate(Iterable.empty).futureValue shouldBe empty
    }

    "process one baggage" in {
      val baggage = BaggageGen.next.copy(eventType = EventTypes.CallsRevenue)
      val expected = getExpectedCampaignEventsForOneBaggage(baggage)

      val actual = aggregator.aggregate(Iterable(baggage)).futureValue
      actual should contain theSameElementsAs Seq(expected)
    }

    "process two baggage for different campaign" in {
      val baggage1 = BaggageGen.next.copy(eventType = EventTypes.CallsRevenue)
      val expected1 = getExpectedCampaignEventsForOneBaggage(baggage1)

      val baggage2 = BaggageGen
        .suchThat(b => b.header != baggage1.header)
        .next
        .copy(eventTime = baggage1.eventTime, eventType = EventTypes.CallsRevenue)
      val expected2 = getExpectedCampaignEventsForOneBaggage(baggage2)

      val actual = aggregator.aggregate(Iterable(baggage1, baggage2)).futureValue
      actual should contain theSameElementsAs Seq(expected1, expected2)
    }

    "process two baggage with different hour of eventTime" in {
      val baggage1 = BaggageGen.next.copy(eventType = EventTypes.CallsRevenue)
      val expected1 = getExpectedCampaignEventsForOneBaggage(baggage1)

      val baggage2 = BaggageGen.next.copy(
        eventTime = baggage1.eventTime.plusHours(1),
        header = baggage1.header,
        eventType = EventTypes.CallsRevenue
      )
      val expected2 = getExpectedCampaignEventsForOneBaggage(baggage2)

      val actual = aggregator.aggregate(Iterable(baggage1, baggage2)).futureValue
      actual should contain theSameElementsAs Seq(expected1, expected2)
    }

    "process three baggage for same campaign with same hour of event type and same payload" in {
      val baggage1 = BaggageGen.next.copy(eventType = EventTypes.CallsRevenue)

      val baggage2 = BaggageGen.next.copy(
        eventTime = baggage1.eventTime,
        header = baggage1.header,
        eventType = EventTypes.CallsRevenue,
        payload = baggage1.payload
      )
      val baggage3 = BaggageGen.next.copy(
        eventTime = baggage1.eventTime,
        header = baggage1.header,
        eventType = EventTypes.CallsRevenue,
        payload = baggage1.payload
      )

      val baggages = Seq(baggage1, baggage2, baggage3)

      val quantity = baggages.map(b => toPayloadQuantity(b.payload)).sum
      val details = baggages
        .map(b => RawEventDetailsOperator.get(b, Some(b.snapshot.product.totalCost)))
        .fold(None)(RawEventDetailsOperator.fold)
      val eventStat = EventStat(
        EventTypes.CallsRevenue,
        quantity,
        baggages.flatMap(_.hold).toSet,
        details,
        baggages.flatMap(_.eventEpoch).maxOption
      )
      val expected = CampaignEvents(baggage1.snapshot.granulated, eventStat)

      val actual = aggregator.aggregate(baggages).futureValue
      actual should contain theSameElementsAs Seq(expected)
    }

  }

  private def getExpectedCampaignEventsForOneBaggage(baggage: Baggage): CampaignEvents = {
    val quantity = toPayloadQuantity(baggage.payload)
    val details = RawEventDetailsOperator.get(baggage, Some(baggage.snapshot.product.totalCost))
    val eventStat = EventStat(EventTypes.CallsRevenue, quantity, baggage.hold.toSet, details, baggage.eventEpoch)
    CampaignEvents(baggage.snapshot.granulated, eventStat)
  }

}
