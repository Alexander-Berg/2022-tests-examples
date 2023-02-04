package auto.dealers.loyalty.logic.test.placementdiscountservice

import auto.dealers.loyalty.logic.PlacementDiscountServiceLive.calculateStockAverage
import zio.test.environment.TestEnvironment
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}

object CalculateStockAverageSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = suite("calculateStockAverage")(
    test("Zero if no stock data") {
      val stockAvg = calculateStockAverage(Seq.empty)
      assertTrue(stockAvg == 0)
    },
    test("Zero if all stock is zero") {
      val stockAvg = calculateStockAverage(Seq.fill(30)(0))
      assertTrue(stockAvg == 0)
    },
    test("Stock is rounded up") {
      val stockData = Seq(44, 44, 45, 45)
      val stockAvg = calculateStockAverage(stockData)
      assertTrue(stockAvg == 45)
    },
    test("Example") {
      val stockData = Seq(0, 30, 47, 16, 0, 33, 40, 0, 0, 39)
      val stockAvg = calculateStockAverage(stockData)
      assertTrue(stockAvg == (30 + 47 + 16 + 33 + 40 + 39) / 6 + 1) // +1 для округления вверх
    }
  )
}
