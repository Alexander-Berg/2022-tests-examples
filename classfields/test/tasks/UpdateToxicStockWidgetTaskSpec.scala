package auto.dealers.dealer_stats.test.tasks

import auto.dealers.dealer_stats.model.{ClientId, Indicator, IndicatorType}
import auto.dealers.dealer_stats.scheduler.tasks.UpdateToxicStockWidgetTask
import auto.dealers.dealer_stats.storage.testkit.{InMemoryIndicatorCalculationDao, InMemoryWarehouseIndicatorDao}
import common.zio.logging.Logging
import zio.test._
import zio.test.Assertion._

import java.time.Instant

object UpdateToxicStockWidgetTaskSpec extends DefaultRunnableSpec {

  val task = new UpdateToxicStockWidgetTask()

  val toxicStockNonEmpty = testM("process non empty toxic stock indicators") {
    val indicators = List(
      Indicator(
        clientId = ClientId(1),
        indicatorType = IndicatorType.ToxicStock,
        indicatorValue = 10,
        modifiedAt = Instant.now()
      )
    )

    val ytDao = InMemoryIndicatorCalculationDao.inMemoryToxicStocks(indicators)
    val postgresDao = InMemoryWarehouseIndicatorDao.test

    {
      for {
        _ <- task.program
        result <- InMemoryWarehouseIndicatorDao.dump
      } yield assert(result)(equalTo(indicators))
    }.provideCustomLayer(Logging.live ++ ytDao ++ postgresDao)

  }

  val toxicStockNil = testM("process empty toxic stock indicators") {
    val ytDao = InMemoryIndicatorCalculationDao.inMemoryToxicStocks(List.empty)
    val postgresDao = InMemoryWarehouseIndicatorDao.test

    {
      for {
        _ <- task.program
        result <- InMemoryWarehouseIndicatorDao.dump
      } yield assert(result)(isEmpty)
    }.provideCustomLayer(Logging.live ++ ytDao ++ postgresDao)

  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("UpdateToxicStockWidgetTask")(
    toxicStockNonEmpty,
    toxicStockNil
  )
}
