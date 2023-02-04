package ru.yandex.vertis.billing.tasks

import com.typesafe.config.ConfigFactory
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.CampaignHistoryDao
import ru.yandex.vertis.billing.dao.CampaignHistoryDao.{CampaignHistoryPoint, EventTypes}
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.dao.gens.{campaignStateChangeGen, CampaignStateChangeGenParams}
import ru.yandex.vertis.billing.service.async.AsyncCampaignService
import ru.yandex.vertis.billing.util.{AutomatedContext, DateTimeUtils}
import ru.yandex.vertis.billing.util.mock.{AsyncCampaignServiceMockBuilder, CampaignHistoryDaoMockBuilder}
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * @author tolmach
  */
class SetCorrectCampaignHistoryEventTypeTaskSpec
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with AsyncSpecBase {

  implicit val requestContext = AutomatedContext("SetCorrectCampaignHistoryEventTypeTask")

  private def prepareCampaignHistoryDao(
      historyPoints: Seq[CampaignHistoryPoint],
      batchSize: Int): (CampaignHistoryDao, AsyncCampaignService) = {

    val batches = historyPoints.grouped(batchSize)
    val raw = batches.map { batch =>
      val withoutEventType = batch.map { point =>
        point.copy(eventType = EventTypes.UndefinedType)
      }
      (withoutEventType, batch)
    }
    val (withoutEventType, withEventType) = raw.toSeq.unzip
    val zeroHistoryService = CampaignHistoryDaoMockBuilder()
    val historyServiceWithGet =
      zeroHistoryService.withGetWithEventTypeBatched(EventTypes.UndefinedType, withoutEventType, batchSize)
    val completedHistoryService = withEventType.foldLeft(historyServiceWithGet) { case (acc, withEventType) =>
      acc.withSwapTypeBatch(withEventType)
    }
    val pointsForCampaigns = withEventType.flatten.groupBy(_.header.id)
    val campaigns = pointsForCampaigns.values.map { points =>
      val creation = points.find(_.eventType == EventTypes.Create)
      val shift = creation match {
        case Some(_) =>
          0
        case None =>
          val shiftGen = Gen.choose(1, 10000)
          shiftGen.next
      }
      val point = points.head
      val epoch = point.header.epoch.get
      val createTime = DateTimeUtils.fromMillis(epoch - shift)
      point.header.copy(createTimestamp = Some(createTime))
    }
    val completedCampaignService = AsyncCampaignServiceMockBuilder().withGetForCampaigns(campaigns.toSeq)
    (completedHistoryService.build, completedCampaignService.build)
  }

  private def test(historyPoints: Seq[CampaignHistoryPoint], batchSize: Int): Unit = {
    val (campaignHistoryDao, asyncCampaignService) = prepareCampaignHistoryDao(historyPoints, batchSize)
    val task = new SetCorrectCampaignHistoryEventTypeTask(campaignHistoryDao, asyncCampaignService)
    task.execute(ConfigFactory.empty()).futureValue
  }

  "SetCorrectCampaignHistoryEventTypeTask" should {
    "complete with success" when {
      "pass correct data" in {
        val historyPointsGroupsCount = 10
        val maxPerGroup = 100
        val params = CampaignStateChangeGenParams(maxCount = Some(maxPerGroup)).withoutCreateTimestamp
        val historyPoints = campaignStateChangeGen(params)
          .next(historyPointsGroupsCount)
          .flatten
          .toSeq
        test(historyPoints, SetCorrectCampaignHistoryEventTypeTask.BatchSize)
      }
    }
  }

}
