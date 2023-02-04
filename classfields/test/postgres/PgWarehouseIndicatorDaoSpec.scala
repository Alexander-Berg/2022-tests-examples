package auto.dealers.dealer_stats.storage.test.postgres

import auto.dealers.dealer_stats.model.{ClientId, Indicator, IndicatorType}
import auto.dealers.dealer_stats.storage.dao.WarehouseIndicatorDao
import auto.dealers.dealer_stats.storage.postgres.PgWarehouseIndicatorDao
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.TestAspect.{after, beforeAll, sequential}
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.{Has, Task, URIO, ZIO}

import java.time.Instant

object PgWarehouseIndicatorDaoSpec extends DefaultRunnableSpec {

  /**
   * Из-за того, что когда возвращается список элементов наносекунды обрезаются, пришлось перевести время в millis
   * Пример:
   * Время в сущности до записи в базу    2021-12-23T11:30:08.199325310Z
   * Что мы видим, когда получаем из базы 2021-12-23T11:30:08.199325Z
   */

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PgWidgetWearhouseIndicationDao")(
      insertIndicator,
      insertIndicators,
      upsertIndicator,
      upsertIndicators,
      upsertIndicatorsOneValue,
      findByClientId
    ) @@
      beforeAll(dbInit) @@
      after(dbClean) @@
      sequential
  }.provideCustomLayerShared(TestPostgresql.managedTransactor >+> PgWarehouseIndicatorDao.live)

  private val insertIndicator = testM("should insert new indicator") {
    val clientId = ClientId(1L)
    val indicator = Indicator(
      clientId = clientId,
      indicatorType = IndicatorType.FastSell,
      indicatorValue = 10,
      modifiedAt = Instant.now()
    )
    for {
      _ <- WarehouseIndicatorDao.batchUpsert(List(indicator))
      indicators <- selectAll
    } yield assert(indicators.convertToAssert)(
      hasSameElements(List(indicator).convertToAssert)
    )
  }

  private val insertIndicators = testM("should insert new indicators") {
    val clientId = ClientId(1L)
    val fastSell = Indicator(
      clientId = clientId,
      indicatorType = IndicatorType.FastSell,
      indicatorValue = 10,
      modifiedAt = Instant.now()
    )

    val revaluation = Indicator(
      clientId = clientId,
      indicatorType = IndicatorType.Revaluation,
      indicatorValue = 15,
      modifiedAt = Instant.now()
    )
    val indicators = List(fastSell, revaluation)
    for {
      _ <- WarehouseIndicatorDao.batchUpsert(indicators)
      indicatorsIn <- selectAll
    } yield assert(indicatorsIn.convertToAssert)(hasSameElements(indicators.convertToAssert))
  }

  private val upsertIndicator = testM("should upsert new indicator") {
    val clientId = ClientId(1L)
    val indicator = Indicator(
      clientId = clientId,
      indicatorType = IndicatorType.FastSell,
      indicatorValue = 10,
      modifiedAt = Instant.now()
    )

    val newIndicator = indicator.copy(indicatorValue = 20, modifiedAt = Instant.now())
    for {
      _ <- WarehouseIndicatorDao.batchUpsert(List(indicator))
      _ <- WarehouseIndicatorDao.batchUpsert(List(newIndicator))
      indicators <- selectAll
    } yield assert(indicators.convertToAssert)(hasSameElements(List(newIndicator).convertToAssert))
  }

  private val upsertIndicators = testM("should upsert new indicators") {
    val clientId = ClientId(1L)
    val fastSell = Indicator(
      clientId = clientId,
      indicatorType = IndicatorType.FastSell,
      indicatorValue = 10,
      modifiedAt = Instant.now()
    )

    val revaluation = Indicator(
      clientId = clientId,
      indicatorType = IndicatorType.Revaluation,
      indicatorValue = 15,
      modifiedAt = Instant.now()
    )

    val indicators = List(fastSell, revaluation)

    val newIndicators = List(
      fastSell.copy(indicatorValue = 20, modifiedAt = Instant.now()),
      revaluation.copy(indicatorValue = 25, modifiedAt = Instant.now())
    )
    for {
      _ <- WarehouseIndicatorDao.batchUpsert(indicators)
      _ <- WarehouseIndicatorDao.batchUpsert(newIndicators)
      indicatorsIn <- selectAll
    } yield assert(indicatorsIn.convertToAssert)(hasSameElements(newIndicators.convertToAssert))
  }

  private val upsertIndicatorsOneValue = testM("should upsert new indicator of them") {
    val clientId = ClientId(1L)
    val fastSell = Indicator(
      clientId = clientId,
      indicatorType = IndicatorType.FastSell,
      indicatorValue = 10,
      modifiedAt = Instant.now()
    )

    val revaluation = Indicator(
      clientId = clientId,
      indicatorType = IndicatorType.Revaluation,
      indicatorValue = 15,
      modifiedAt = Instant.now()
    )

    val indicators = List(fastSell, revaluation)

    val newIndicators = List(
      revaluation,
      fastSell.copy(indicatorValue = 20, modifiedAt = Instant.now())
    )
    for {
      _ <- WarehouseIndicatorDao.batchUpsert(indicators)
      _ <- WarehouseIndicatorDao.batchUpsert(newIndicators)
      indicatorsIn <- selectAll
    } yield assert(indicatorsIn.convertToAssert)(hasSameElements(newIndicators.convertToAssert))
  }

  val findByClientId = testM("should find indicators by client id") {
    val clientId1 = ClientId(1L)
    val clientId2 = ClientId(2L)
    val fastSellClientId1 = Indicator(
      clientId = clientId1,
      indicatorType = IndicatorType.FastSell,
      indicatorValue = 10,
      modifiedAt = Instant.now()
    )
    val revaluationClientId1 = Indicator(
      clientId = clientId1,
      indicatorType = IndicatorType.Revaluation,
      indicatorValue = 20,
      modifiedAt = Instant.now()
    )

    val fastSellClientId2 = Indicator(
      clientId = clientId2,
      indicatorType = IndicatorType.FastSell,
      indicatorValue = 15,
      modifiedAt = Instant.now()
    )

    val indicators = List(fastSellClientId1, revaluationClientId1, fastSellClientId2)
    val expectedIndicators = List(fastSellClientId1, revaluationClientId1)

    for {
      _ <- WarehouseIndicatorDao.batchUpsert(indicators)
      indicators <- WarehouseIndicatorDao.findByClientId(clientId1)
    } yield assert(indicators.map(_.convertToAssert))(hasSameElements(expectedIndicators.map(_.convertToAssert)))
  }

  private val dbInit: URIO[Has[doobie.Transactor[Task]], Unit] = ZIO
    .service[Transactor[Task]]
    .flatMap(InitSchema("/schema.sql", _))
    .orDie

  private val selectAll = ZIO
    .service[Transactor[Task]]
    .flatMap { xa =>
      sql"select * from warehouse_indicator".query[Indicator].to[List].transact(xa)
    }

  private val dbClean = ZIO
    .service[Transactor[Task]]
    .flatMap { xa =>
      sql"truncate warehouse_indicator".update.run.transact(xa)
    }

  implicit class RichIndicators(val indicators: List[Indicator]) extends AnyVal {

    def convertToAssert =
      indicators.map(_.convertToAssert)
  }

  implicit class RichIndicator(val indicator: Indicator) extends AnyVal {

    def convertToAssert =
      (indicator.clientId, indicator.indicatorType, indicator.indicatorValue, indicator.modifiedAt.toEpochMilli)
  }

}
