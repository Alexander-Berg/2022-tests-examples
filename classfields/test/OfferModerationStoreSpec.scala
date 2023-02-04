package ru.yandex.vertis.general.hammer.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.hammer.storage.model.OfferModerationInfo
import general.hammer.storage.model.ExactOfferStatus.OfferStatus._
import ru.yandex.vertis.general.hammer.storage.ydb.{YdbBannedOffersDao, YdbModerationInfoDao}
import ru.yandex.vertis.general.hammer.storage.{BannedOffersDao, OfferModerationStore}
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion._
import zio.test._
import zio.test.TestAspect._

object OfferModerationStoreSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    (suite("OfferModerationStore")(
      testM("add offer to banned snapshot") {
        for {
          now <- ZIO.accessM[Clock](_.get.instant)
          _ <- Ydb.runTx(BannedOffersDao.removeOld(now))
          _ <- Ydb.runTx(OfferModerationStore.update("id1", OfferModerationInfo(exactStatus = BANNED)))
          list <- OfferModerationStore.listBanned(now)
        } yield assert(list)(hasSize(equalTo(1))) && assert(list.head)(equalTo("id1"))
      },
      testM("remove offer from banned snapshot") {
        for {
          now <- ZIO.accessM[Clock](_.get.instant)
          _ <- Ydb.runTx(BannedOffersDao.removeOld(now))
          _ <- Ydb.runTx(OfferModerationStore.update("id1", OfferModerationInfo(exactStatus = BANNED)))
          _ <- Ydb.runTx(OfferModerationStore.update("id1", OfferModerationInfo(exactStatus = ACTIVE)))
          list <- OfferModerationStore.listBanned(now)
        } yield assert(list)(hasSize(equalTo(0)))
      },
      testM("add 1500 offers to banned snapshot") {
        for {
          now <- ZIO.accessM[Clock](_.get.instant)
          _ <- Ydb.runTx(BannedOffersDao.removeOld(now))
          _ <- ZIO.foreachParN_(10)((1 to 1001).toList) { id =>
            Ydb.runTx(OfferModerationStore.update(s"id$id", OfferModerationInfo(exactStatus = BANNED)))
          }
          list <- OfferModerationStore.listBanned(now)
        } yield assert(list)(hasSize(equalTo(1001)))
      },
      testM("remove old bans") {
        for {
          old <- ZIO.accessM[Clock](_.get.instant)
          _ <- ZIO.foreachParN_(10)((1 to 10).toList) { id =>
            Ydb.runTx(OfferModerationStore.update(s"id$id", OfferModerationInfo(exactStatus = BANNED)))
          }
          now <- ZIO.accessM[Clock](_.get.instant)
          _ <- Ydb.runTx(BannedOffersDao.removeOld(now))
          _ <- Ydb.runTx(OfferModerationStore.update(s"id", OfferModerationInfo(exactStatus = BANNED)))
          list <- OfferModerationStore.listBanned(old)
        } yield assert(list)(hasSize(equalTo(1)))
      }
    ) @@ sequential).provideCustomLayer {
      TestYdb.ydb >>> (YdbBannedOffersDao.live ++ YdbModerationInfoDao.live ++ Ydb.txRunner) ++ Clock.live
    }
}
