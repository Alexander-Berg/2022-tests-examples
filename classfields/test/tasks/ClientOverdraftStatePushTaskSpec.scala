package ru.yandex.vertis.billing.tasks

import com.typesafe.config.ConfigFactory
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.SupportedServices
import ru.yandex.vertis.billing.async.AsyncSpecBase
import ru.yandex.vertis.billing.dao.NotifyClientDao.ClientIdWithTid
import ru.yandex.vertis.billing.service.delivery.MessageDeliveryService
import ru.yandex.vertis.billing.model_core.{EpochWithTypedId, NotifyClient, NotifyClientRecordId}
import ru.yandex.vertis.billing.service.TypedKeyValueService
import ru.yandex.vertis.billing.service.async.AsyncNotifyClientService
import ru.yandex.vertis.billing.util.{AutomatedContext, WithBeforeAndAfterEpochWithTypedId}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.billing.model_core.gens.{notifyClientGen, EpochGen, NotifyClientGenParams, Producer}
import ru.yandex.vertis.billing.BillingEvent.CommonBillingInfo
import ru.yandex.vertis.billing.dao.Conversions
import ru.yandex.vertis.billing.util.mock.{
  AsyncNotifyClientServiceMockBuilder,
  MessageDeliveryServiceMockBuilder,
  TypedKeyValueServiceMockBuilder
}

/**
  * @author tolmach
  */
class ClientOverdraftStatePushTaskSpec extends AnyWordSpec with Matchers with MockitoSupport with AsyncSpecBase {

  implicit val requestContext = AutomatedContext("ClientOverdraftStatePushTaskSpec")

  private val TestDomain = "autoru"

  private def prepareAsyncNotifyClientService(dataParts: Seq[DataPart], batchSize: Int): AsyncNotifyClientService = {
    val zero = AsyncNotifyClientServiceMockBuilder()
    val builderWithInitPart = dataParts.foldLeft(zero) { case (builder, part) =>
      val batchByClientId = part.batch.groupBy(_.clientId)
      val clientIdsWithTid = batchByClientId.values.map { clientNotifications =>
        val first = clientNotifications.head
        ClientIdWithTid(first.clientId, first.tid)
      }
      builder
        .withGetUpdatedSinceBatchOrdered(part.before.epoch, part.before.id, part.batch, batchSize)
        .withGetLastBeforeForClientsByTid(clientIdsWithTid.toSeq, part.previous)
    }
    val builder = dataParts.lastOption match {
      case Some(last) if last.batch.size == batchSize =>
        builderWithInitPart.withGetUpdatedSinceBatchOrdered(last.after.epoch, last.after.id, Seq.empty, batchSize)
      case _ =>
        builderWithInitPart
    }
    builder.build
  }

  private def prepareMessageDeliveryService(
      dataParts: Seq[DataPart],
      protoBillingDomain: CommonBillingInfo.BillingDomain): MessageDeliveryService = {
    val zero = MessageDeliveryServiceMockBuilder()
    val builder = dataParts.foldLeft(zero) { case (acc, part) =>
      val batchGroupedByClient = part.batch.groupBy(_.clientId)
      val previousClientMap = part.previous.groupBy(_.clientId).view.mapValues(_.head).toMap
      val batch = batchGroupedByClient.flatMap { case (clientId, clientNotifications) =>
        val previous = previousClientMap.get(clientId)
        val current = clientNotifications.head
        val first = Conversions.toClientOverdraftInfoChangeEvent(
          current,
          previous,
          protoBillingDomain
        )
        val rest = if (clientNotifications.size > 1) {
          val clientNotificationPairs = clientNotifications.sliding(2).toSeq
          clientNotificationPairs.map { notificationsPair =>
            val previous = notificationsPair.head
            val current = notificationsPair.tail.head
            Conversions.toClientOverdraftInfoChangeEvent(
              current,
              Some(previous),
              protoBillingDomain
            )
          }
        } else {
          Seq.empty
        }
        first +: rest
      }
      acc.withSendBatch(batch.toSeq)
    }
    builder.build
  }

  private def prepareTypedKeyValueService(dataParts: Seq[DataPart], marker: String): TypedKeyValueService = {
    val zero = TypedKeyValueServiceMockBuilder()

    val withGet = dataParts.headOption.foldLeft(zero) { case (builder, head) =>
      builder.withGetMock[EpochWithTypedId[NotifyClientRecordId]](marker, head.before)
    }
    val builder = dataParts.foldLeft(withGet) { case (builder, part) =>
      builder.withSetMock[EpochWithTypedId[NotifyClientRecordId]](marker, part.after)
    }

    builder.build
  }

  // notifications ordered by tid and epoch for same client in fetched batch
  private def genNotificationsSorted(clientCounts: Int, maxNotificationPerClient: Int): Seq[NotifyClient] = {
    val clientIds = (1 to clientCounts).map(_.toLong)
    val notificationsWithTidAndEpoch = clientIds.flatMap { clientId =>
      val count = Gen.choose(1, maxNotificationPerClient).next
      val notificationGen = notifyClientGen(NotifyClientGenParams().withClientId(clientId).withoutEpoch)
      val rawNotifications = notificationGen.next(count)
      val notificationsSortedByTid = rawNotifications.toSeq.sortBy(_.tid)
      val epoches = EpochGen.next(count)
      val sortedEpoches = epoches.toSeq.sorted
      val notificationsSortedByTidWithEpoch = notificationsSortedByTid.zip(sortedEpoches)
      notificationsSortedByTidWithEpoch.map { case (notificationWithTid, epoch) =>
        notificationWithTid.copy(epoch = Some(epoch))
      }
    }
    notificationsWithTidAndEpoch.sortBy(n => (n.epoch, n.recordId))
  }

  case class DataPart(
      batch: Seq[NotifyClient],
      previous: Seq[NotifyClient],
      before: EpochWithTypedId[NotifyClientRecordId],
      after: EpochWithTypedId[NotifyClientRecordId])
    extends WithBeforeAndAfterEpochWithTypedId[NotifyClientRecordId]

  case class State(previousId: EpochWithTypedId[Long], dataParts: Seq[DataPart])

  private def prepareDataParts(clientCounts: Int, maxNotificationPerClient: Int, batchSize: Int): Seq[DataPart] = {
    val notifications = genNotificationsSorted(clientCounts, maxNotificationPerClient)
    val splitPoint = notifications.size / 3
    val mainPart = notifications.drop(splitPoint)
    val batches = mainPart.grouped(batchSize)
    val notificationsMapSortedByTid = notifications.groupBy(_.clientId)
    val zero = State(EpochWithTypedId[Long](mainPart.head.epoch.get, None), Seq.empty[DataPart])
    val state = batches.foldLeft(zero) { case (acc, batch) =>
      val batchByClientId = batch.groupBy(_.clientId)
      val previous = batchByClientId.values.flatMap { clientNotifications =>
        val withoutPrevious = clientNotifications.head
        notificationsMapSortedByTid.get(withoutPrevious.clientId).flatMap { clientNotifications =>
          val lessThan = clientNotifications.filter(_.tid < withoutPrevious.tid)
          lessThan.lastOption
        }
      }
      val last = batch.last
      val lastId = EpochWithTypedId[Long](last.epoch.get, last.recordId)
      val part = DataPart(batch, previous.toSeq, acc.previousId, lastId)
      acc.copy(lastId, acc.dataParts :+ part)
    }
    state.dataParts
  }

  private def test(dataParts: Seq[DataPart], batchSize: Int, marker: String, domain: String): Unit = {
    val protoBillingDomain = SupportedServices.toBillingDomain(domain)

    val asyncNotifyClientService = prepareAsyncNotifyClientService(dataParts, batchSize)

    val messageDeliveryService = prepareMessageDeliveryService(dataParts, protoBillingDomain)

    val typedKeyValueService = prepareTypedKeyValueService(dataParts, marker)

    val task =
      new ClientOverdraftStatePushTask(asyncNotifyClientService, messageDeliveryService, typedKeyValueService, domain)

    task.execute(ConfigFactory.empty()).futureValue
  }

  "ClientOverdraftStatePushTask" should {
    "complete with success" when {
      "correct data was passed" in {
        val clientCounts = 100
        val maxNotificationPerClient = 50
        val dataParts =
          prepareDataParts(clientCounts, maxNotificationPerClient, ClientOverdraftStatePushTask.BatchSize)
        test(
          dataParts,
          ClientOverdraftStatePushTask.BatchSize,
          ClientOverdraftStatePushTask.ClientOverdraftStateMarker,
          TestDomain
        )
      }
    }
  }

}
