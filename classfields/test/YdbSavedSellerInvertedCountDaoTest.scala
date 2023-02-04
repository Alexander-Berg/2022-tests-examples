package ru.yandex.vertis.general.favorites.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.favorites.storage.SavedSellerInvertedCountDao
import ru.yandex.vertis.general.favorites.storage.ydb.inverted.YdbSavedSellerInvertedCountDao
import zio.clock.Clock
import zio.random.Random
import zio.test.{assert, checkNM, DefaultRunnableSpec, ZSpec}
import zio.test.Assertion._
import zio.test.TestAspect._

object YdbSavedSellerInvertedCountDaoTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbSavedSellerInvertedCountDaoTest")(
      testM("adjust counters") {
        checkNM(1)(SellerGen.anyUserId, SellerGen.anyUserId) { (ownerId, ownerId2) =>
          for {
            beforeInsert <- runTx(SavedSellerInvertedCountDao.get(Set(ownerId, ownerId2)))
            _ <- runTx(SavedSellerInvertedCountDao.adjust(Map(ownerId -> 1, ownerId2 -> 2)))
            afterInsert <- runTx(SavedSellerInvertedCountDao.get(Set(ownerId, ownerId2)))
            _ <- runTx(SavedSellerInvertedCountDao.adjust(Map(ownerId -> -1, ownerId2 -> -1)))
            afterAdjust <- runTx(SavedSellerInvertedCountDao.get(Set(ownerId, ownerId2)))
          } yield assert(beforeInsert)(isEmpty) &&
            assert(afterInsert)(equalTo(Map[SellerId, Int](ownerId -> 1, ownerId2 -> 2))) &&
            assert(afterAdjust)(equalTo(Map[SellerId, Int](ownerId -> 0, ownerId2 -> 1)))
        }
      }
    ).provideCustomLayer {
      TestYdb.ydb >>> (YdbSavedSellerInvertedCountDao.live ++ Ydb.txRunner) ++ Clock.live ++ Random.live
    } @@ sequential @@ shrinks(0)
  }
}
