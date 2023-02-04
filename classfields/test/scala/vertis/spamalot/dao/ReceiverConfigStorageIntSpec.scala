package vertis.spamalot.dao

import ru.yandex.vertis.spamalot.model.ReceiverThrottleConfiguration
import ru.yandex.vertis.ydb.Ydb
import vertis.spamalot.SpamalotYdbTest
import vertis.spamalot.model.{ReceiverId, UserId}
import vertis.zio.test.ZioSpecBase
import org.scalacheck.magnolia._
import vertis.spamalot.model.ReceiverId.DeviceId

class ReceiverConfigStorageIntSpec extends ZioSpecBase with SpamalotYdbTest {

  private def randomConfig = random[ReceiverThrottleConfiguration]
    .copy(bannedTopics = Seq("topic1"))

  "ReceiverConfigStorage" should {
    "get and update config" when {
      "receiver is user" in ydbTest {
        val receiver = random[ReceiverId.User]
        val config = randomConfig
        for {
          _ <- checkM("no config") {
            Ydb
              .runTx(storages.receiverConfigStorage.get(receiver))
              .map(_ shouldBe empty)
          }
          _ <- Ydb.runTx(storages.receiverConfigStorage.update(receiver, config))
          _ <- checkM("update:") {
            Ydb
              .runTx(storages.receiverConfigStorage.get(receiver))
              .map(_.get shouldBe config)
          }
        } yield ()
      }
      "receiver is device" in ydbTest {
        val receiver: ReceiverId = random[DeviceId]
        val config = random[ReceiverThrottleConfiguration]
          .copy(bannedTopics = Seq("topic1"))
        for {
          _ <- checkM("no config") {
            Ydb
              .runTx(storages.receiverConfigStorage.get(receiver))
              .map(_ shouldBe empty)
          }
          _ <- Ydb.runTx(storages.receiverConfigStorage.update(receiver, config))
          _ <- checkM("update:") {
            Ydb
              .runTx(storages.receiverConfigStorage.get(receiver))
              .map(_.get shouldBe config)
          }
        } yield ()
      }
    }

    "delete config" when {
      "receiver is user" in ydbTest {
        val receiver = ReceiverId.User(random[UserId])
        val config = random[ReceiverThrottleConfiguration]
          .copy(bannedTopics = Seq("topic1"))
        for {
          _ <- Ydb.runTx(storages.receiverConfigStorage.update(receiver, config))
          nonEmpty <- Ydb.runTx(storages.receiverConfigStorage.get(receiver))
          _ <- check(nonEmpty should not be empty)
          _ <- Ydb.runTx(storages.receiverConfigStorage.delete(receiver))
          emptyConf <- Ydb.runTx(storages.receiverConfigStorage.get(receiver))
          _ <- check(emptyConf shouldBe empty)
        } yield ()
      }

      "receiver is device" in ydbTest {
        val receiver: ReceiverId = random[DeviceId]
        val config = random[ReceiverThrottleConfiguration]
          .copy(bannedTopics = Seq("topic1"))
        for {
          _ <- Ydb.runTx(storages.receiverConfigStorage.update(receiver, config))
          nonEmpty <- Ydb.runTx(storages.receiverConfigStorage.get(receiver))
          _ <- check(nonEmpty should not be empty)
          _ <- Ydb.runTx(storages.receiverConfigStorage.delete(receiver))
          emptyConf <- Ydb.runTx(storages.receiverConfigStorage.get(receiver))
          _ <- check(emptyConf shouldBe empty)
        } yield ()
      }
    }
  }
}
