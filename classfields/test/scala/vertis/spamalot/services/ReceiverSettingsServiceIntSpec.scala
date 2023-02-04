package vertis.spamalot.services

import org.scalacheck.magnolia._
import ru.yandex.vertis.spamalot.model.ReceiverThrottleConfiguration
import ru.yandex.vertis.ydb.Ydb
import vertis.spamalot.SpamalotYdbTest
import vertis.spamalot.dao.model.settings.UserPushSchedule
import vertis.spamalot.dao.user.ReceiverSettingsServiceImpl
import vertis.spamalot.model.ReceiverId
import vertis.zio.test.ZioSpecBase

class ReceiverSettingsServiceIntSpec extends ZioSpecBase with SpamalotYdbTest {
  private val service = new ReceiverSettingsServiceImpl(storages)

  "ReceiverSettingsService" should {
    "get empty schedule if no schedule is present" in {
      val receiver = random[ReceiverId]
      val emptySchedule = UserPushSchedule()
      for {
        schedule <- service.getPushSchedule(receiver)
        _ <- check("no config:") {
          schedule shouldBe emptySchedule
        }
      } yield ()
    }

    "get correct schedule from new table" in ydbTest {
      val receiver = random[ReceiverId]
      val bannedTopics = Seq("bannedTopic")
      val config = random[ReceiverThrottleConfiguration]
        .copy(bannedTopics = bannedTopics)
      val expectedSchedule = UserPushSchedule(bannedTopics = bannedTopics.toSet)
      for {
        _ <- Ydb.runTx(storages.receiverConfigStorage.update(receiver, config))
        schedule <- service.getPushSchedule(receiver)
        _ <- check(schedule shouldBe expectedSchedule)
      } yield ()
    }
  }
}
