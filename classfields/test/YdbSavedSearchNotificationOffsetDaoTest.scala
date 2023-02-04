package ru.yandex.vertis.general.favorites.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.favorites.model.searches.SavedSearchInvertedKey
import ru.yandex.vertis.general.favorites.model.testkit.SavedSearchKeyGen
import ru.yandex.vertis.general.favorites.storage.SavedSearchNotificationOffsetDao
import ru.yandex.vertis.general.favorites.storage.ydb.notifications.YdbSavedSearchNotificationOffsetDao
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.TestAspect._
import zio.test.{assert, checkNM, DefaultRunnableSpec, ZSpec}

object YdbSavedSearchNotificationOffsetDaoTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite(" YdbSavedSearchNotificationOffsetDaoTest ")(
      testM("upsert, get, delete") {
        checkNM(1)(SavedSearchKeyGen.anySavedSearchKeys(10)) { keys =>
          for {
            _ <- runTx(SavedSearchNotificationOffsetDao.upsert(keys))
            storedKeys1 <- runTx(SavedSearchNotificationOffsetDao.get(keys.map(_.shardId)))
            _ <- runTx(SavedSearchNotificationOffsetDao.delete(keys.map(_.shardId)))
            storedKeys2 <- runTx(SavedSearchNotificationOffsetDao.get(keys.map(_.shardId)))
          } yield assert(storedKeys1.values.toSet)(equalTo(keys.toSet)) &&
            assert(storedKeys2)(equalTo(Map.empty[Int, SavedSearchInvertedKey]))
        }
      }
    )
  }.provideCustomLayer {
    TestYdb.ydb >>> (YdbSavedSearchNotificationOffsetDao.live ++ Ydb.txRunner) ++ Clock.live
  } @@ sequential @@ shrinks(0)
}
