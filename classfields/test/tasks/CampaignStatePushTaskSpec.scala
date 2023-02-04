package ru.yandex.vertis.billing.tasks

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.BillingEvent.CommonBillingInfo
import ru.yandex.vertis.billing.{BillingEvent, SupportedServices}
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.CampaignHistoryDao.CampaignHistoryPoint
import ru.yandex.vertis.billing.service.delivery.MessageDeliveryService
import ru.yandex.vertis.billing.service.TypedKeyValueService
import ru.yandex.vertis.billing.service.async.{AsyncArchiveService, AsyncCampaignHistoryService}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.billing.dao.gens.{archiveRecordGen, campaignStateChangeGen, ArchiveRecordGenParams}
import ru.yandex.vertis.billing.model_core.{Epoch, EpochWithTypedId}
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.service.ArchiveService.{ArchiveRecord, RecordTypes}
import ru.yandex.vertis.billing.tasks.CampaignStatePushTaskSpec.{
  PreparedCampaignArchiveChanges,
  PreparedCampaignCreateUpdateChanges
}
import ru.yandex.vertis.billing.util.{AutomatedContext, WithBeforeAndAfterEpochWithTypedId}
import ru.yandex.vertis.billing.dao.{Conversions => DaoConversions}
import ru.yandex.vertis.billing.util.mock.{
  AsyncArchiveServiceMockBuilder,
  AsyncCampaignHistoryServiceMockBuilder,
  MessageDeliveryServiceMockBuilder,
  TypedKeyValueServiceMockBuilder
}

/**
  * @author tolmach
  */
class CampaignStatePushTaskSpec extends AnyWordSpec with Matchers with MockitoSupport with AsyncSpecBase {

  implicit val requestContext = AutomatedContext("CampaignStatePushTask")

  private def prepareTypedKeyValueServiceMockBuilder(
      builder: TypedKeyValueServiceMockBuilder,
      key: String,
      w: Seq[WithBeforeAndAfterEpochWithTypedId[String]]): TypedKeyValueServiceMockBuilder = {
    w.headOption match {
      case Some(head) =>
        val first = builder.withGetMock(key, head.before).withSetMock(key, head.after)
        w.tail.foldLeft(first) { case (acc, p) =>
          acc.withSetMock(key, p.after)
        }
      case None =>
        builder
    }
  }

  private def prepareMessageDeliveryServiceMockBuilder(
      builder: MessageDeliveryServiceMockBuilder,
      historyBatches: Seq[Seq[CampaignHistoryPoint]],
      archiveBatches: Seq[Seq[ArchiveRecord]],
      protoBillingDomain: CommonBillingInfo.BillingDomain): MessageDeliveryServiceMockBuilder = {

    val historyMessageBatches = historyBatches.map { batch =>
      batch.map { state =>
        DaoConversions.toCampaignCreateUpdateStateEvent(state, protoBillingDomain)
      }
    }
    val archiveMessageBatches = archiveBatches.map { batch =>
      batch.map { state =>
        DaoConversions.toCampaignCreateUpdateStateEvent(state, protoBillingDomain)
      }
    }

    val messageBatches = historyMessageBatches ++ archiveMessageBatches
    messageBatches.foldLeft(builder) { case (acc, batch) =>
      acc.withSendBatch(batch)
    }
  }

  private def mockCampaignHistoryService(
      preparedCampaignCreateUpdateChanges: Seq[PreparedCampaignCreateUpdateChanges],
      batchSize: Int): AsyncCampaignHistoryService = {
    val zero = AsyncCampaignHistoryServiceMockBuilder()
    val prepared = preparedCampaignCreateUpdateChanges.foldLeft(zero) { case (acc, batch) =>
      acc.withGetUpdateSinceBatchOrderedMock(batch.before.epoch, batch.before.id, batch.changesBatch, batchSize)
    }
    val completed = preparedCampaignCreateUpdateChanges.lastOption match {
      case Some(last) if last.changesBatch.size == batchSize =>
        prepared.withGetUpdateSinceBatchOrderedMock(last.after.epoch, last.after.id, Seq.empty, batchSize)
      case _ =>
        prepared
    }
    completed.build
  }

  private def mockArchiveService(
      preparedCampaignArchiveChanges: Seq[PreparedCampaignArchiveChanges],
      batchSize: Int): AsyncArchiveService = {
    val zero = AsyncArchiveServiceMockBuilder()
    val prepared = preparedCampaignArchiveChanges.foldLeft(zero) { case (acc, batch) =>
      acc.withGetCampaignUpdatedSinceBatchOrdered(batch.before.epoch, batch.before.id, batch.changesBatch, batchSize)
    }
    val completed = preparedCampaignArchiveChanges.lastOption match {
      case Some(last) if last.changesBatch.size == batchSize =>
        prepared.withGetCampaignUpdatedSinceBatchOrdered(last.after.epoch, last.after.id, Seq.empty, batchSize)
      case _ =>
        prepared
    }
    completed.build
  }

  private def mockKeyValueService(
      preparedCampaignCreateUpdateChanges: Seq[PreparedCampaignCreateUpdateChanges],
      preparedCampaignArchiveChanges: Seq[PreparedCampaignArchiveChanges]): TypedKeyValueService = {
    val typedKeyValueServiceMockBuilder = TypedKeyValueServiceMockBuilder()

    val data = Seq(
      CampaignStatePushTask.HistoryMarker -> preparedCampaignCreateUpdateChanges,
      CampaignStatePushTask.ArchiveMarker -> preparedCampaignArchiveChanges
    )

    val completed = data.foldLeft(typedKeyValueServiceMockBuilder) { case (acc, (marker, epoches)) =>
      prepareTypedKeyValueServiceMockBuilder(acc, marker, epoches)
    }

    completed.build
  }

  private def mockMessageDeliveryService(
      preparedCampaignCreateUpdateChanges: Seq[PreparedCampaignCreateUpdateChanges],
      preparedCampaignArchiveChanges: Seq[PreparedCampaignArchiveChanges],
      protoBillingDomain: BillingEvent.CommonBillingInfo.BillingDomain): MessageDeliveryService = {
    val gropedHistoryPoints = preparedCampaignCreateUpdateChanges.map(_.changesBatch)
    val gropedArchiveRecords = preparedCampaignArchiveChanges.map(_.changesBatch)
    val messageDeliveryServiceMockBuilder = prepareMessageDeliveryServiceMockBuilder(
      MessageDeliveryServiceMockBuilder(),
      gropedHistoryPoints,
      gropedArchiveRecords,
      protoBillingDomain
    )
    messageDeliveryServiceMockBuilder.build
  }

  private def test(
      historyPoints: Seq[CampaignHistoryPoint],
      historyPointsStartEpoch: Epoch,
      archiveRecords: Seq[ArchiveRecord],
      archiveRecordsStartEpoch: Epoch,
      domain: String): Unit = {
    val protoBillingDomain = SupportedServices.toBillingDomain(domain)

    val preparedCampaignCreateUpdateChanges =
      PreparedCampaignCreateUpdateChanges.from(historyPointsStartEpoch, CampaignStatePushTask.BatchSize, historyPoints)

    val preparedCampaignArchiveChanges =
      PreparedCampaignArchiveChanges.from(archiveRecordsStartEpoch, CampaignStatePushTask.BatchSize, archiveRecords)

    val asyncCampaignHistoryService = mockCampaignHistoryService(
      preparedCampaignCreateUpdateChanges,
      CampaignStatePushTask.BatchSize
    )

    val typedKeyValueService = mockKeyValueService(
      preparedCampaignCreateUpdateChanges,
      preparedCampaignArchiveChanges
    )

    val asyncArchiveService = mockArchiveService(
      preparedCampaignArchiveChanges,
      CampaignStatePushTask.BatchSize
    )

    val messageDeliveryService = mockMessageDeliveryService(
      preparedCampaignCreateUpdateChanges,
      preparedCampaignArchiveChanges,
      protoBillingDomain
    )

    val task = new CampaignStatePushTask(
      asyncCampaignHistoryService,
      asyncArchiveService,
      messageDeliveryService,
      typedKeyValueService,
      domain
    )
    task.execute(ConfigFactory.empty()).futureValue
  }

  "CampaignStatePushTask" should {
    "complete with success" when {
      "correct data was passed" in {
        val historyPoints = campaignStateChangeGen().next(10).flatten.toSeq
        val archiveParams = ArchiveRecordGenParams().withRecordType(RecordTypes.Campaign).withoutCampaignEpoch
        val archiveRecords = archiveRecordGen(archiveParams).next(1000).toSeq
        test(historyPoints, 0L, archiveRecords, 0L, "autoru")
      }
    }
  }
}

object CampaignStatePushTaskSpec {

  case class PreparedCampaignCreateUpdateChanges(
      before: EpochWithTypedId[String],
      changesBatch: Seq[CampaignHistoryPoint],
      after: EpochWithTypedId[String])
    extends WithBeforeAndAfterEpochWithTypedId[String]

  object PreparedCampaignCreateUpdateChanges {

    def from(
        startEpoch: Epoch,
        batchSize: Int,
        stateChanges: Seq[CampaignHistoryPoint]): Seq[PreparedCampaignCreateUpdateChanges] = {
      val sorted = stateChanges.sortBy(p => (p.header.epoch, p.header.id))
      val sortedGrouped = sorted.grouped(batchSize)
      val first = EpochWithTypedId[String](startEpoch, None)
      val seq = Seq.empty[PreparedCampaignCreateUpdateChanges]
      val (_, points) = sortedGrouped.foldLeft((first, seq)) { case ((before, seq), group) =>
        val lastEpochWithId = EpochWithTypedId[String](group.last.header.epoch.get, Some(group.last.header.id))
        val cur = PreparedCampaignCreateUpdateChanges(before, group, lastEpochWithId)
        (lastEpochWithId, seq :+ cur)
      }
      points
    }

  }

  case class PreparedCampaignArchiveChanges(
      before: EpochWithTypedId[String],
      changesBatch: Seq[ArchiveRecord],
      after: EpochWithTypedId[String])
    extends WithBeforeAndAfterEpochWithTypedId[String]

  object PreparedCampaignArchiveChanges {

    def from(
        startEpoch: Epoch,
        batchSize: Int,
        stateChanges: Seq[ArchiveRecord]): Seq[PreparedCampaignArchiveChanges] = {
      val sorted = stateChanges.sortBy(p => (p.epoch, p.id))
      val sortedGrouped = sorted.grouped(batchSize)
      val first = EpochWithTypedId[String](startEpoch, None)
      val seq = Seq.empty[PreparedCampaignArchiveChanges]
      val (_, points) = sortedGrouped.foldLeft((first, seq)) { case ((before, seq), group) =>
        val lastEpochWithId = EpochWithTypedId[String](group.last.epoch.get, Some(group.last.id))
        val cur = PreparedCampaignArchiveChanges(before, group, lastEpochWithId)
        (lastEpochWithId, seq :+ cur)
      }
      points
    }

  }

}
