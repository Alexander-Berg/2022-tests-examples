package vertis.spamalot.dao

import ru.yandex.vertis.spamalot.model.ReceiverThrottleConfiguration
import ru.yandex.vertis.ydb.Ydb
import vertis.spamalot.SpamalotYdbTest
import vertis.spamalot.model.UserId
import vertis.zio.test.ZioSpecBase
import org.scalacheck.magnolia._

/** @author kusaeva
  */
class UserConfigStorageIntSpec extends ZioSpecBase with SpamalotYdbTest {

  private def randomConfig: ReceiverThrottleConfiguration = random[ReceiverThrottleConfiguration]
    .copy(bannedTopics = Seq("topic1"))

  "UserConfigStorage" should {
    "get config" in ydbTest {
      val userId = random[UserId]
      val config = randomConfig
      for {
        _ <- checkM("no config") {
          Ydb
            .runTx(storages.userConfigStorage.get(userId))
            .map(_ shouldBe empty)
        }
        _ <- Ydb.runTx(storages.userConfigStorage.update(userId, config))
        _ <- checkM("update:") {
          val c = Ydb.runTx(storages.userConfigStorage.get(userId))
          c.map(_ shouldBe defined)
          c.map(_.get shouldBe config)
        }
      } yield ()
    }

    "delete config" in ydbTest {
      val userId = random[UserId]
      val config = randomConfig
      for {
        _ <- Ydb.runTx(storages.userConfigStorage.update(userId, config))
        savedConfig <- Ydb.runTx(storages.userConfigStorage.get(userId))
        _ <- check(savedConfig shouldBe defined)
        _ <- Ydb.runTx(storages.userConfigStorage.delete(userId))
        deletedConfig <- Ydb.runTx(storages.userConfigStorage.get(userId))
        _ <- check(deletedConfig shouldBe empty)
      } yield ()
    }
  }
}
