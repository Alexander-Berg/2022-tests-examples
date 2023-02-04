package ru.yandex.vertis.general.favorites.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.favorites.model.testkit.FavoriteNotificationGen
import ru.yandex.vertis.general.favorites.model.searches.ShardCount
import ru.yandex.vertis.general.favorites.storage.FavoritesNotificationQueueDao
import ru.yandex.vertis.general.favorites.storage.ydb.notifications.YdbFavoritesNotificationQueueDao
import zio.UIO
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.{assert, checkNM, DefaultRunnableSpec, ZSpec}
import zio.test.TestAspect._

object YdbFavoritesNotificationQueueDaoTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite(" YdbFavoritesNotificationQueueDaoTest ")(
      testM("push, pull, size, delete") {
        checkNM(1)(FavoriteNotificationGen.favoriteNotifications(10).noShrink) { notifications =>
          val shardId = 0
          for {
            _ <- runTx(FavoritesNotificationQueueDao.push(notifications))
            savedNotification <- runTx(FavoritesNotificationQueueDao.peek(shardId, limit = 10))
            size <- FavoritesNotificationQueueDao.size
            _ <- runTx(FavoritesNotificationQueueDao.delete(notifications))
            savedNotification2 <- runTx(
              FavoritesNotificationQueueDao.peek(shardId, limit = 10)
            )
          } yield assert(savedNotification.size)(equalTo(notifications.size)) &&
            assert(size)(equalTo(notifications.size)) &&
            assert(savedNotification2.size)(equalTo(0))
        }
      }
    ) @@ before(runTx(YdbFavoritesNotificationQueueDao.clean)) @@ sequential @@ shrinks(0)
  }.provideCustomLayerShared {
    val shardCount = UIO(ShardCount(1)).toLayer
    val ydb = TestYdb.ydb
    val txRunner = ydb >+> Ydb.txRunner
    val dao = (shardCount ++ txRunner) >+> YdbFavoritesNotificationQueueDao.live
    dao ++ Clock.live
  }

}
