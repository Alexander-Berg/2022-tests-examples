package auto.dealers.dealer_pony.storage.postgres.test

import common.geobase.model.RegionIds.RegionId
import auto.dealers.dealer_pony.storage.postgres.PgDealerStatusDao
import auto.dealers.dealer_pony.storage.dao.DealerStatusDao
import auto.dealers.dealer_pony.storage.dao.DealerStatusDao.Record

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie._
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test.{DefaultRunnableSpec, _}
import zio.{Has, Task, ZIO}

object PgDealerStatusDaoSpec extends DefaultRunnableSpec {

  implicit val regionMapping: Meta[RegionId] = Meta[Long].imap(RegionId.apply)(_.id.toInt)

  override def spec = {
    val tests = suite("PgDealerStatusDaoSpec")(
      simpleInsert,
      insertOutdatedEntry,
      wlCheck,
      wlLostCheck
    ) @@ beforeAll(dbInit) @@ after(dbCleanup) @@ sequential

    tests.provideCustomLayerShared(
      TestPostgresql.managedTransactor ++
        (TestPostgresql.managedTransactor >>> PgDealerStatusDao.live)
    )
  }

  val now = OffsetDateTime
    .now()
    .truncatedTo(ChronoUnit.DAYS)
    .plusHours(1)

  val entry = Record(
    dealerId = 0L,
    updatedAt = now,
    loyaltyLevel = 12,
    regionId = RegionId(0),
    hasFullStock = true,
    wlAvailable = true
  )

  val simpleInsert =
    testM("Insert single loyalty status") {
      for {
        xa <- ZIO.access[Has[Transactor[Task]]](_.get)
        _ <- DealerStatusDao.upsert(entry)
        result <- sql"SELECT * FROM dealers_status".query[Record].unique.transact(xa)
      } yield assert(result)(equalTo(entry))
    }

  val insertOutdatedEntry =
    testM("Inserting outdated entry does nothing") {
      for {
        xa <- ZIO.access[Has[Transactor[Task]]](_.get)
        _ <- DealerStatusDao.upsert(entry)
        _ <- DealerStatusDao.upsert(
          entry.copy(
            wlAvailable = false,
            updatedAt = entry.updatedAt.minusMinutes(20)
          )
        )
        result <- sql"SELECT * FROM dealers_status".query[Record].unique.transact(xa)
      } yield assert(result)(equalTo(entry))
    }

  val wlCheck =
    testM("Whitelist available") {
      for {
        _ <- DealerStatusDao.upsert(entry)
        result <- DealerStatusDao.wlAvailable(entry.dealerId)
      } yield assert(result)(isTrue)
    }

  val wlLostCheck =
    testM("Whitelist flag unset") {
      for {
        _ <- DealerStatusDao.upsert(entry)
        _ <- DealerStatusDao.upsert(
          entry.copy(
            wlAvailable = false,
            updatedAt = now.plusHours(1)
          )
        )
        result <- DealerStatusDao.wlAvailable(entry.dealerId)
      } yield assert(result)(isFalse)
    }

  val dbInit =
    ZIO
      .service[Transactor[Task]]
      .flatMap(InitSchema("/schema.sql", _))
      .orDie

  val dbCleanup =
    ZIO
      .service[Transactor[Task]]
      .flatMap(xa =>
        sql"DELETE FROM dealers_privileged".update.run.transact(xa) *>
          sql"DELETE FROM dealers_status".update.run.transact(xa)
      )

}
