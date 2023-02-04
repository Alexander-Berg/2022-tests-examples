package ru.yandex.vertis.billing.tasks

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.gens._
import ru.yandex.vertis.billing.tasks.IndexingToCampaignEventsTask.CollectorByCampaignId

class CollectorByCampaignIdSpec extends AnyWordSpec with Matchers {

  "CollectorByCampaignId" should {
    "aggregates baggages with the same campaign id" in {
      val campaignHeader1 = CampaignHeaderGen.next
      val campaignHeader2 = CampaignHeaderGen.suchThat(_.id != campaignHeader1.id).next
      val baggagesFirstBatch = BaggageGen.next(3).map(_.copy(header = campaignHeader1))
      val baggage2 = BaggageGen.next.copy(header = campaignHeader2)
      val baggage3 = BaggageGen.next.copy(header = campaignHeader2)

      val collector = new CollectorByCampaignId()

      val result1 = baggagesFirstBatch.map(collector.collect)
      result1.flatten shouldBe empty

      val result2 = collector.collect(baggage2)
      result2 should contain theSameElementsAs baggagesFirstBatch

      val result3 = collector.collect(baggage3)
      result3 shouldBe empty

      val result4 = collector.flushBuffer()
      result4 should contain theSameElementsAs Iterable(baggage2, baggage3)
    }
  }
}
