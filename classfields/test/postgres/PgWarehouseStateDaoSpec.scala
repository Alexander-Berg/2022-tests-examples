package auto.dealers.multiposting.storage.test.postgres

import java.time.LocalDate
import doobie.Transactor
import doobie.implicits._
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import auto.dealers.multiposting.model._
import auto.dealers.multiposting.storage.WarehouseStateDao
import auto.dealers.multiposting.storage.postgresql.PgWarehouseStateDao
import ru.auto.api.api_offer_model.{Category, OfferStatus, Section}
import zio._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, _}
import zio.test.TestAspect.{after, beforeAll, sequential}

object PgWarehouseStateDaoSpec extends DefaultRunnableSpec {
  val now = LocalDate.now()

  val prototypeRecord = WarehouseState(
    day = now,
    clientId = ClientId(1),
    cardId = CardId(1),
    vin = Vin("vin"),
    changeVersion = 0,
    category = Category.CARS,
    section = Section.NEW,
    autoruStatus = Some(OfferStatus.INACTIVE),
    avitoStatus = None,
    dromStatus = None,
    globalStatus = Some(OfferStatus.INACTIVE)
  )

  val upsertBatchCheck = testM("batch insert should do update status on conflict") {
    for {
      emptyResult <- PgWarehouseStateDao.selectAll
      records = (1 to 20).map(n => prototypeRecord.copy(day = now.plusDays(n)))
      _ <- WarehouseStateDao.upsertBatch(records)
      firstResult <- PgWarehouseStateDao.selectAll
      recordsChanged = (1 to 20).map(n =>
        prototypeRecord.copy(
          changeVersion = 1,
          day = now.plusDays(n),
          autoruStatus = Some(OfferStatus.ACTIVE),
          globalStatus = Some(OfferStatus.ACTIVE)
        )
      )
      _ <- WarehouseStateDao.upsertBatch(recordsChanged)
      secondResult <- PgWarehouseStateDao.selectAll
    } yield {
      assert(emptyResult.isEmpty)(equalTo(true)) &&
      assert(firstResult)(equalTo(records)) &&
      assert(secondResult)(
        equalTo(
          records.map(
            _.copy(changeVersion = 1, autoruStatus = Some(OfferStatus.ACTIVE), globalStatus = Some(OfferStatus.ACTIVE))
          )
        )
      )
    }
  }

  val upsertBatchCheckDoNotUpdateIfNewChangeVersionLessOrEqualThanCurrent =
    testM("batch insert should not update records if new change version is less than current") {
      for {
        emptyResult <- PgWarehouseStateDao.selectAll
        records = (1 to 20).map(n => prototypeRecord.copy(changeVersion = 5, day = now.plusDays(n)))
        _ <- WarehouseStateDao.upsertBatch(records)
        firstResult <- PgWarehouseStateDao.selectAll
        recordsChanged = (1 to 20).map(n =>
          prototypeRecord.copy(
            changeVersion = 3,
            day = now.plusDays(n),
            autoruStatus = Some(OfferStatus.ACTIVE),
            globalStatus = Some(OfferStatus.ACTIVE)
          )
        )
        _ <- WarehouseStateDao.upsertBatch(recordsChanged)
        secondResult <- PgWarehouseStateDao.selectAll
      } yield {
        assert(emptyResult.isEmpty)(equalTo(true)) &&
        assert(firstResult)(equalTo(records)) &&
        assert(secondResult)(
          equalTo(
            records.map(
              _.copy(
                changeVersion = 5,
                autoruStatus = Some(OfferStatus.INACTIVE),
                globalStatus = Some(OfferStatus.INACTIVE)
              )
            )
          )
        )
      }
    }

  val perDayState =
    testM("get per day state") {
      val autoruActiveRecord =
        prototypeRecord.copy(
          cardId = CardId(1),
          vin = Vin("v1"),
          autoruStatus = Some(OfferStatus.ACTIVE),
          globalStatus = Some(OfferStatus.ACTIVE)
        )
      val avitoActiveRecord =
        prototypeRecord.copy(
          cardId = CardId(2),
          vin = Vin("v2"),
          autoruStatus = None,
          avitoStatus = Some(OfferStatus.ACTIVE),
          globalStatus = Some(OfferStatus.ACTIVE)
        )
      val dromActiveRecord =
        prototypeRecord.copy(
          cardId = CardId(3),
          vin = Vin("v3"),
          autoruStatus = None,
          dromStatus = Some(OfferStatus.ACTIVE),
          globalStatus = Some(OfferStatus.ACTIVE)
        )

      val filters = Filters.WarehouseFilter(now, now, None, None)
      val expected = Seq(
        WarehouseDayState(
          day = now,
          autoruActive = 1,
          autoruInactive = 0,
          autoruRemoved = 0,
          avitoActive = 1,
          avitoInactive = 0,
          avitoRemoved = 0,
          dromActive = 1,
          dromInactive = 0,
          dromRemoved = 0,
          totalActive = 3,
          totalInactive = 0,
          totalRemoved = 0
        )
      )

      for {
        _ <- WarehouseStateDao.upsertBatch(Seq(autoruActiveRecord, avitoActiveRecord, dromActiveRecord))
        perDayState <- WarehouseStateDao.getPerDayState(ClientId(1), filters)
      } yield {
        assert(perDayState)(equalTo(expected))
      }
    }

  val getUniqueCounters =
    testM("get unique counters") {
      val autoruActiveRecord =
        prototypeRecord.copy(
          cardId = CardId(1),
          vin = Vin("vin"),
          autoruStatus = Some(OfferStatus.ACTIVE),
          globalStatus = Some(OfferStatus.ACTIVE)
        )
      val avitoActiveRecord =
        prototypeRecord.copy(
          cardId = CardId(2),
          vin = Vin("vin"),
          autoruStatus = None,
          avitoStatus = Some(OfferStatus.ACTIVE),
          globalStatus = Some(OfferStatus.ACTIVE)
        )
      val dromActiveRecord =
        prototypeRecord.copy(
          cardId = CardId(3),
          vin = Vin("vin"),
          autoruStatus = None,
          dromStatus = Some(OfferStatus.ACTIVE),
          globalStatus = Some(OfferStatus.ACTIVE)
        )

      val filters = Filters.WarehouseFilter(now, now, None, None)
      val expected =
        WarehouseUniqueCounters(
          autoruActive = 1,
          autoruInactive = 0,
          avitoActive = 1,
          avitoInactive = 0,
          dromActive = 1,
          dromInactive = 0,
          totalActive = 1, // distinct by vin
          totalInactive = 0,
          totalRemoved = 0
        )

      for {
        _ <- WarehouseStateDao.upsertBatch(Seq(autoruActiveRecord, avitoActiveRecord, dromActiveRecord))
        uniqueCounters <- WarehouseStateDao.getUniqueCounters(ClientId(1), filters)
      } yield {
        assert(uniqueCounters)(equalTo(expected))
      }
    }

  val dbInit =
    ZIO
      .service[Transactor[Task]]
      .flatMap(InitSchema("/schema.sql", _))
      .orDie

  val dbCleanup =
    ZIO
      .service[Transactor[Task]]
      .flatMap(xa => sql"DELETE FROM warehouse_state".update.run.transact(xa))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PgWarehouseStateDao")(
      upsertBatchCheck,
      upsertBatchCheckDoNotUpdateIfNewChangeVersionLessOrEqualThanCurrent,
      perDayState,
      getUniqueCounters
    ) @@
      beforeAll(dbInit) @@
      after(dbCleanup) @@
      sequential
  }.provideCustomLayerShared(TestPostgresql.managedTransactor >+> PgWarehouseStateDao.live)
}
