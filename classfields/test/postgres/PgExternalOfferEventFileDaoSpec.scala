package auto.dealers.multiposting.storage.test.postgres

import common.zio.doobie.testkit.TestPostgresql
import auto.dealers.multiposting.model.EventType._
import auto.dealers.multiposting.storage.ExternalOfferEventFileDao
import auto.dealers.multiposting.storage.postgresql.PgExternalOfferEventFileDao
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object PgExternalOfferEventFileDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PgExternalOfferEventFileDao")(
      testM("return last filename") {
        for {
          none <- ExternalOfferEventFileDao.getLastProcessedFilename(Statistics)
          _ <- ExternalOfferEventFileDao.insertNewFilename(ClassifiedsState, "f1")
          _ <- ExternalOfferEventFileDao.insertNewFilename(Statistics, "f2")
          _ <- ExternalOfferEventFileDao.insertNewFilename(Statistics, "f3")
          lastStatistics <- ExternalOfferEventFileDao.getLastProcessedFilename(Statistics)
          lastClassifiedsInfo <- ExternalOfferEventFileDao.getLastProcessedFilename(ClassifiedsState)
        } yield {
          assert(none)(isNone) &&
          assert(lastStatistics)(isSome(equalTo("f3"))) &&
          assert(lastClassifiedsInfo)(isSome(equalTo("f1")))
        }
      }
    ) @@ after(PgExternalOfferEventFileDao.clean) @@ beforeAll(
      PgExternalOfferEventFileDao.initSchema.orDie
    ) @@ sequential
  }.provideCustomLayerShared(
    TestPostgresql.managedTransactor >>> PgExternalOfferEventFileDao.live
  )
}
