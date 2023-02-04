package ru.yandex.vertis.billing.event.call

import akka.stream.scaladsl.Source

import java.io.File
import org.joda.time.{DateTime, LocalDate}
import ru.yandex.vertis.billing.DetailsOperator.RawEventDetailsOperator
import ru.yandex.vertis.billing.async.ActorSystemSpecBase
import ru.yandex.vertis.billing.dao.CampaignHistoryDao.CampaignHistoryPoint
import ru.yandex.vertis.billing.dao.impl.yocto.YoctoCallsSearchDaoFactory
import ru.yandex.vertis.billing.dao.{CallFactDao, CampaignCallDao}
import ru.yandex.vertis.billing.event.Generator._
import ru.yandex.vertis.billing.event._
import ru.yandex.vertis.billing.event.call.CampaignCallRevenueBaseSpec.Timetable
import ru.yandex.vertis.billing.event.failures.DummyTryHandler
import ru.yandex.vertis.billing.event.logging.{LoggedCallFactMatcher, LoggedCallFactModifier}
import ru.yandex.vertis.billing.event.model.CallRevenueBaggageExtractor
import ru.yandex.vertis.billing.model_core.BaggagePayload.PhoneShowIdentifier
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core.TeleponyCallFact.CallResults
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.event.EventContext
import ru.yandex.vertis.billing.model_core.gens.{
  teleponyCallFactGen,
  Producer,
  ResolutionsVectorGen,
  TeleponyCallFactGenCallType,
  TeleponyCallFactGenCallTypes,
  TeleponyCallFactGenParams
}
import ru.yandex.vertis.billing.service.{CallPriceEstimateService, CampaignHistoryService}
import ru.yandex.vertis.billing.settings.RealtyTasksServiceComponents
import ru.yandex.vertis.billing.util.DateTimeUtils.now
import ru.yandex.vertis.billing.util.{DateTimeInterval, DateTimeUtils}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.{immutable, Iterable}
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.implicitConversions
import scala.util.{Random, Success}

/**
  * Base spec for calls revenue operating
  *
  * @author ruslansd
  */
trait CampaignCallRevenueBaseSpec extends EventsProviders with MockitoSupport with ActorSystemSpecBase {

  import materializer.executionContext

  protected val campaignCallDao = {
    val m = mock[CampaignCallDao]
    when(m.getValuableIncomings(?)).thenReturn(Future.successful(immutable.Set.empty[IncomingForCampaign]))
    when(m.write(?)).thenReturn(Success(()))
    m
  }

  protected val extractor = CallRevenueBaggageExtractor(RealtyTasksServiceComponents, EventTypes.CallsRevenue)

  protected val CallFactCount = 10

  implicit def external2CallSettings(external: ExternalCallSettings): CallSettings =
    CallSettings(Some(external.phone), Some(external.objectId))

  // Evaluated calls with Ok status
  protected val ClearCallFacts =
    teleponyCallFactGen(TeleponyCallFactGenParams(TeleponyCallFactGenCallTypes.Redirect))
      .next(CallFactCount)
      .toSeq
      .map { c =>
        val currentTime = now()
        EvaluatedCallFact(
          c.copy(timestamp = currentTime, duration = c.duration + 60.seconds, result = CallResults.Unknown),
          ResolutionsVector(),
          currentTime
        )
      }

  protected def callFactDao(evaluatedCallFacts: Seq[EvaluatedCallFact]) = {
    val m = mock[CallFactDao]
    when(m.get(?)).thenReturn(Future.successful(evaluatedCallFacts))
    m
  }

  private val callsSearchDaoFactory = {
    val file = new File("./yocto-test")
    file.mkdirs()
    new YoctoCallsSearchDaoFactory(file, "test")
  }

  protected def processEvents(
      phoneShows: Iterable[Baggage],
      evaluatedCalls: Seq[EvaluatedCallFact],
      analyzer: CallFactAnalyzer,
      interval: DateTimeInterval): Future[Iterable[CampaignEvents]] = {

    val phoneShowsSource = Source.fromIterator(() => phoneShows.map(Success.apply).iterator)
    val mockPhoneShowService = mock[PhoneShowService]
    when(mockPhoneShowService.stream(?)).thenReturn(phoneShowsSource)

    val mockCampaignHistoryService = mock[CampaignHistoryService]
    when(mockCampaignHistoryService.get(?)(?)).thenReturn(Future.successful(Iterable.empty))

    val aggregator = new CampaignEventsWithBaggageAggregator(
      EventTypes.CallsRevenue,
      CampaignEventsWithBaggageAggregator.sum,
      RawEventDetailsOperator
    )

    val modifier = new CallFactModifier(
      new CallFactMatcher(
        mockPhoneShowService,
        new DummyTryHandler(),
        mockCampaignHistoryService,
        callsSearchDaoFactory,
        callFactDao(evaluatedCalls),
        None
      ) with LoggedCallFactMatcher,
      campaignCallDao,
      Some(analyzer)
    )

    for {
      baggages <- modifier.readAndProcess(interval)
      aggregatedEvents <- aggregator.aggregate(baggages)
    } yield aggregatedEvents
  }

  private val CallSettingVariants =
    ClearCallFacts
      .map(c =>
        c.call match {
          case mcf: MetrikaCallFact =>
            MetrikaCallSettings(mcf.internal, mcf.redirect, mcf.track)
          case tcf: TeleponyCallFact =>
            TeleponyCallSettings(tcf.internal, tcf.redirect, tcf.redirectId, tcf.objectId, tcf.tag, tcf.callbackOrderId)
        }
      )
      .toVector

  private val CallFactTimestampVariants =
    ClearCallFacts.map(_.call.timestamp.minusMinutes(CallFactCount)).toVector

  private def randomFields =
    Iterable(
      Extractor.TimestampCellName ->
        CallFactTimestampVariants(Random.nextInt(CallFactTimestampVariants.size)).toString
    )

  protected def input(revenue: Funds, product: Funds, context: EventContext = EventContext(Some(Timetable))) = {
    CallSettingVariants.flatMap { cs =>
      EventRecordGen
        .next(1)
        .toVector
        .map(withProduct(Product(Placement(CostPerCall(product)))))
        .map(withCallRevenue(revenue))
        .map(withFields(randomFields))
        .map(withCallSettings(cs))
        .map(withPhoneShowId(PhoneShowIdentifier(cs.objectId, cs.redirect, None)))
        .map(withDateTime(DateTime.now().minusDays(1)))
        .map(withEventContext(context))
    }
  }

  protected def inputBaggage(revenue: Funds, product: Funds, context: EventContext = EventContext(Some(Timetable))) =
    input(revenue, product, context).map(e => extractor.extract(e).get)

  // Generates calls with status
  protected def randomCallsWithStatus =
    ClearCallFacts.map(_.copy(resolutions = ResolutionsVectorGen.next))

  protected def countClearCalls(withSuspiciousCalls: Iterable[EvaluatedCallFact]) =
    withSuspiciousCalls.count { c =>
      !CallFactAnalyzer.getCompositeResolution(c.resolutions).contains(Resolution.Statuses.Fail)
    }

}

object CampaignCallRevenueBaseSpec {

  private val Timetable = {
    val time = {
      val from = DateTimeInterval.currentDay.from.toLocalTime
      val to = DateTimeInterval.currentDay.to.toLocalTime
      LocalTimeInterval(from, to)
    }
    val from = LocalDate.now().minusMonths(1)
    val to = LocalDate.now().plusMonths(1)
    new Timetable(Iterable(LocalDateInterval(from, to, Iterable(time))), DateTimeUtils.TimeZone)
  }
}
