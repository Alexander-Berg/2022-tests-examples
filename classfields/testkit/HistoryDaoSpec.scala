package ru.yandex.vertis.general.bonsai.storage.testkit

import java.time.Instant

import common.zio.ydb.Ydb.HasTxRunner
import ru.yandex.vertis.general.bonsai.model.CommentType.Comment
import ru.yandex.vertis.general.bonsai.model.{BonsaiError, VersionInfo}
import ru.yandex.vertis.general.bonsai.storage.HistoryDao
import ru.yandex.vertis.general.bonsai.storage.HistoryDao.HistoryDao
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object HistoryDaoSpec {

  def spec(
      label: String): Spec[zio.ZEnv with HistoryDao with HasTxRunner, TestFailure[BonsaiError], TestSuccess] = {
    suite(label)(
      testM("creates version") {
        runTx {
          for {
            now <- ZIO.effectTotal(Instant.now)
            version <- HistoryDao.newVersion(now, uid = 512L, Comment("")).run
          } yield assert(version)(succeeds(isGreaterThan(0L)))
        }
      },
      testM("lists versions") {
        val (uid1, uid2, uid3) = (512, 513, 514)
        val (comment1, comment2, comment3) = (Comment("a"), Comment("b"), Comment("c c"))
        for {
          now <- ZIO.effectTotal(Instant.ofEpochMilli(1595433333898L))
          version1 <- runTx(HistoryDao.newVersion(now, uid1, comment1))
          version2 <- runTx(HistoryDao.newVersion(now.plusMillis(1), uid2, comment2))
          version3 <- runTx(HistoryDao.newVersion(now.plusMillis(2), uid3, comment3))
          savedVersions <- runTx(HistoryDao.listVersions(Set(version1, version2, version3)))
          expectedVersions = Set(
            VersionInfo(version1, uid1, now, comment1),
            VersionInfo(version2, uid2, now.plusMillis(1), comment2),
            VersionInfo(version3, uid3, now.plusMillis(2), comment3)
          )
        } yield assert(savedVersions.toSet)(equalTo(expectedVersions)) &&
          assert(Seq(version1, version2, version3))(isSorted)
      }
    )
  }
}
