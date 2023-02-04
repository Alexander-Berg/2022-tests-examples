package auto.dealers.dealer_stats.test.tasks

import auto.dealers.dealer_stats.model.{ClientId, Indicator, IndicatorType}
import auto.dealers.dealer_stats.scheduler.tasks.UpdateRevaluationWidgetTask
import auto.dealers.dealer_stats.storage.testkit.{InMemoryIndicatorCalculationDao, InMemoryWarehouseIndicatorDao}
import zio.test._
import zio.test.Assertion._

import java.time.Instant

object UpdateRevaluationWidgetTaskSpec extends DefaultRunnableSpec {
  val task = new UpdateRevaluationWidgetTask()

  val checkProcessingOfNonEmptyResult = testM("process non empty list of indicators") {
    val indicators = List(
      Indicator(
        clientId = ClientId(1),
        indicatorType = IndicatorType.Revaluation,
        indicatorValue = 1,
        modifiedAt = Instant.now()
      )
    )
    val ytDao = InMemoryIndicatorCalculationDao.inMemoryRevaluations(indicators)
    val pgDao = InMemoryWarehouseIndicatorDao.test

    (for {
      _ <- task.program
      result <- InMemoryWarehouseIndicatorDao.dump
    } yield assert(result)(equalTo(indicators)))
      .provideCustomLayer(ytDao ++ pgDao)
  }

  val checkProcessingOfEmptyResult = testM("process empty list of indicators") {
    val ytDao = InMemoryIndicatorCalculationDao.inMemoryRevaluations(List.empty)
    val pgDao = InMemoryWarehouseIndicatorDao.test

    (for {
      _ <- task.program
      result <- InMemoryWarehouseIndicatorDao.dump
    } yield assertTrue(result.isEmpty))
      .provideCustomLayer(ytDao ++ pgDao)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("UpdateRevaluationWidgetTask")(
    checkProcessingOfNonEmptyResult,
    checkProcessingOfEmptyResult
  )
}
