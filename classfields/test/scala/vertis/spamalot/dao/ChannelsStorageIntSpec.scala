package vertis.spamalot.dao

import org.scalacheck.magnolia._
import org.scalactic.TypeCheckedTripleEquals
import ru.yandex.vertis.ydb.Ydb
import vertis.core.utils.NoWarnFilters
import vertis.spamalot.SpamalotYdbTest
import vertis.spamalot.dao.model.storage.StoredChannel
import vertis.spamalot.model.{ReceiverId, UserId}
import vertis.zio.test.ZioSpecBase

/** @author tymur-lysenko
  */
class ChannelsStorageIntSpec extends ZioSpecBase with SpamalotYdbTest with TypeCheckedTripleEquals {

  "ChannelsStorage" should {
    "get unread count" when {
      "receiver is user" in ydbTest {
        val userId = ReceiverId.User(random[UserId])
        val channel = random[StoredChannel].copy(protoReceiverId = userId.proto)
        for {
          _ <- Ydb.runTx(storages.channelStorage.upsert(channel)): @annotation.nowarn(NoWarnFilters.Deprecation)
          count <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
          _ <- check(count should contain(channel.unreadCount))
        } yield ()
      }

      "receiver is device" in ydbTest {
        val deviceId = ReceiverId.DeviceId(random[String])
        val channel = random[StoredChannel].copy(protoReceiverId = deviceId.proto)
        for {
          _ <- Ydb.runTx(storages.channelStorage.upsert(channel)): @annotation.nowarn(NoWarnFilters.Deprecation)
          count <- Ydb.runTx(storages.channelStorage.unreadCount(deviceId))
          _ <- check(count should contain(channel.unreadCount))
        } yield ()
      }
    }

    "delete unread count (for tests only)" when {
      "receiver is user" in ydbTest {
        val userId = ReceiverId.User(random[UserId])
        val channel = random[StoredChannel].copy(protoReceiverId = userId.proto)
        for {
          _ <- Ydb.runTx(storages.channelStorage.upsert(channel))
          countBeforeDeletion <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
          _ <- check(countBeforeDeletion should ===(Some(channel.unreadCount)))
          _ <- Ydb.runTx(storages.channelStorage.delete(userId))
          countAfterDeletion <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
          _ <- check(countAfterDeletion shouldBe empty)
        } yield ()
      }

      "receiver is device" in ydbTest {
        val deviceId = ReceiverId.DeviceId(random[String])
        val channel = random[StoredChannel].copy(protoReceiverId = deviceId.proto)
        for {
          _ <- Ydb.runTx(storages.channelStorage.upsert(channel))
          _ <- Ydb.runTx(storages.channelStorage.delete(deviceId))
          count <- Ydb.runTx(storages.channelStorage.unreadCount(deviceId))
          _ <- check(count shouldBe empty)
        } yield ()
      }
    }
  }
}
