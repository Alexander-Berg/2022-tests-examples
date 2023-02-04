package vertis.spamalot.services

import ru.yandex.vertis.spamalot.model.ReceiverThrottleConfiguration
import ru.yandex.vertis.ydb.Ydb
import vertis.spamalot.SpamalotYdbTest
import vertis.spamalot.dao.model.settings.UserPushSchedule
import vertis.spamalot.dao.user.ReceiverSettingsServiceImpl
import vertis.spamalot.model.{ReceiverId, UserId}
import vertis.zio.test.ZioSpecBase
import org.scalacheck.magnolia._

/**
 * Tests for backward compatibility
 */
class ReceiverSettingsBackCompSpec extends ZioSpecBase with SpamalotYdbTest {
  private val service = new ReceiverSettingsServiceImpl(storages)

  private def randomConfig(bannedTopics: Seq[String]): ReceiverThrottleConfiguration =
    random[ReceiverThrottleConfiguration].copy(bannedTopics = bannedTopics)

  "ReceiverSettingsServiceImpl" should {
    "get correct schedule from old table, save it to new one and delete from old one" in ydbTest {
      val receiver = ReceiverId.User(random[UserId])
      val bannedTopics = Seq("bannedTopic")
      val config = randomConfig(bannedTopics)
      val expectedSchedule = UserPushSchedule(bannedTopics = bannedTopics.toSet)
      for {
        _ <- Ydb.runTx(storages.userConfigStorage.update(receiver.userId, config))
        schedule <- service.getPushSchedule(receiver)
        _ <- check(schedule shouldBe expectedSchedule)
        newConfig <- Ydb.runTx(storages.receiverConfigStorage.get(receiver))
        _ <- check(newConfig.get shouldBe config)
        deletedConfig <- Ydb.runTx(storages.userConfigStorage.get(receiver.userId))
        _ <- check(deletedConfig shouldBe empty)
      } yield ()
    }
  }
}
