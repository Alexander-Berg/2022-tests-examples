package ru.yandex.vertis.general.wisp.storage.test

import java.time.Instant
import java.time.temporal.{ChronoUnit, TemporalUnit}

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.wisp.model.UnreadRecord
import ru.yandex.vertis.general.wisp.storage.UnreadDao
import ru.yandex.vertis.general.wisp.storage.ydb.YdbUnreadDao
import zio.clock.Clock
import zio.test._
import zio.test.Assertion._
import common.zio.ydb.testkit.TestYdb.runTx
import zio.ZIO

object YdbUnreadDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("YdbUnreadDao")(
      testM("Get nothing for non-exist room") {
        assertM(runTx(UnreadDao.getUnread("fakeRoomId")))(isNone)
      },
      testM("Upsert new record") {
        checkNM(1)(
          Gen.instant(Instant.EPOCH, Instant.now()),
          Gen.option(Gen.instant(Instant.now, Instant.now.plusSeconds(20)))
        ) { (lastMessage, notifyAfter) =>
          val unreadRecord =
            UnreadRecord(
              "upsertInsert",
              Some(lastMessage.toEpochMilli),
              Some(lastMessage.toEpochMilli),
              lastMessage.toEpochMilli,
              notifyAfter.map(_.truncatedTo(ChronoUnit.SECONDS))
            )
          for {
            _ <- runTx(UnreadDao.upsertUnread(unreadRecord))
            fromDb <- runTx(UnreadDao.getUnread(unreadRecord.roomId))
          } yield assert(fromDb)(isSome(equalTo(unreadRecord)))
        }
      },
      testM("Update timestamp in record through upsert") {
        checkNM(1)(Gen.instant(Instant.EPOCH, Instant.now())) { lastMessage =>
          val unreadRecord =
            UnreadRecord(
              "upsertUpdates",
              Some(lastMessage.toEpochMilli),
              Some(lastMessage.toEpochMilli),
              lastMessage.toEpochMilli,
              Some(Instant.now.truncatedTo(ChronoUnit.SECONDS))
            )
          for {
            _ <- runTx(UnreadDao.upsertUnread(unreadRecord))
            _ <- runTx(UnreadDao.upsertUnread(unreadRecord.copy(lastMessage = lastMessage.plusSeconds(5).toEpochMilli)))
            afterUpdate <- runTx(UnreadDao.getUnread(unreadRecord.roomId))
          } yield assert(afterUpdate.map(_.lastMessage))(isSome(isGreaterThan(unreadRecord.lastMessage)))
        }
      },
      testM("Get relevant unreads ready to notify") {
        checkNM(1)(Gen.instant(Instant.EPOCH, Instant.now())) { timestamp =>
          for {
            _ <- runTx(UnreadDao.upsertUnread(UnreadRecord("readyToNotify", None, None, 0, Some(timestamp))))
            _ <- runTx(UnreadDao.upsertUnread(UnreadRecord("neverNotify", None, None, 0, None)))
            later <- ZIO.succeed(Instant.now().plusSeconds(100))
            _ <- runTx(UnreadDao.upsertUnread(UnreadRecord("earlyToNotify", None, None, 0, Some(later))))
            resultIds <- runTx(UnreadDao.getUnreadsReadyToNotify(1000)).map(_.map(_.roomId))
          } yield assert(resultIds)(contains("readyToNotify")) &&
            assert(resultIds)(not(contains("neverNotify"))) &&
            assert(resultIds)(not(contains("earlyToNotify")))

        }
      }
    ).provideCustomLayerShared(
      TestYdb.ydb ++ Clock.live >>> (YdbUnreadDao.live ++ Ydb.txRunner)
    ) @@ TestAspect.sequential
}
