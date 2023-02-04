package ru.yandex.vertis.billing.tasks

import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.mockito.Mockito.{times, verify}
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.DetailsOperator.RawEventDetailsOperator
import ru.yandex.vertis.billing.async.{ActorSystemSpecBase, AsyncSpecBase}
import ru.yandex.vertis.billing.dao.CampaignEventDao
import ru.yandex.vertis.billing.event.CampaignEventsWithBaggageAggregator
import ru.yandex.vertis.billing.event.failures.DummyTryHandler
import ru.yandex.vertis.billing.event.indexing.IndexingEventsService
import ru.yandex.vertis.billing.model_core.EventTypes
import ru.yandex.vertis.billing.model_core.gens._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future
import scala.util.Success

class IndexingToCampaignEventsTaskSpec
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with ActorSystemSpecBase
  with AsyncSpecBase {

  private val aggregator = new CampaignEventsWithBaggageAggregator(
    EventTypes.IndexingRevenue,
    CampaignEventsWithBaggageAggregator.sum,
    RawEventDetailsOperator
  )

  "IndexingToCampaignEventsTask" should {
    "correctly aggregate events" in {
      val baggage1 = BaggageGen.next.copy(eventType = EventTypes.IndexingRevenue)
      val baggage2 = BaggageGen.next.copy(eventType = EventTypes.IndexingRevenue)

      val mockCampaignEventDao = mock[CampaignEventDao]
      when(mockCampaignEventDao.write(?, ?)).thenReturn(Future.unit)

      val mockIndexingEventsService = mock[IndexingEventsService]
      val baggagesIterator = Seq(baggage1, baggage2).map(Success.apply).iterator
      val baggagesSource = Source.fromIterator(() => baggagesIterator)
      when(mockIndexingEventsService.streamSortedByCampaign(?)).thenReturn(baggagesSource)

      val task = new IndexingToCampaignEventsTask(
        campaignEventDao = mockCampaignEventDao,
        indexingEventsService = mockIndexingEventsService,
        aggregator = aggregator,
        meteringTryHandler = new DummyTryHandler(),
        serviceName = "autoru"
      )

      task.execute(ConfigFactory.empty()).futureValue

      verify(mockCampaignEventDao, times(2)).write(?, ?)
    }
  }

  override protected def name: String = "IndexingToCampaignEventsTaskSpec"
}
