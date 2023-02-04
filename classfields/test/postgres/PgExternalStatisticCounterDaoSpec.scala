package auto.dealers.multiposting.storage.test.postgres

import java.time.Instant
import java.time.temporal.ChronoUnit

import common.zio.doobie.testkit.TestPostgresql
import auto.dealers.multiposting.model._
import auto.dealers.multiposting.storage.ExternalStatisticCounterDao
import auto.dealers.multiposting.storage.postgresql.PgExternalStatisticCounterDao
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object PgExternalStatisticCounterDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PgExternalStatisticCounterDao")(
      testM("upsert record") {
        val offerId = OfferId("offerId1")
        val source = Source("source")
        val clientId = ClientId(1)
        val record =
          ExternalStatisticCounterRecord(offerId, source, clientId, 0, 0, 0)

        for {
          _ <- ExternalStatisticCounterDao.upsert(record)
          initial <- ExternalStatisticCounterDao.find(offerId, source)
          _ <- ExternalStatisticCounterDao.upsert(record.copy(views = 1, phoneViews = 1, favorites = 1))
          updated <- ExternalStatisticCounterDao.find(offerId, source)
        } yield {
          assert(initial)(isSome(equalTo(record))) &&
          assert(updated)(isSome(equalTo(record.copy(views = 1, phoneViews = 1, favorites = 1))))
        }
      },
      testM("delete record") {
        val offerId = OfferId("offerId2")
        val source = Source("source")
        val clientId = ClientId(1)
        val record =
          ExternalStatisticCounterRecord(offerId, source, clientId, 0, 0, 0)

        for {
          _ <- ExternalStatisticCounterDao.upsert(record)
          notNone <- ExternalStatisticCounterDao.find(offerId, source)
          _ <- ExternalStatisticCounterDao.delete(offerId, source)
          none <- ExternalStatisticCounterDao.find(offerId, source)
        } yield {
          assert(notNone)(isSome(equalTo(record))) &&
          assert(none)(isNone)
        }
      },
      testM("delete stale rows") {
        val source = Source("source")
        val clientId = ClientId(1)
        val record1 = ExternalStatisticCounterRecord(OfferId("offerId1"), source, clientId, 0, 0, 0)
        val record2 = ExternalStatisticCounterRecord(OfferId("offerId2"), source, clientId, 0, 0, 0)
        val record3 = ExternalStatisticCounterRecord(OfferId("offerId3"), source, clientId, 0, 0, 0)
        val record4 = ExternalStatisticCounterRecord(OfferId("offerId4"), source, clientId, 0, 0, 0)
        val record5 = ExternalStatisticCounterRecord(OfferId("offerId5"), source, clientId, 0, 0, 0)
        val instant = Instant.now()

        for {
          _ <- PgExternalStatisticCounterDao.insertWintInstant(record1, instant)
          _ <- PgExternalStatisticCounterDao.insertWintInstant(record2, instant.minus(21, ChronoUnit.DAYS))
          _ <- PgExternalStatisticCounterDao.insertWintInstant(record3, instant.minus(25, ChronoUnit.DAYS))
          _ <- PgExternalStatisticCounterDao.insertWintInstant(record4, instant.minus(31, ChronoUnit.DAYS))
          _ <- PgExternalStatisticCounterDao.insertWintInstant(record5, instant.minus(33, ChronoUnit.DAYS))
          rows <- ExternalStatisticCounterDao.deleteStaleRows()
        } yield {
          assert(rows)(equalTo(1))
        }
      }
    ) @@ after(PgExternalStatisticCounterDao.clean) @@ beforeAll(
      PgExternalStatisticCounterDao.initSchema.orDie
    ) @@ sequential
  }.provideCustomLayerShared(
    TestPostgresql.managedTransactor >>> PgExternalStatisticCounterDao.live
  )
}
