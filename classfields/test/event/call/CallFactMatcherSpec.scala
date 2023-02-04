package ru.yandex.vertis.billing.event.call

import akka.stream.scaladsl.Source
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.{ActorSystemSpecBase, AsyncSpecBase}
import ru.yandex.vertis.billing.dao.CampaignHistoryDao.CampaignHistoryPoint
import ru.yandex.vertis.billing.dao.impl.yocto.YoctoCallsSearchDaoFactory
import ru.yandex.vertis.billing.dao.{CallFactDao, CallsSearchDaoFactory, CampaignHistoryDao}
import ru.yandex.vertis.billing.event.call.CallFactMatcher.UnavailableCallPriceException
import ru.yandex.vertis.billing.event.call.CallFactMatcherSpec.{
  callDaoMock,
  DummyCallPriceService,
  DummyFailureCallPriceService
}
import ru.yandex.vertis.billing.event.failures.DummyTryHandler
import ru.yandex.vertis.billing.event.model.CallRevenueBaggageExtractor
import ru.yandex.vertis.billing.event.EventsProviders
import ru.yandex.vertis.billing.model_core.BaggagePayload.{
  CallWithOptRevenue,
  EventSource,
  EventSources,
  PhoneShowIdentifier
}
import ru.yandex.vertis.billing.model_core.gens.{
  evaluatedCallFactGen,
  BaggageGen,
  CallSettingsGen,
  CampaignHeaderGen,
  CampaignSettingsGen,
  EvaluatedCallFactGenParams,
  Producer,
  ProductForCallGen,
  TeleponyCallFactGenCallType,
  TeleponyCallFactGenCallTypes,
  TeleponyCallFactGenParams
}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.service.{CallPriceEstimateService, CampaignHistoryService}
import ru.yandex.vertis.billing.settings.TasksServiceComponents
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.mockito.MockitoSupport

import java.io.File
import scala.concurrent.Future
import scala.util.{Failure, Random, Success, Try}
import scala.concurrent.duration._

/**
  * Spec on [[CallFactMatcher]]
  */
class CallFactMatcherSpec
  extends AnyWordSpec
  with Matchers
  with EventsProviders
  with ActorSystemSpecBase
  with MockitoSupport
  with AsyncSpecBase {

  import materializer.executionContext

  val file = new File("./yocto-test")
  file.mkdirs()

  val factory: CallsSearchDaoFactory =
    new YoctoCallsSearchDaoFactory(file, "test")

  val baggageExtractor = CallRevenueBaggageExtractor(TasksServiceComponents("realty"), EventTypes.CallsRevenue)

  def getMatcher(
      phoneShows: Iterable[Baggage],
      campaigns: Iterable[CampaignHistoryPoint],
      callDao: CallFactDao,
      callPriceService: Option[CallPriceEstimateService] = None) = {

    val phoneShowsSource = Source.fromIterator(() => phoneShows.map(Success.apply).iterator)
    val mockPhoneShowService = mock[PhoneShowService]
    when(mockPhoneShowService.stream(?)).thenReturn(phoneShowsSource)

    val mockCampaignHistoryService = mock[CampaignHistoryService]
    when(mockCampaignHistoryService.get(?)(?)).thenReturn(Future.successful(campaigns))

    new CallFactMatcher(
      mockPhoneShowService,
      new DummyTryHandler(),
      mockCampaignHistoryService,
      factory,
      callDao,
      callPriceService
    )
  }

  case class CallFactWithBaggage(callFact: EvaluatedCallFact, baggage: Baggage)

  case class CallFactWithCampaign(callFact: EvaluatedCallFact, campaign: CampaignHistoryPoint)

  private def check(
      outputBaggage: Baggage,
      expectedCall: CallFact,
      expectedCampaignId: CampaignId,
      expectedEventSource: EventSource,
      expectedCostInPayload: Option[Funds] = None,
      needPhoneShowTimeCheck: Boolean = true): Unit = {
    outputBaggage.eventTime shouldBe expectedCall.timestamp
    outputBaggage.header.id shouldBe expectedCampaignId
    outputBaggage.eventType shouldBe EventTypes.CallsRevenue

    val outputPayload = outputBaggage.payload.asInstanceOf[BaggagePayload.CallWithResolution]
    outputPayload.fact shouldBe expectedCall
    outputPayload.source shouldBe expectedEventSource
    if (needPhoneShowTimeCheck) {
      outputPayload.phoneShowTime.isAfter(expectedCall.timestamp) shouldBe false
    }
    expectedCostInPayload match {
      case Some(cost) =>
        outputPayload.revenue shouldBe cost
      case None =>
        val expectedPrice = DummyCallPriceService.getPrice(expectedCall, outputBaggage.header).get
        outputPayload.revenue shouldBe expectedPrice
    }
    ()
  }

  "CallFactMatcher" should {
    "correctly match non intersect events" in {
      val byShow = genEcf(TeleponyCallFactGenCallTypes.Redirect).next(10).toList
      val byHistory = genEcf().next(1).toList
      val byShowWithBaggage = byShow.map { f =>
        CallFactWithBaggage(f, asBaggage(f, EventSources.PhoneShows))
      }
      val byHistoryWithCampaigns = byHistory.map { f =>
        CallFactWithCampaign(f, toCampaignHistoryPoint(f.call, withRevenue = true))
      }

      val matcher = getMatcher(
        phoneShows = byShowWithBaggage.map(_.baggage),
        campaigns = byHistoryWithCampaigns.map(_.campaign),
        callDao = callDaoMock(byShow ++ byHistory)
      )

      val result = matcher.read(DateTimeInterval.currentDay).futureValue

      result.size shouldBe (byShow.size + byHistory.size)

      byShowWithBaggage.foreach { input =>
        val matched = result.filter { b =>
          b.payload.asInstanceOf[BaggagePayload.CallWithResolution].fact == input.callFact.call
        }
        matched.size shouldBe 1
        check(
          outputBaggage = matched.head,
          expectedCall = input.callFact.call,
          expectedCampaignId = input.baggage.header.id,
          expectedEventSource = EventSources.PhoneShows,
          expectedCostInPayload = input.baggage.payload.asInstanceOf[CallWithOptRevenue].revenue
        )
      }

      byHistoryWithCampaigns.foreach { input =>
        val matched = result.filter { b =>
          b.payload.asInstanceOf[BaggagePayload.CallWithResolution].fact == input.callFact.call
        }
        matched.size shouldBe 1
        check(
          outputBaggage = matched.head,
          expectedCall = input.callFact.call,
          expectedCampaignId = input.campaign.header.id,
          expectedEventSource = EventSources.CampaignHistory,
          expectedCostInPayload = Some(input.campaign.header.product.totalCost)
        )
      }
    }

    "correctly on campaign history events" in {
      val byHistory = genEcf().next(500).toList
      val byHistoryWithCampaigns = byHistory.map { f =>
        CallFactWithCampaign(f, toCampaignHistoryPoint(f.call, withRevenue = true))
      }

      val matcher = getMatcher(
        phoneShows = Iterable.empty,
        campaigns = byHistoryWithCampaigns.map(_.campaign),
        callDao = callDaoMock(byHistory)
      )

      val result = matcher.read(DateTimeInterval.currentDay).futureValue

      result.size shouldBe byHistoryWithCampaigns.size

      byHistoryWithCampaigns.foreach { input =>
        val matched = result.filter { b =>
          b.payload.asInstanceOf[BaggagePayload.CallWithResolution].fact == input.callFact.call
        }
        matched.size shouldBe 1
        check(
          outputBaggage = matched.head,
          expectedCall = input.callFact.call,
          expectedCampaignId = input.campaign.header.id,
          expectedEventSource = EventSources.CampaignHistory,
          expectedCostInPayload = Some(input.campaign.header.product.totalCost)
        )
      }
    }

    "match at first on phone shows" in {
      val calls = genEcf(TeleponyCallFactGenCallTypes.Redirect).next(1000).toList
      val byShowWithBaggage = calls.map { f =>
        CallFactWithBaggage(f, asBaggage(f, EventSources.PhoneShows))
      }
      val byHistoryWithCampaigns = calls.map { f =>
        CallFactWithCampaign(f, toCampaignHistoryPoint(f.call, withRevenue = true))
      }

      val matcher = getMatcher(
        phoneShows = byShowWithBaggage.map(_.baggage),
        campaigns = byHistoryWithCampaigns.map(_.campaign),
        callDao = callDaoMock(calls)
      )

      val result = matcher.read(DateTimeInterval.currentDay).futureValue

      result.size shouldBe calls.size

      byShowWithBaggage.foreach { input =>
        val matched = result.filter { b =>
          b.payload.asInstanceOf[BaggagePayload.CallWithResolution].fact == input.callFact.call
        }
        matched.size shouldBe 1
        check(
          outputBaggage = matched.head,
          expectedCall = input.callFact.call,
          expectedCampaignId = input.baggage.header.id,
          expectedEventSource = EventSources.PhoneShows,
          expectedCostInPayload = input.baggage.payload.asInstanceOf[CallWithOptRevenue].revenue
        )
      }
    }

    "correctly match with call price service" in {
      val byShow = genEcf(TeleponyCallFactGenCallTypes.Redirect).next(500).toList
      val byHistory = genEcf().next(500).toList
      val byShowWithBaggage = byShow.map { f =>
        CallFactWithBaggage(f, asBaggage(f, EventSources.PhoneShows))
      }
      val byHistoryWithCampaigns = byHistory.map { f =>
        CallFactWithCampaign(f, toCampaignHistoryPoint(f.call, withRevenue = true))
      }

      val matcher = getMatcher(
        phoneShows = byShowWithBaggage.map(_.baggage),
        campaigns = byHistoryWithCampaigns.map(_.campaign),
        callDao = callDaoMock(byShow ++ byHistory),
        callPriceService = Some(DummyCallPriceService)
      )

      val result = matcher.read(DateTimeInterval.currentDay).futureValue

      result.size shouldBe (byShow.size + byHistory.size)

      byShowWithBaggage.foreach { input =>
        val matched = result.filter { b =>
          b.payload.asInstanceOf[BaggagePayload.CallWithResolution].fact == input.callFact.call
        }
        matched.size shouldBe 1
        check(
          outputBaggage = matched.head,
          expectedCall = input.callFact.call,
          expectedCampaignId = input.baggage.header.id,
          expectedEventSource = EventSources.PhoneShows
        )
      }

      byHistoryWithCampaigns.foreach { input =>
        val matched = result.filter { b =>
          b.payload.asInstanceOf[BaggagePayload.CallWithResolution].fact == input.callFact.call
        }
        matched.size shouldBe 1
        check(
          outputBaggage = matched.head,
          expectedCall = input.callFact.call,
          expectedCampaignId = input.campaign.header.id,
          expectedEventSource = EventSources.CampaignHistory
        )
      }
    }

    "fail if no price for call" in {
      val facts = genEcf().next(500).toList
      val baggages = facts.map { f =>
        val baggage = asBaggage(f, EventSources.PhoneShows, withRevenue = false)
        CallFactWithBaggage(f, baggage)
      }

      val matcher = getMatcher(
        phoneShows = baggages.map(_.baggage),
        campaigns = Iterable.empty,
        callDao = callDaoMock(facts),
        callPriceService = None
      )

      val actualException = matcher.read(DateTimeInterval.currentDay).failed.futureValue
      actualException shouldBe an[UnavailableCallPriceException]
    }

    "fail if price service failed" in {
      val facts = genEcf().next(500).toList
      val baggages = facts.map { f =>
        val baggage = asBaggage(f, EventSources.PhoneShows)
        CallFactWithBaggage(f, baggage)
      }

      val matcher = getMatcher(
        phoneShows = baggages.map(_.baggage),
        campaigns = Iterable.empty,
        callDao = callDaoMock(facts),
        callPriceService = Some(DummyFailureCallPriceService)
      )

      val actualException = matcher.read(DateTimeInterval.currentDay).failed.futureValue
      actualException shouldBe an[UnavailableCallPriceException]
    }

  }

  def genEcf(callType: TeleponyCallFactGenCallType = TeleponyCallFactGenCallTypes.Any) = {
    val currentDayTimestamp =
      DateTimeInterval.currentDay.from.plusSeconds(Gen.choose(1, 24.hours.toSeconds - 1).next.toInt)
    evaluatedCallFactGen(EvaluatedCallFactGenParams(TeleponyCallFactGenParams(callType)))
      .map { x =>
        x.copy(call = {
          x.call match {
            case fact: TeleponyCallFact => fact.copy(timestamp = currentDayTimestamp)
            case fact: MetrikaCallFact => fact.copy(timestamp = currentDayTimestamp)
          }
        })
      }
  }

  def asBaggage(f: EvaluatedCallFact, source: EventSource, withRevenue: Boolean = true): Baggage = {
    val b = BaggageGen.next
    val identifier = source match {
      case EventSources.PhoneShows =>
        PhoneShowIdentifier(f.call.objectId, f.call.redirect, f.call.tag)
      case EventSources.CampaignHistory =>
        PhoneShowIdentifier(f.call.objectId, None)
    }
    val revenue = if (withRevenue) {
      Some(Random.nextInt(1000).toLong)
    } else {
      None
    }
    val payload = BaggagePayload.CallWithOptRevenue(
      identifier,
      source,
      revenue
    )
    b.copy(payload = payload, eventType = EventTypes.CallsRevenue, eventTime = f.call.timestamp.minusMinutes(10))
  }

  def toCampaignHistoryPoint(call: CallFact, withRevenue: Boolean): CampaignHistoryPoint = {
    val product = if (withRevenue) {
      ProductForCallGen.suchThat(_.hasDefinedCost).next
    } else {
      ProductForCallGen.suchThat(!_.hasDefinedCost).next
    }

    val callSettings = CallSettingsGen.next.copy(objectId = Some(call.objectId))
    val settings = CampaignSettingsGen.next.copy(callSettings = Some(callSettings))

    val header = CampaignHeaderGen.next.copy(
      product = product,
      settings = settings,
      epoch = Some(call.timestamp.minusMinutes(10).getMillis)
    )
    CampaignHistoryPoint(header, CampaignHistoryDao.EventTypes.Update)
  }

  override protected def name: String = "CallFactMatcherSpec"
}

object CallFactMatcherSpec {

  case object DummyCallPriceService extends CallPriceEstimateService {

    override def getPrice(call: CallFact, header: CampaignHeader): Try[Funds] =
      Success(call.timestamp.getMillis % 100000 / 100 * 100)
  }

  case object DummyFailureCallPriceService extends CallPriceEstimateService {

    override def getPrice(call: CallFact, header: CampaignHeader): Try[Funds] =
      Failure(new RuntimeException("artificial"))
  }

  private def callDaoMock(facts: Seq[EvaluatedCallFact]) = {
    import ru.yandex.vertis.mockito.MockitoSupport.{?, mock, when}
    val m = mock[CallFactDao]
    when(m.get(?)).thenReturn(Future.successful(facts))
    m
  }
}
