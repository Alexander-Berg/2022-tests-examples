package ru.yandex.vertis.general.wisp.storage.test

import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.general.wisp.storage.LockDao
import ru.yandex.vertis.general.wisp.storage.ydb.YdbLockDao
import zio.clock.Clock
import zio.test.Assertion.{isFalse, isTrue}
import zio.test._

object YdbLockDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("YdbLockDao")(
      testM("Can't acquire lock while already acquired") {
        for {
          firstResult <- LockDao.tryAcquireLock("offer_double", "buyer_double")
          secondResult <- LockDao.tryAcquireLock("offer_double", "buyer_double")
        } yield assert(firstResult)(isTrue) && assert(secondResult)(isFalse)
      },
      testM("Can acquire lock after release") {
        for {
          firstResult <- LockDao.tryAcquireLock("offer", "buyer")
          _ <- LockDao.releaseLock("offer", "buyer")
          secondResult <- LockDao.tryAcquireLock("offer", "buyer")
        } yield assert(firstResult)(isTrue) && assert(secondResult)(isTrue)
      }
    )
      .provideCustomLayerShared(
        TestYdb.ydb ++ Clock.live >+> (YdbLockDao.live)
      )
}
