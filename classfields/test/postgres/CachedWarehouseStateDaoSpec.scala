package auto.dealers.multiposting.storage.test.postgres

import java.time.LocalDate

import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie.Transactor
import doobie.implicits._
import ru.auto.api.api_offer_model.{Category, OfferStatus, Section}
import auto.dealers.multiposting.cache.Cache
import auto.dealers.multiposting.cache.testkit.InMemoryCache
import auto.dealers.multiposting.model.{
  CardId,
  ClientId,
  Filters,
  Vin,
  WarehouseDayState,
  WarehouseState,
  WarehouseUniqueCounters
}
import auto.dealers.multiposting.storage.{CachedWarehouseStateDao, WarehouseStateDao}
import auto.dealers.multiposting.storage.CachedWarehouseStateDao._
import auto.dealers.multiposting.storage.postgresql.PgWarehouseStateDao
import zio.{Has, Task, ZIO}
import zio.interop.catz._
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.{after, beforeAll, sequential}

object CachedWarehouseStateDaoSpec extends DefaultRunnableSpec {
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

  private val readStateFromCache =
    testM("Read warehouse daily state first time from DB second time from cache") {
      val autoruActiveRecord =
        prototypeRecord.copy(
          cardId = CardId(1),
          vin = Vin("v1"),
          autoruStatus = Some(OfferStatus.ACTIVE),
          globalStatus = Some(OfferStatus.ACTIVE)
        )

      val filters = Filters.WarehouseFilter(now, now, None, None)
      val key = WarehouseStateKey(ClientId(1), WarehouseStateRequest, filters)

      val expected = Seq(
        WarehouseDayState(
          day = now,
          autoruActive = 1,
          autoruInactive = 0,
          autoruRemoved = 0,
          avitoActive = 0,
          avitoInactive = 0,
          avitoRemoved = 0,
          dromActive = 0,
          dromInactive = 0,
          dromRemoved = 0,
          totalActive = 1,
          totalInactive = 0,
          totalRemoved = 0
        )
      )

      for {
        shouldBeEmpty <- Cache.get[WarehouseStateKey, Seq[WarehouseDayState]](key)
        _ <- WarehouseStateDao.upsertBatch(Seq(autoruActiveRecord))
        perDayState <- WarehouseStateDao.getPerDayState(ClientId(1), filters)
        cached <- Cache.get[WarehouseStateKey, Seq[WarehouseDayState]](key)
      } yield assert(shouldBeEmpty)(isNone) && assert(perDayState)(equalTo(expected)) && assert(cached)(
        isSome(equalTo(expected))
      )
    }

  private val fallbackToStorageStateOnCacheError =
    testM("WarehouseDailyState should fallback to storage on CacheError") {
      val autoruActiveRecord =
        prototypeRecord.copy(
          cardId = CardId(1),
          vin = Vin("v1"),
          autoruStatus = Some(OfferStatus.ACTIVE),
          globalStatus = Some(OfferStatus.ACTIVE)
        )

      val filters = Filters.WarehouseFilter(now, now, None, None)
      val key = WarehouseStateKey(ClientId(1), WarehouseStateRequest, filters)

      val expected = Seq(
        WarehouseDayState(
          day = now,
          autoruActive = 1,
          autoruInactive = 0,
          autoruRemoved = 0,
          avitoActive = 0,
          avitoInactive = 0,
          avitoRemoved = 0,
          dromActive = 0,
          dromInactive = 0,
          dromRemoved = 0,
          totalActive = 1,
          totalInactive = 0,
          totalRemoved = 0
        )
      )

      for {
        _ <- Cache.set[WarehouseStateKey, String](key, "random value", None)
        _ <- WarehouseStateDao.upsertBatch(Seq(autoruActiveRecord))
        perDayState <- WarehouseStateDao.getPerDayState(ClientId(1), filters)
      } yield assert(perDayState)(equalTo(expected))
    }

  private val readUniqueCountersFromCache =
    testM("Read warehouse unique counters first time from DB second time from cache") {
      val autoruActiveRecord =
        prototypeRecord.copy(
          cardId = CardId(1),
          vin = Vin("v1"),
          autoruStatus = Some(OfferStatus.ACTIVE),
          globalStatus = Some(OfferStatus.ACTIVE)
        )

      val filters = Filters.WarehouseFilter(now, now, None, None)
      val key = WarehouseStateKey(ClientId(1), WarehouseUniqueCountersRequest, filters)

      val expected =
        WarehouseUniqueCounters(
          autoruActive = 1,
          autoruInactive = 0,
          avitoActive = 0,
          avitoInactive = 0,
          dromActive = 0,
          dromInactive = 0,
          totalActive = 1,
          totalInactive = 0,
          totalRemoved = 0
        )

      for {
        shouldBeEmpty <- Cache.get[WarehouseStateKey, WarehouseUniqueCounters](key)
        _ <- WarehouseStateDao.upsertBatch(Seq(autoruActiveRecord))
        perDayState <- WarehouseStateDao.getUniqueCounters(ClientId(1), filters)
        cached <- Cache.get[WarehouseStateKey, WarehouseUniqueCounters](key)
      } yield assert(shouldBeEmpty)(isNone) && assert(perDayState)(equalTo(expected)) && assert(cached)(
        isSome(equalTo(expected))
      )
    }

  private val fallbackToStorageUniqueCountersOnCacheError =
    testM("UniqueCounters should fallback to storage on CacheError") {
      val autoruActiveRecord =
        prototypeRecord.copy(
          cardId = CardId(1),
          vin = Vin("v1"),
          autoruStatus = Some(OfferStatus.ACTIVE),
          globalStatus = Some(OfferStatus.ACTIVE)
        )

      val filters = Filters.WarehouseFilter(now, now, None, None)
      val key = WarehouseStateKey(ClientId(1), WarehouseStateRequest, filters)

      val expected =
        WarehouseUniqueCounters(
          autoruActive = 1,
          autoruInactive = 0,
          avitoActive = 0,
          avitoInactive = 0,
          dromActive = 0,
          dromInactive = 0,
          totalActive = 1,
          totalInactive = 0,
          totalRemoved = 0
        )

      for {
        _ <- Cache.set[WarehouseStateKey, String](key, "random value", None)
        _ <- WarehouseStateDao.upsertBatch(Seq(autoruActiveRecord))
        perDayState <- WarehouseStateDao.getUniqueCounters(ClientId(1), filters)
      } yield assert(perDayState)(equalTo(expected))
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

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    (suite("CachedWarehouseStateDao")(
      readStateFromCache,
      readUniqueCountersFromCache,
      fallbackToStorageStateOnCacheError,
      fallbackToStorageUniqueCountersOnCacheError
    ) @@ beforeAll(dbInit) @@ after(dbCleanup) @@ sequential).provideCustomLayerShared(
      (InMemoryCache.test ++ (TestPostgresql.managedTransactor >+> PgWarehouseStateDao.live)) >+>
        CachedWarehouseStateDao.wrap
    )

}
