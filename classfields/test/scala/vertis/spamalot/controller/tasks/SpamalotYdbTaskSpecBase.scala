package vertis.spamalot.controller.tasks

import cats.syntax.option._
import common.zio.token_distributor.TokenDistributor
import ru.yandex.common.tokenization.NonBlockingTokensFilter
import ru.yandex.vertis.common.Domain.DOMAIN_AUTO
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.spamalot.inner.OperationPayload
import ru.yandex.vertis.ydb.Ydb
import vertis.spamalot.controller.SpamalotControllerDomainComponents
import vertis.spamalot.dao.OperationsQueueStorage
import vertis.spamalot.dao.model.Pagination
import vertis.spamalot.dao.queue.partitioning.ReceiverIdHashing
import vertistraf.common.pushnoy.client.render.PushRendererImpl
import vertis.spamalot.external.pushnoy.service.{PayloadServiceImpl, PushServiceImpl}
import vertis.spamalot.inner.StoredNotificationValidators
import vertis.spamalot.mocks.TestSendingTimeService
import vertis.spamalot.model.{ReceiverId, UserId}
import vertistraf.common.pushnoy.client.mocks.{TestPushnoyClient, TestTopicNameResolver}
import vertis.spamalot.services.SubscriptionService
import vertis.spamalot.services.impl.ReceiverNotificationServiceImpl
import vertis.spamalot.{SpamalotCoreDomainComponents, SpamalotYdbTest, TestImagesConfig}
import vertis.ydb.partitioning.manual.ManualPartition
import vertis.ydb.partitioning.manual.ManualPartitionHashing.QueuePartitionHash
import vertis.zio.BaseEnv
import vertis.zio.test.ZioSpecBase
import zio.{RIO, ZIO}

import java.time.Instant

/** @author Ratskevich Natalia reimai@yandex-team.ru
 */
trait SpamalotYdbTaskSpecBase extends SpamalotYdbTest with TestImagesConfig with MockitoSupport {
  this: org.scalatest.Suite with ZioSpecBase =>

  import OperationsQueueStorage.OperationPayloadCodec

  protected lazy val testPushClient = new TestPushnoyClient(domainConfig.domain)

  protected lazy val allPartitions = domainConfig.queuePartitioning.allPartitions.toSet

  protected lazy val topicNameResolver = new TestTopicNameResolver

  protected lazy val sendingTimeService = TestSendingTimeService

  protected lazy val subscriptionService: SubscriptionService.Service = (_: String) => ZIO.succeed(None)

  private val tokensFilter = mock[NonBlockingTokensFilter]
  private val distributor = mock[TokenDistributor.Service]
  when(distributor.tokensFilter).thenReturn(ZIO.succeed(tokensFilter))
  when(tokensFilter.isAcceptable(?)).thenReturn(true)

  protected lazy val components =
    SpamalotControllerDomainComponents(
      SpamalotCoreDomainComponents(
        domainConfig.domain,
        storages,
        new ReceiverNotificationServiceImpl(
          DOMAIN_AUTO,
          storages,
          receiverSettingsService,
          testBrokerService,
          testPushClient,
          sendingTimeService,
          StoredNotificationValidators.default
        ),
        receiverSettingsService,
        new PushServiceImpl(
          testPushClient,
          new PushRendererImpl(imagesConfig, topicNameResolver),
          subscriptionService,
          storages.pushHistoryStorage,
          storages.ydbLayer
        ),
        new PayloadServiceImpl(imagesConfig),
        topicNameResolver,
        testPushClient
      ),
      distributor,
      domainConfig.queuePartitioning
    )

  protected def checkFullState(
      receiverId: ReceiverId,
      expectedNotificationsToAdd: Int = 0,
      expectedNotifications: Int = 0,
      expectedUnread: Int = 0,
      expectedPushes: Int = 0) =
    checkState(
      receiverId,
      Some(expectedNotificationsToAdd),
      Some(expectedNotifications),
      Some(expectedUnread),
      Some(expectedPushes)
    )

  protected def checkState(
      receiverId: ReceiverId,
      expectedNotificationsToAdd: Option[Int] = None,
      expectedNotifications: Option[Int] = None,
      expectedUnread: Option[Int] = None,
      expectedPushes: Option[Int] = None) =
    for {
      now <- zio.clock.currentDateTime.map(_.toInstant)
      partition = getPartition(receiverId)
      _ <- ZIO.foreach_(expectedNotificationsToAdd)(checkNotificationsScheduled(now, partition, _))
      _ <- ZIO.foreach_(expectedNotifications)(checkNotifications(receiverId, _))
      _ <- ZIO.foreach_(expectedUnread)(checkUnread(receiverId, _))
      _ <- ZIO.foreach_(expectedPushes)(checkPushesScheduled(now, partition, _))
    } yield ()

  protected def checkNotificationsScheduled(
      now: Instant,
      partition: ManualPartition,
      expectedNotificationsToAdd: Int) =
    checkTx(s"Have $expectedNotificationsToAdd operations left") {
      storages.operationStorage
        .countElements(now, partition, "AddNotification", 1000)
        .map(addOperations => addOperations should be(expectedNotificationsToAdd))
    }

  protected def checkNotifications(receiverId: ReceiverId, expectedNotifications: Int) =
    checkTx(s"Got $expectedNotifications notifications") {
      // New tests should insert notifications only in new table, so only checking the new table should be fine
      storages.notificationStorage
        .list(receiverId, newOnly = true, pagination = Pagination.default)
        .map(p => (p.items should have).length(expectedNotifications.toLong))
    }

  protected def checkUnread(receiverId: ReceiverId, expectedUnread: Int) =
    checkTx(s"Got $expectedUnread unread in channel") {
      storages.channelStorage
        .unreadCount(receiverId)
        .map(_.getOrElse(0))
        .map(unread => unread should be(expectedUnread))
    }

  protected def checkPushesScheduled(
      now: Instant,
      partition: ManualPartition,
      expectedPushes: Int) =
    checkTx(s"Got $expectedPushes push operations") {
      storages.operationStorage
        .countElements(now, partition, "SendPush", 1000)
        .map(scheduledPushes => scheduledPushes should be(expectedPushes))
    }

  private def totalPushes(receiverId: ReceiverId, expectedPushes: Int) =
    for {
      id <- ZIO.effectSuspendTotal(receiverId match {
        case ReceiverId.User(UserId(userId)) =>
          for {
            devices <- testPushClient.getUserDevices(userId)
            firstDevice <- ZIO
              .fromOption(devices.headOption)
              .orElseFail(new IllegalArgumentException("User did not have any devices in mocked pushnoy client"))
          } yield firstDevice.device.id
        case ReceiverId.DeviceId(deviceId) => ZIO.succeed(deviceId)
      })
      totalPushes <- testPushClient
        .totalPushes(id)
        .map(pushes => {
          println(s"Expected: $expectedPushes, got $pushes")
          pushes should be(expectedPushes)
        })
    } yield totalPushes

  protected def checkPushesSend(receiverId: ReceiverId, expectedPushes: Int) =
    checkM(s"Send $expectedPushes pushes to $receiverId") {
      totalPushes(receiverId, expectedPushes)
    }

  protected def addOperations(receiverId: ReceiverId, operations: Seq[OperationPayload]): RIO[BaseEnv, Unit] = {
    val partition = components.storages.operationStorage.partitioning.getByHash(getReceiverIdHash(receiverId))
    val elements = operations.map(components.storages.operationStorage.toQueueElement)
    Ydb
      .runTx(components.storages.operationStorage.addElements[OperationPayload](partition, elements))
      .provideSomeLayer[BaseEnv](ydbLayer)
      .unit
  }

  private def getReceiverIdHash(receiverId: ReceiverId): QueuePartitionHash =
    ReceiverIdHashing.getHash(receiverId.proto.some, "")
}
