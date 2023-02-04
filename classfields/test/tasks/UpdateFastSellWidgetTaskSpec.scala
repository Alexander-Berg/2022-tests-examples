package auto.dealers.dealer_stats.test.tasks

import auto.dealers.dealer_stats.model.{ClientId, Indicator, IndicatorType}
import auto.dealers.dealer_stats.scheduler.tasks.UpdateFastSellWidgetTask
import auto.dealers.dealer_stats.storage.testkit.{InMemoryIndicatorCalculationDao, InMemoryWarehouseIndicatorDao}
import common.zio.logging.Logging
import zio.test._
import zio.test.Assertion._

import java.time.Instant

object UpdateFastSellWidgetTaskSpec extends DefaultRunnableSpec {
  val task = new UpdateFastSellWidgetTask()

  val checkProcessingOfNonEmptyResult = testM("process non empty list of indicators") {
    val indicators = List(
      Indicator(
        clientId = ClientId(1),
        indicatorType = IndicatorType.FastSell,
        indicatorValue = 42,
        modifiedAt = Instant.now()
      )
    )
    val ytDao = InMemoryIndicatorCalculationDao.inMemoryFastSell(indicators)
    val pgDao = InMemoryWarehouseIndicatorDao.test

    (for {
      _ <- task.program
      result <- InMemoryWarehouseIndicatorDao.dump
    } yield assert(result)(equalTo(indicators)))
      .provideCustomLayer(ytDao ++ pgDao ++ Logging.live)
  }

  val checkProcessingOfEmptyResult = testM("process empty list of indicators") {
    val ytDao = InMemoryIndicatorCalculationDao.inMemoryFastSell(List.empty)
    val pgDao = InMemoryWarehouseIndicatorDao.test

    (for {
      _ <- task.program
      result <- InMemoryWarehouseIndicatorDao.dump
    } yield assertTrue(result.isEmpty))
      .provideCustomLayer(ytDao ++ pgDao ++ Logging.live)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("UpdateFastSellWidgetTask")(
    checkProcessingOfNonEmptyResult,
    checkProcessingOfEmptyResult
  )
}
