package ru.yandex.vertis.billing.tasks

import java.io.File

import org.joda.time.DateTime
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.OperationComponent
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.{CallFactDao, CampaignEventDao}
import ru.yandex.vertis.billing.event._
import ru.yandex.vertis.billing.event.call._
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{evaluatedCallFactGen, BaggageGen, CampaignEventsGen, Producer}
import ru.yandex.vertis.billing.service.EpochService
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.Iterable
import scala.concurrent.Future

/**
  * Tests for calls statistics update task
  */
class CallsRevenueStatisticsUpdateSpec
  extends AnyWordSpec
  with Matchers
  with EventsProviders
  with MockitoSupport
  with AsyncSpecBase {

  private def task(
      calls: Future[Iterable[EvaluatedCallFact]],
      processedEvents: Future[Iterable[Baggage]],
      aggregatedEvents: Future[Iterable[CampaignEvents]]) = {
    val mockCampaignEventDao = mock[CampaignEventDao]
    when(mockCampaignEventDao.write(?, ?)).thenReturn(Future.unit)

    val mockEpochService = mock[EpochService]
    when(mockEpochService.get(?)).thenReturn(Future.successful(100L))
    when(mockEpochService.set(?, ?)).thenReturn(Future.unit)

    val mockCallFactDao = mock[CallFactDao]
    when(mockCallFactDao.get(?)).thenReturn(calls.map(_.toSeq))

    val mockCallFactModifier = mock[CallFactModifier]
    when(mockCallFactModifier.readAndProcess(?)).thenReturn(processedEvents)

    val mockAggregator = mock[CampaignEventsWithBaggageAggregator]
    when(mockAggregator.aggregate(?)).thenReturn(aggregatedEvents)

    new CallsStatisticsUpdateTask(
      eventService = mockCampaignEventDao,
      callFactModifier = mockCallFactModifier,
      aggregator = mockAggregator,
      callFactDao = mockCallFactDao,
      epochService = mockEpochService
    ) with OperationComponent {
      override def serviceName: String = "test"
    }
  }

  "StatisticsUpdateTask" should {
    "success process calls" in {
      val calls = evaluatedCallFactGen().next(5)
      val baggages = BaggageGen.next(5)
      val events = CampaignEventsGen.next(3)

      val statisticsUpdateTask = task(Future.successful(calls), Future.successful(baggages), Future.successful(events))

      statisticsUpdateTask.execute(ConfigFactory.empty()).futureValue shouldBe ()
    }
    "failed in getting calls" in {
      val baggages = BaggageGen.next(5)
      val events = CampaignEventsGen.next(3)

      val statisticsUpdateTask = task(
        Future.failed(new IllegalArgumentException("some")),
        Future.successful(baggages),
        Future.successful(events)
      )

      statisticsUpdateTask.execute(ConfigFactory.empty()).failed.futureValue shouldBe an[IllegalArgumentException]
    }
    "failed in processing events" in {
      val calls = evaluatedCallFactGen().next(5)
      val events = CampaignEventsGen.next(3)

      val statisticsUpdateTask =
        task(Future.successful(calls), Future.failed(new IllegalStateException("some")), Future.successful(events))

      statisticsUpdateTask.execute(ConfigFactory.empty()).failed.futureValue shouldBe an[IllegalStateException]
    }
    "failed in aggregating events" in {
      val calls = evaluatedCallFactGen().next(5)
      val baggages = BaggageGen.next(5)

      val statisticsUpdateTask =
        task(Future.successful(calls), Future.successful(baggages), Future.failed(new RuntimeException("some")))

      statisticsUpdateTask.execute(ConfigFactory.empty()).failed.futureValue shouldBe an[RuntimeException]
    }
  }
}
