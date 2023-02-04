package auto.carfax.promo_dispenser.storage.test

import auto.carfax.promo_dispenser.storage.dao.UsersDao
import auto.carfax.promo_dispenser.storage.postgresql.UsersDaoImpl
import auto.carfax.promo_dispenser.storage.testkit.{Generators, Schema}
import common.zio.doobie.syntax._
import common.zio.doobie.testkit.TestPostgresql
import zio.ZIO
import zio.test.TestAspect.{after, beforeAll, sequential, shrinks}
import zio.test.{assertTrue, checkM, DefaultRunnableSpec, ZSpec}

object UsersDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("UsersDao")(
      testM("upsert and read user") {
        checkM(Generators.anyUserId) { userId =>
          for {
            dao <- ZIO.service[UsersDao.Service]
            beforeInsertRes <- dao.getUserWithLock(userId).transactIO
            _ <- dao.upsert(userId).transactIO
            afterInsertRes <- dao.getUserWithLock(userId).transactIO
            _ <- dao.upsert(userId).transactIO
            afterUpdateRes <- dao.getUserWithLock(userId).transactIO
          } yield assertTrue(beforeInsertRes.isEmpty) &&
            assertTrue(afterInsertRes.contains(userId)) &&
            assertTrue(afterUpdateRes.contains(userId))
        }
      }
    ) @@ beforeAll(Schema.init) @@ after(Schema.cleanup) @@ sequential @@ shrinks(0)).provideCustomLayerShared {
      TestPostgresql.managedTransactor(version = "12") >+> UsersDaoImpl.live
    }
  }
}
