package ru.yandex.vertis.general.feed.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.feed.model.testkit.NamespaceIdGen
import ru.yandex.vertis.general.feed.model.{LockContext, NamespaceId}
import ru.yandex.vertis.general.feed.storage.FeedLockDao
import ru.yandex.vertis.general.feed.storage.postgresql.PgFeedLockDao
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.{clock, ZIO}

import java.time.Instant

object PgFeedLockDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("PgFeedLockDao")(
      testM("lock and unlock sellers") {
        checkNM(1)(
          SellerGen.anySellerId.noShrink,
          NamespaceIdGen.anyNamespaceId("1").noShrink,
          Gen.anyUUID.noShrink,
          NamespaceIdGen.anyNamespaceId("2").noShrink,
          Gen.anyUUID.noShrink
        ) { (sellerId, namespaceId1, uuid1, namespaceId2, uuid2) =>
          for {
            dao <- ZIO.service[FeedLockDao.Service]
            time <- clock.instant
            success11 <- dao.lock(LockContext.Processor, sellerId, namespaceId1, time, uuid1).transactIO
            success21 <- dao.lock(LockContext.Processor, sellerId, namespaceId2, time, uuid2).transactIO
            failure11 <- dao.lock(LockContext.Processor, sellerId, namespaceId1, time, uuid1).transactIO
            failure21 <- dao.lock(LockContext.Processor, sellerId, namespaceId2, time, uuid2).transactIO
            _ <- dao.unlock(LockContext.Processor, sellerId, namespaceId2, uuid1).transactIO
            failure12 <- dao.lock(LockContext.Processor, sellerId, namespaceId1, time, uuid1).transactIO
            failure22 <- dao.lock(LockContext.Processor, sellerId, namespaceId2, time, uuid2).transactIO
            _ <- dao.unlock(LockContext.Processor, sellerId, namespaceId1, uuid1).transactIO
            _ <- dao.unlock(LockContext.Processor, sellerId, namespaceId2, uuid2).transactIO
            success12 <- dao.lock(LockContext.Processor, sellerId, namespaceId1, time, uuid1).transactIO
            success22 <- dao.lock(LockContext.Processor, sellerId, namespaceId2, time, uuid1).transactIO
          } yield {
            assert(Seq(success11, success22, success12, success21))(forall(isTrue)) &&
            assert(Seq(failure11, failure22, failure12, failure21))(forall(isFalse))
          }
        }
      },
      testM("cleanup stale locks") {
        checkNM(1)(SellerGen.anySellerId.noShrink, NamespaceIdGen.anyNamespaceId().noShrink, Gen.anyUUID.noShrink) {
          (sellerId, namespaceId, uuid) =>
            for {
              dao <- ZIO.service[FeedLockDao.Service]
              time = Instant.ofEpochMilli(0)
              _ <- dao.lock(LockContext.Processor, sellerId, namespaceId, time, uuid).transactIO
              _ <- dao.lock(LockContext.Loader, sellerId, namespaceId, time, uuid).transactIO
              _ <- dao.cleanup(LockContext.Processor, Instant.ofEpochMilli(1)).transactIO
              success <- dao.lock(LockContext.Processor, sellerId, namespaceId, time, uuid).transactIO
              failure <- dao.lock(LockContext.Loader, sellerId, namespaceId, time, uuid).transactIO
            } yield assert(success)(isTrue) && assert(failure)(isFalse)
        }
      },
      testM("refresh owned locks") {
        val context = LockContext.Processor
        checkNM(1)(Gen.anyUUID.noShrink, Gen.anyUUID.noShrink) { (uuid, uuid2) =>
          val sellerId = SellerId.UserId(123456L)
          val namespaceId = NamespaceId("test_123412351")
          for {
            dao <- ZIO.service[FeedLockDao.Service]
            time = Instant.ofEpochMilli(0)
            _ <- dao.lock(context, sellerId, namespaceId, time, uuid).transactIO
            refreshTime = Instant.ofEpochMilli(10)
            refreshed <- dao.refreshLock(context, sellerId, namespaceId, refreshTime, uuid).transactIO
            wasOwned <- dao.refreshLock(context, sellerId, namespaceId, refreshTime, uuid2).transactIO
            cleanupBefore = Instant.ofEpochMilli(5)
            _ <- dao.cleanup(context, cleanupBefore).transactIO
            wasFree <- dao.lock(context, sellerId, namespaceId, Instant.ofEpochMilli(20), uuid).transactIO
          } yield assert(refreshed)(equalTo(uuid)) &&
            assert(wasFree)(isFalse) &&
            assert(wasOwned)(equalTo(uuid))
        }
      }
    ) @@ sequential).provideCustomLayerShared {
      TestPostgresql.managedTransactor >+> PgFeedLockDao.live
    }
  }
}
