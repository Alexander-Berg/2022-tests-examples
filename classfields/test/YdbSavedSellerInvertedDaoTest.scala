package ru.yandex.vertis.general.favorites.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.favorites.notification_model.{NotificationChannel, NotificationSettings}
import ru.yandex.vertis.general.common.model.user.OwnerId
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.favorites.model.sellers.SavedSellerInverted
import ru.yandex.vertis.general.favorites.storage.SavedSellerInvertedDao
import ru.yandex.vertis.general.favorites.storage.ydb.inverted.YdbSavedSellerInvertedDao
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.{assert, checkNM, DefaultRunnableSpec, ZSpec}

object YdbSavedSellerInvertedDaoTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbSavedSellerInvertedDaoTest")(
      testM("Modify & list") {
        checkNM(1)(SellerGen.anyUserId) { sellerId =>
          val notificationSettings = NotificationSettings(Seq(NotificationChannel.EMAIL))
          val lastSeenAt = 123
          val lastSentAt = 456
          val key1 = SavedSellerInverted.Key(sellerId, OwnerId.UserId(123))
          val key2 = SavedSellerInverted.Key(sellerId, OwnerId.UserId(456))

          for {
            _ <- runTx(
              SavedSellerInvertedDao.upsert(
                Seq(
                  SavedSellerInverted(key1, notificationSettings, lastSeenAt = lastSeenAt, lastSentAt = lastSentAt),
                  SavedSellerInverted(key2, notificationSettings, lastSeenAt = lastSeenAt, lastSentAt = lastSentAt)
                )
              )
            )
            listing1 <- runTx(SavedSellerInvertedDao.list(sellerId, 1, None))
            listing2 <- runTx(SavedSellerInvertedDao.list(sellerId, 1, listing1.lastOption.map(_.key)))
            _ <- runTx(SavedSellerInvertedDao.delete(Seq(key1)))
            listing3 <- runTx(SavedSellerInvertedDao.list(sellerId, 2, None))
          } yield assert(listing1)(hasSize(equalTo(1))) &&
            assert(listing2)(hasSize(equalTo(1))) &&
            assert(listing1 ++ listing2)(
              hasSameElements(
                Seq(
                  SavedSellerInverted(key1, notificationSettings, lastSeenAt = lastSeenAt, lastSentAt = lastSentAt),
                  SavedSellerInverted(key2, notificationSettings, lastSeenAt = lastSeenAt, lastSentAt = lastSentAt)
                )
              )
            ) &&
            assert(listing3)(equalTo(Seq(SavedSellerInverted(key2, notificationSettings, lastSeenAt, lastSentAt))))
        }
      },
      testM("Modify & get") {
        checkNM(1)(SellerGen.anyUserId) { sellerId =>
          val notificationSettings = NotificationSettings(Seq(NotificationChannel.EMAIL))
          val lastSeenAt = 123
          val lastSentAt = 456
          val key1 = SavedSellerInverted.Key(sellerId, OwnerId.UserId(123))
          val key2 = SavedSellerInverted.Key(sellerId, OwnerId.UserId(456))
          val key3 = SavedSellerInverted.Key(sellerId, OwnerId.UserId(789))
          val seller1 =
            SavedSellerInverted(key1, notificationSettings, lastSeenAt = lastSeenAt, lastSentAt = lastSentAt)
          val seller2 =
            SavedSellerInverted(key2, notificationSettings, lastSeenAt = lastSeenAt, lastSentAt = lastSentAt)

          for {
            _ <- runTx(SavedSellerInvertedDao.upsert(Seq(seller1, seller2)))
            listing1 <- runTx(SavedSellerInvertedDao.get(Seq(key1)))
            listing2 <- runTx(SavedSellerInvertedDao.get(Seq(key2, key3)))
          } yield assert(listing1)(equalTo(Seq(seller1))) &&
            assert(listing2)(equalTo(Seq(seller2)))
        }
      }
    ).provideCustomLayer {
      TestYdb.ydb >>> (YdbSavedSellerInvertedDao.live ++ Ydb.txRunner) ++ Clock.live ++ Random.live
    } @@ sequential @@ shrinks(0)
  }
}
