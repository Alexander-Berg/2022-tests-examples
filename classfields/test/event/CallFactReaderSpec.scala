package ru.yandex.vertis.billing.event

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.event.call.{
  CallFactMatcher,
  CallFactModifier,
  CampaignCallRevenueBaseSpec,
  RealtyCallFactAnalyzer
}
import ru.yandex.vertis.billing.event.logging.LoggedCallFactModifier
import ru.yandex.vertis.billing.model_core.BaggagePayload.EventSources
import ru.yandex.vertis.billing.model_core.CampaignCallFact._
import ru.yandex.vertis.billing.model_core.TeleponyCallFact.CallResults
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.event.EventContext
import ru.yandex.vertis.billing.model_core.gens.{
  randomPrintableString,
  withDefinedPrice,
  CampaignHeaderGen,
  Producer,
  UserOfferIdGen
}
import ru.yandex.vertis.billing.settings.RealtyComponents
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.now
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection._
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Specs for [[CallFactModifier]]
  */
class CallFactReaderSpec
  extends AnyWordSpec
  with CampaignCallRevenueBaseSpec
  with Matchers
  with MockitoSupport
  with AsyncSpecBase {

  def mockCallFactMatcher(output: Iterable[Baggage]): CallFactMatcher = {
    val mockCallFactMatcher = mock[CallFactMatcher]
    when(mockCallFactMatcher.read(?)).thenReturn(Future.successful(output))
    mockCallFactMatcher
  }

  def getModifier(input: Iterable[Baggage]) =
    new CallFactModifier(
      mockCallFactMatcher(input),
      campaignCallDao,
      Some(new RealtyCallFactAnalyzer(RealtyComponents))
    ) with LoggedCallFactModifier

  val CallFact = new TeleponyCallFact(
    now(),
    "123",
    Phone.unapply("7(812)1234567"),
    Phone.unapply("7(812)7654321").get,
    5.seconds,
    600.seconds,
    Some("1"),
    "1",
    None,
    CallResults.Unknown,
    None,
    Some(randomPrintableString(5)),
    None,
    None,
    false
  )

  val matchedCall = BaggagePayload.CallWithResolution(
    CallFact,
    CallFact.timestamp,
    33L,
    ResolutionsVector(),
    EventSources.PhoneShows
  )

  val callWithStatus = BaggagePayload.CallWithStatus(
    CallFact,
    CallFact.timestamp,
    33L,
    Statuses.Ok,
    EventSources.PhoneShows,
    ResolutionsVector(),
    DetailedStatuses.Ok
  )

  val OfferId = UserOfferIdGen.next

  val product = Product(Placement(CostPerCall(FixPrice(100))))

  val Baggage = new Baggage(
    now(),
    withDefinedPrice(CampaignHeaderGen.next).copy(product = product),
    OfferId.user,
    EventTypes.CallsRevenue,
    BaggageObjectId.Empty,
    matchedCall,
    None,
    EventContext(Some(CallFactAnalyzerBaseSpec.AlwaysWork))
  )

  def payload(
      resolution: Option[ResolutionsVector] = None,
      revenue: Option[Funds] = None,
      incoming: Option[String] = None,
      time: Option[DateTime] = None,
      duration: Option[FiniteDuration] = None
    )(p: BaggagePayload.CallWithResolution): BaggagePayload = {
    var updatedCall = p.fact
    time.foreach(t => updatedCall = updatedCall.withTimestamp(t))
    incoming.foreach(s => updatedCall = updatedCall.withIncoming(s))
    duration.foreach(d => updatedCall = updatedCall.withDuration(d))
    var updatedPayload = p
    resolution.foreach(s => updatedPayload = updatedPayload.copy(resolution = s))
    revenue.foreach(r => updatedPayload = updatedPayload.copy(revenue = r))
    updatedPayload.copy(fact = updatedCall)
  }

  def baggage(
      resolution: Option[ResolutionsVector] = None,
      revenue: Option[Funds] = None,
      incoming: Option[String] = None,
      time: Option[DateTime] = None,
      duration: Option[FiniteDuration] = None
    )(b: Baggage = Baggage): Baggage = {
    val p = payload(resolution, revenue, incoming, time, duration) {
      b.payload.asInstanceOf[BaggagePayload.CallWithResolution]
    }
    Baggage.copy(payload = p)
  }

  def expectedCall(
      status: Status,
      payload: BaggagePayload.CallWithResolution,
      detailedStatus: Option[DetailedStatus] = None): Baggage = {
    val revenue =
      if (status == Statuses.Ok)
        RealtyComponents.callPayloadCorrection(Baggage.snapshot, matchedCall.revenue)
      else
        0L
    Baggage.copy(payload =
      callWithStatus.copy(
        phoneShowTime = payload.phoneShowTime,
        status = status,
        revenue = revenue,
        detailedStatus = detailedStatus.getOrElse(callWithStatus.detailedStatus),
        fact = callWithStatus.fact
          .withTimestamp(payload.fact.timestamp)
          .withIncoming(payload.fact.incoming)
          .withDuration(payload.fact.duration)
      )
    )
  }

  def expectedBaggage(status: Status, baggage: Baggage, detailedStatus: Option[DetailedStatus] = None): Baggage =
    expectedCall(status, baggage.payload.asInstanceOf[BaggagePayload.CallWithResolution], detailedStatus)

  "CallFactReader" should {

    "operate empty event stream" in {
      val events = mutable.ListBuffer[Baggage]()
      val input = Iterable.empty[Baggage]
      val res = getModifier(input).readAndProcess(DateTimeInterval.currentDay).futureValue
      res.foreach(accumulate(events))
      assert(events.isEmpty)
    }

    "operate fact" in {
      val events = mutable.ListBuffer[Baggage]()
      val input = Iterable(Baggage)
      val res = getModifier(input).readAndProcess(DateTimeInterval.currentDay).futureValue
      res.foreach(accumulate(events))

      val expected = Iterable(
        expectedCall(Statuses.Ok, matchedCall)
      )
      events.toList should be(expected)
    }

    "operate repeated fact" in {
      val events = mutable.ListBuffer[Baggage]()
      val repeated = baggage(time = Some(matchedCall.fact.timestamp.plusHours(1)))()
      val input = Iterable(
        Baggage,
        repeated
      )
      val expected = Iterable(
        expectedCall(Statuses.Ok, matchedCall),
        expectedBaggage(Statuses.RepeatedOnCurrentDay, repeated, Some(DetailedStatuses.Repeated))
      )
      val res = getModifier(input).readAndProcess(DateTimeInterval.currentDay).futureValue
      res.foreach(accumulate(events))

      events.toList should be(expected)
    }

    "operate fact with empty incoming" in {
      val events = mutable.ListBuffer[Baggage]()
      val emptyIncoming = baggage(incoming = Some(EmptyIncoming))()
      val input = Iterable(
        emptyIncoming
      )
      val expected = Iterable(expectedBaggage(Statuses.Ok, emptyIncoming))
      val res = getModifier(input).readAndProcess(DateTimeInterval.currentDay).futureValue
      res.foreach(accumulate(events))
      events.toList should be(expected)
    }

    "operate repeated fact with empty incoming" in {
      val events = mutable.ListBuffer[Baggage]()
      val emptyIncoming = baggage(incoming = Some(EmptyIncoming))()
      val input = Iterable(
        emptyIncoming,
        emptyIncoming
      )
      val expected = Iterable(expectedBaggage(Statuses.Ok, emptyIncoming), expectedBaggage(Statuses.Ok, emptyIncoming))
      val res = getModifier(input).readAndProcess(DateTimeInterval.currentDay).futureValue
      res.foreach(accumulate(events))
      events.toList should be(expected)
    }

    "operate non-suspicious fact (found show near 3h)" in {
      val events = mutable.ListBuffer[Baggage]()
      val near3Hour = baggage(duration = Some(45.seconds), time = Some(CallFact.timestamp.plusMinutes(179)))()
      val input = Iterable(near3Hour)
      val expected = Iterable(expectedBaggage(Statuses.Ok, near3Hour))

      val res = getModifier(input).readAndProcess(DateTimeInterval.currentDay).futureValue
      res.foreach(accumulate(events))
      events.toList should be(expected)
    }

    "operate non-suspicious fact (duration more 60 sec)" in {
      val events = mutable.ListBuffer[Baggage]()
      val nonSuspicious = baggage(duration = Some(61.seconds), time = Some(CallFact.timestamp.plusMinutes(181)))()
      val input = Iterable(nonSuspicious)
      val expected = Iterable(expectedBaggage(Statuses.Ok, nonSuspicious))
      val res = getModifier(input).readAndProcess(DateTimeInterval.currentDay).futureValue
      res.foreach(accumulate(events))
      events.toList should be(expected)
    }

  }

  override protected def name: String = "CallFactReaderSpec"
}
