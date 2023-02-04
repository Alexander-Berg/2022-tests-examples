package vertis.spamalot.dao

import ru.yandex.vertis.ydb.Ydb
import vertis.spamalot.SpamalotYdbTest
import vertis.spamalot.dao.model.storage.old.StoredChannel
import vertis.spamalot.model.UserId
import vertis.zio.test.ZioSpecBase
import org.scalacheck.magnolia._
import vertis.core.utils.NoWarnFilters

/** @author kusaeva
  */
class OldChannelsStorageIntSpec extends ZioSpecBase with SpamalotYdbTest {

  "OldChannelsStorage" should {
    "get user unread count" in ydbTest {
      val userId = random[UserId]
      val channel = random[StoredChannel].copy(userId = userId)
      for {
        _ <- Ydb.runTx(storages.oldChannelStorage.upsert(channel)): @annotation.nowarn(NoWarnFilters.Deprecation)
        count <- Ydb.runTx(storages.oldChannelStorage.unreadCount(userId))
        _ <- check(count should contain(channel.unreadCount))
      } yield ()
    }

    "remove user unread count" in ydbTest {
      val userId = random[UserId]
      val channel = random[StoredChannel].copy(userId = userId)
      for {
        _ <- Ydb.runTx(storages.oldChannelStorage.upsert(channel)): @annotation.nowarn(NoWarnFilters.Deprecation)
        _ <- Ydb.runTx(storages.oldChannelStorage.delete(userId))
        count <- Ydb.runTx(storages.oldChannelStorage.unreadCount(userId))
        _ <- check(count shouldBe empty)
      } yield ()
    }
  }
}
