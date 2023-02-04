package auto.plus_me.storage.test

import auto.plus_me.storage.LockDao
import auto.plus_me.storage.pg.PgLockDao
import common.db.migrations.doobie.DoobieMigrations
import common.zio.doobie.testkit.TestPostgresql
import zio.{NonEmptyChunk, ZIO}
import zio.magic._
import zio.test.TestAspect._
import zio.test._

import java.time.Instant

object PgLockDaoSpec extends DefaultRunnableSpec {

  def spec = {
    suite("PgLockDao")(
      testM("повторная блокировка не работает") {
        for {
          dao <- ZIO.service[LockDao.Service]
          res1 <- dao.lock("1", Instant.EPOCH)
          res2 <- dao.lock("1", Instant.EPOCH)
        } yield assertTrue(res1, !res2)
      },
      testM("разблокировка работает") {
        for {
          dao <- ZIO.service[LockDao.Service]
          res1 <- dao.lock("1", Instant.EPOCH)
          _ <- dao.release("1")
          res2 <- dao.lock("1", Instant.EPOCH)
        } yield assertTrue(res1, res2)
      },
      testM("можно залочить после expire") {
        for {
          dao <- ZIO.service[LockDao.Service]
          res1 <- dao.lock("1", Instant.EPOCH)
          _ <- dao.expireOld(Instant.EPOCH)
          res2 <- dao.lock("1", Instant.EPOCH)
        } yield assertTrue(res1, res2)
      },
      testM("expire не удаляет актуальные локи") {
        for {
          dao <- ZIO.service[LockDao.Service]
          res1 <- dao.lock("1", Instant.EPOCH)
          _ <- dao.expireOld(Instant.EPOCH.minusSeconds(1))
          res2 <- dao.lock("1", Instant.EPOCH)
        } yield assertTrue(res1, !res2)
      },
      testM("prolong обновляет deadline у лока") {
        for {
          dao <- ZIO.service[LockDao.Service]
          res1 <- dao.lock("1", Instant.EPOCH)
          _ <- dao.prolong(NonEmptyChunk("1"), Instant.EPOCH.plusSeconds(1))
          _ <- dao.expireOld(Instant.EPOCH)
          res2 <- dao.lock("1", Instant.EPOCH)
        } yield assertTrue(res1, !res2)
      },
      testM("release не трогает чужие локи") {
        for {
          dao <- ZIO.service[LockDao.Service]
          otherDao <- ZIO.service[LockDao.Service].provideLayer(PgLockDao.live)
          res1 <- otherDao.lock("1", Instant.EPOCH)
          _ <- dao.release("1")
          res2 <- dao.lock("1", Instant.EPOCH)
        } yield assertTrue(res1, !res2)
      }
    ) @@ before(DoobieMigrations.dropAll *> DoobieMigrations.migrate("test")) @@ sequential
  }.injectCustomShared(
    TestPostgresql.managedTransactor,
    PgLockDao.live
  )
}
