package ru.yandex.vertis.general.snatcher.storage.test

import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import ru.yandex.vertis.general.snatcher.storage.LocksDao
import ru.yandex.vertis.general.snatcher.storage.postgresql.locks.PgLocksDao
import zio.ZIO
import zio.test._

object PgLocksDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("PgLocksDao")(
      testM("Cannot acquire lock before release") {
        for {
          dao <- ZIO.service[LocksDao.Service]
          hash = "cannotacquire"
          firstAcquire <- dao.tryAcquireLock(hash).transactIO
          secondAcquire <- dao.tryAcquireLock(hash).transactIO
        } yield assertTrue(firstAcquire) && assertTrue(!secondAcquire)
      },
      testM("Acquire - release - acquire") {
        for {
          dao <- ZIO.service[LocksDao.Service]
          hash = "release"
          firstAcquire <- dao.tryAcquireLock(hash).transactIO
          _ <- dao.releaseLock(hash).transactIO
          secondAcquire <- dao.tryAcquireLock(hash).transactIO
        } yield assertTrue(firstAcquire) && assertTrue(secondAcquire)
      }
    ).provideCustomLayerShared(
      TestPostgresql.managedTransactor >+> PgLocksDao.live
    )
}
