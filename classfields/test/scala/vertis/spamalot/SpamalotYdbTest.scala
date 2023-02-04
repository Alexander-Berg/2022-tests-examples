package vertis.spamalot

import common.zio.events_broker.Broker
import common.zio.events_broker.Broker.{BrokerError, BrokerEvent}
import org.scalatest.Assertion
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.ydb.Ydb
import ru.yandex.vertis.ydb.zio.{Tx, YdbZioWrapper}
import vertis.SpamalotArbitraryTest
import vertis.core.utils.NoWarnFilters
import vertis.spamalot.config.SpamalotDomainConfig
import vertis.spamalot.dao._
import vertis.spamalot.dao.user.ReceiverSettingsServiceImpl
import vertis.spamalot.model.{ReceiverId, UserId}
import vertis.ydb.partitioning.manual.{ManualPartition, ManualPartitionHashing}
import vertis.ydb.queue.storage.Storage
import vertis.ydb.test.YdbTest
import vertis.zio.test.ZioSpecBase
import zio.clock.Clock
import zio.{Has, IO, RIO, ULayer, ZIO}

import scala.annotation.nowarn

/** A base for ydb integration tests in spamalot
  * @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait SpamalotYdbTest extends YdbTest with SpamalotArbitraryTest {
  this: org.scalatest.Suite with ZioSpecBase =>

  import SpamalotYdbTest.SpamalotTestStorages

  private val domain = Domain.DOMAIN_AUTO
  protected val domainConfig = SpamalotDomainConfig(domain, 2)

  protected val testBrokerService = new Broker.Service {

    override def send[T: BrokerEvent](
        event: T,
        id: Option[String] = None,
        schemaVersion: Option[String] = None): IO[BrokerError, Unit] = ZIO.unit
  }

  protected def getPartition(receiverId: ReceiverId): ManualPartition = {
    val stringToHash = receiverId match {
      case ReceiverId.User(UserId(userId)) => s"userId:$userId"
      case ReceiverId.DeviceId(deviceId) => s"deviceId:$deviceId"
    }

    domainConfig.queuePartitioning.getByHash(ManualPartitionHashing.getHash(stringToHash))
  }

  protected lazy val storages =
    SpamalotTestStorages(
      new OldChannelsStorage(ydbWrapper, prometheusRegistry) with OldChannelsStorageTestMixin,
      new ChannelsStorage(ydbWrapper, prometheusRegistry) with ChannelsStorageTestMixin,
      new OldNotificationsStorage(ydbWrapper, prometheusRegistry) with OldNotificationStorageTestMixin,
      new NotificationsStorage(ydbWrapper, prometheusRegistry),
      new OperationsQueueStorage(ydbWrapper, prometheusRegistry, domainConfig.queuePartitioning),
      new PayloadsStorage(ydbWrapper, prometheusRegistry),
      new UserConfigStorage(ydbWrapper, prometheusRegistry),
      new ReceiverConfigStorage(ydbWrapper, prometheusRegistry) with ReceiverConfigStorageTestMixin,
      new PushHistoryStorage(ydbWrapper, prometheusRegistry) with PushHistoryStorageTestMixin
    ): @nowarn(NoWarnFilters.Deprecation)

  protected lazy val receiverSettingsService = new ReceiverSettingsServiceImpl(storages)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    runSync(ZIO.foreachPar(storages.all)(_.init))
    ()
  }

  protected def runTx[R <: Clock, E, T](tx: Tx[R with Ydb, E, T]): ZIO[R, E, T] =
    Ydb.runTx(tx).provideSomeLayer[R](storages.ydbLayer) // todo: new table

  protected def checkTx[R <: Clock, E <: Throwable](clue: String)(tx: Tx[R with Ydb, E, Assertion]): RIO[R, Assertion] =
    checkM[R, E](clue)(runTx[R, E, Assertion](tx))
}

object SpamalotYdbTest {

  @nowarn(NoWarnFilters.Deprecation)
  case class SpamalotTestStorages(
      oldChannelStorage: OldChannelsStorage with OldChannelsStorageTestMixin,
      channelStorage: ChannelsStorage with ChannelsStorageTestMixin,
      oldNotificationStorage: OldNotificationsStorage with OldNotificationStorageTestMixin,
      notificationStorage: NotificationsStorage,
      operationStorage: OperationsQueueStorage,
      payloadStorage: PayloadsStorage,
      userConfigStorage: UserConfigStorage,
      receiverConfigStorage: ReceiverConfigStorage with ReceiverConfigStorageTestMixin,
      pushHistoryStorage: PushHistoryStorage with PushHistoryStorageTestMixin) {

    lazy val ydbLayer: ULayer[Has[YdbZioWrapper]] = notificationStorage.ydbLayer

    def all: Seq[Storage] =
      List(
        oldChannelStorage,
        channelStorage,
        oldNotificationStorage,
        notificationStorage,
        operationStorage,
        payloadStorage,
        userConfigStorage,
        receiverConfigStorage,
        pushHistoryStorage
      )
  }

  implicit def testStoragesToRegular(testStorages: SpamalotTestStorages): SpamalotStorages =
    SpamalotStorages(
      testStorages.oldChannelStorage,
      testStorages.channelStorage,
      testStorages.oldNotificationStorage,
      testStorages.notificationStorage,
      testStorages.operationStorage,
      testStorages.payloadStorage,
      testStorages.userConfigStorage,
      testStorages.receiverConfigStorage,
      testStorages.pushHistoryStorage
    )
}
