package ru.yandex.vertis.general.favorites.logic.test

import common.clients.email.testkit.TestEmailSender
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.users.model.UserView
import io.circe.Json
import ru.yandex.vertis.general.common.clients.sup.testkit.SupClientMock
import ru.yandex.vertis.general.favorites.logic.FavoritesNotificationManager
import ru.yandex.vertis.general.favorites.model.FavoritesNotificationSchedulerConfig
import ru.yandex.vertis.general.favorites.model.searches.ShardCount
import ru.yandex.vertis.general.favorites.model.testkit.FavoriteNotificationGen
import ru.yandex.vertis.general.favorites.storage.FavoritesNotificationQueueDao
import ru.yandex.vertis.general.favorites.storage.ydb.notifications.YdbFavoritesNotificationQueueDao
import ru.yandex.vertis.general.favorites.testkit.TestSendingService
import ru.yandex.vertis.general.users.testkit.TestUserService
import ru.yandex.vertis.general.users.testkit.TestUserService.UpdateUsers
import common.zio.logging.Logging
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.{before, sequential, shrinks}
import zio.test.{assert, checkNM, DefaultRunnableSpec, ZSpec, _}
import zio.{UIO, ZIO}

object FavoritesNotificationManagerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("FavoritesNotificationManagerTest")(
      testM("normal process shards") {
        checkNM(1)(FavoriteNotificationGen.favoriteNotifications(10)) { notifications =>
          val shardId = 0
          for {
            _ <- SupClientMock.setPushResponse((_, _) => ZIO.succeed(""))
            _ <- runTx(
              FavoritesNotificationQueueDao.push(notifications)
            )
            savedNotificationBefore <- runTx(
              FavoritesNotificationQueueDao.peek(shardId, limit = 100)
            )
            _ <- FavoritesNotificationManager.processShards(Set(shardId))
            savedNotificationAfter <- runTx(
              FavoritesNotificationQueueDao.peek(shardId, limit = 100)
            )
          } yield assert(savedNotificationAfter.size)(equalTo(5)) &&
            assert(savedNotificationBefore.size)(equalTo(10))
        }
      },
      testM("process shards and sup failed") {
        checkNM(1)(FavoriteNotificationGen.favoriteNotifications(10)) { notifications =>
          val shardId = 0
          for {
            _ <- SupClientMock.setPushResponse((_, _) => ZIO.fail(new IllegalArgumentException("test")))
            _ <- runTx(
              FavoritesNotificationQueueDao.push(notifications)
            )
            savedNotificationBefore <- runTx(
              FavoritesNotificationQueueDao.peek(shardId, limit = 100)
            )
            _ <- FavoritesNotificationManager.processShards(Set(shardId))
            savedNotificationAfter <- runTx(
              FavoritesNotificationQueueDao.peek(shardId, limit = 100)
            )
          } yield assert(savedNotificationAfter.size)(equalTo(5)) &&
            assert(savedNotificationBefore.size)(equalTo(10))
        }
      },
      testM("не отправлять пуш, если в настройках нотификации пуши отключены") {
        checkNM(1)(Gen.listOfN(10)(FavoriteNotificationGen.emailNotificationGen)) { notifications =>
          val shardId = 0
          for {
            _ <- SupClientMock.setPushResponse((_, _) => ZIO.dieMessage("отправки пуша быть не должно"))
            _ <- runTx(
              FavoritesNotificationQueueDao.push(notifications)
            )
            savedNotificationBefore <- runTx(
              FavoritesNotificationQueueDao.peek(shardId, limit = 100)
            )
            _ <- FavoritesNotificationManager.processShards(Set(shardId))
            savedNotificationAfter <- runTx(
              FavoritesNotificationQueueDao.peek(shardId, limit = 100)
            )
          } yield assert(savedNotificationAfter.size)(equalTo(5)) &&
            assert(savedNotificationBefore.size)(equalTo(10))
        }
      },
      testM("отправлять письма") {
        checkNM(1)(FavoriteNotificationGen.emailNotificationGen) { notification =>
          val shardId = 0

          val params = Json.obj(
            "arg1" -> Json.fromBoolean(true),
            "arg2" -> Json.arr(Json.fromInt(123), Json.fromInt(456)),
            "arg3" -> Json.obj(
              "arg4" -> Json.fromString("test")
            )
          )

          for {
            _ <- SupClientMock.setPushResponse((_, _) => ZIO.succeed(""))
            updateUsers <- ZIO.service[UpdateUsers]
            _ <- updateUsers(_ =>
              notification.userIds.map(id => id -> UserView(id = id, email = Some(s"$id@email.ru"))).toMap
            )
            _ <- runTx(
              FavoritesNotificationQueueDao.push(
                notification.copy(params = params) :: Nil
              )
            )
            _ <- FavoritesNotificationManager.processShards(Set(shardId))
            emails <- TestEmailSender.allSent
          } yield assert(emails.map(_.template.args))(forall(equalTo(params.asObject.get)))
        }
      }
    ) @@ before(
      runTx(YdbFavoritesNotificationQueueDao.clean)
    ) @@ sequential @@ shrinks(0)
  }.provideCustomLayerShared {
    val sup = SupClientMock.layer

    val userService = TestUserService.layer
    val emailSender = TestEmailSender.layer

    val logging = Logging.live
    val txRunner = TestYdb.ydb >+> Ydb.txRunner
    val shardCount = UIO(ShardCount(1)).toLayer
    val notificationDao = (shardCount ++ TestYdb.ydb) >+> YdbFavoritesNotificationQueueDao.live
    val schedulerConfig = UIO(
      FavoritesNotificationSchedulerConfig(batchSize = 5, avatarsHost = "https://avatars.mdst.yandex.net")
    ).toLayer
    (notificationDao ++ schedulerConfig ++ sup ++ txRunner ++ Clock.live ++ logging ++ userService ++ emailSender) >+>
      FavoritesNotificationManager.live ++ notificationDao
  }
}
