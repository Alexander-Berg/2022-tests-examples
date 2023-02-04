package auto.dealers.dealer_stats.storage.testkit

import auto.dealers.dealer_stats.model.Indicator
import auto.dealers.dealer_stats.storage.dao.IndicatorCalculationDao
import auto.dealers.dealer_stats.storage.dao.IndicatorCalculationDao.IndicatorCalculationDao
import zio.{Has, Task, ULayer, ZLayer}

class InMemoryIndicatorCalculationDao(
    revaluationIndicators: List[Indicator] = List.empty,
    toxicStockIndicators: List[Indicator] = List.empty,
    fastSellIndicators: List[Indicator] = List.empty)
  extends IndicatorCalculationDao.Service {
  override def revaluations(): Task[List[Indicator]] = Task.succeed(revaluationIndicators)
  override def toxicStocks: Task[List[Indicator]] = Task.succeed(toxicStockIndicators)
  override def fastSellIndicators: Task[List[Indicator]] = Task.succeed(fastSellIndicators)
}

object InMemoryIndicatorCalculationDao {

  def inMemoryRevaluations(revaluationIndicators: List[Indicator]): ULayer[IndicatorCalculationDao] =
    ZLayer.succeed(new InMemoryIndicatorCalculationDao(revaluationIndicators = revaluationIndicators))

  def inMemoryToxicStocks(toxicStockIndicators: List[Indicator]): ULayer[IndicatorCalculationDao] =
    ZLayer.succeed(new InMemoryIndicatorCalculationDao(toxicStockIndicators = toxicStockIndicators))

  def inMemoryFastSell(fastSellIndicators: List[Indicator]): ULayer[IndicatorCalculationDao] =
    ZLayer.succeed(new InMemoryIndicatorCalculationDao(fastSellIndicators = fastSellIndicators))

}
