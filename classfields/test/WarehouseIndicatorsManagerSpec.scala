package auto.dealers.dealer_stats.logic

import auto.dealers.dealer_stats.model.{
  ClientId,
  Indicator,
  IndicatorLimitConfig,
  IndicatorType,
  WarehouseIndicatorsConfig
}
import auto.dealers.dealer_stats.storage.dao.WarehouseIndicatorDao
import auto.dealers.dealer_stats.storage.testkit.InMemoryWarehouseIndicatorDao
import common.scalapb.ScalaProtobuf.instantToTimestamp
import ru.auto.dealer_stats.proto.rpc.{
  DealerWarehouseIndicatorsResponse,
  FastSellIndicator,
  IndicatorColor,
  RevaluationIndicator,
  ToxicStockIndicator
}
import zio.ZIO
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test._
import zio.test.Assertion._

import java.time.Instant

object WarehouseIndicatorsManagerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("WarehouseIndicatorsManager")(
      convertToIndicators,
      getDealerWarehouseIndicatorsAll,
      getDealerWarehouseIndicatorsSomeOfThem
    )

  val convertToIndicators = test("should convert to widget indicators") {
    val clientId1 = ClientId(1)

    val indicators = List(
      Indicator(
        clientId = clientId1,
        indicatorType = IndicatorType.ToxicStock,
        indicatorValue = 15,
        modifiedAt = modifiedAt
      ),
      Indicator(
        clientId = clientId1,
        indicatorType = IndicatorType.FastSell,
        indicatorValue = 6,
        modifiedAt = modifiedAt
      ),
      Indicator(
        clientId = clientId1,
        indicatorType = IndicatorType.Revaluation,
        indicatorValue = 0,
        modifiedAt = modifiedAt
      )
    )

    val expectedRevaluation = RevaluationIndicator(
      score = "0",
      color = IndicatorColor.GREEN,
      modifiedAt = Some(modifiedAtTimestamp)
    )
    val expectedToxicStock = ToxicStockIndicator(
      percent = 15,
      color = IndicatorColor.YELLOW,
      modifiedAt = Some(modifiedAtTimestamp)
    )
    val expectedFastSell = FastSellIndicator(
      percent = 6,
      color = IndicatorColor.RED,
      modifiedAt = Some(modifiedAtTimestamp)
    )

    val resultFastSell = WarehouseIndicatorsManager.getIndicator(indicators, IndicatorType.FastSell)(
      WarehouseIndicatorsManager.toFastSellColorIndicator(fastSellConfig)
    )
    val resultRevaluation = WarehouseIndicatorsManager.getIndicator(indicators, IndicatorType.Revaluation)(
      WarehouseIndicatorsManager.toRevaluationColorIndicator(revaluationConfig)
    )
    val resultToxicStock = WarehouseIndicatorsManager.getIndicator(indicators, IndicatorType.ToxicStock)(
      WarehouseIndicatorsManager.toToxicStockColorIndicator(toxicStockConfig)
    )

    assertTrue(resultRevaluation.contains(expectedRevaluation)) &&
    assertTrue(resultFastSell.contains(expectedFastSell)) &&
    assertTrue(resultToxicStock.contains(expectedToxicStock))
  }

  val getDealerWarehouseIndicatorsAll = testM("should return all indicators for client") {
    val clientId1 = ClientId(1)

    val indicators = List(
      Indicator(
        clientId = clientId1,
        indicatorType = IndicatorType.ToxicStock,
        indicatorValue = 30,
        modifiedAt = modifiedAt
      ),
      Indicator(
        clientId = clientId1,
        indicatorType = IndicatorType.FastSell,
        indicatorValue = 2,
        modifiedAt = modifiedAt
      ),
      Indicator(
        clientId = clientId1,
        indicatorType = IndicatorType.Revaluation,
        indicatorValue = 2,
        modifiedAt = modifiedAt
      )
    )

    val expectedRevaluation = RevaluationIndicator(
      score = "2",
      color = IndicatorColor.ORANGE,
      modifiedAt = Some(modifiedAtTimestamp)
    )
    val expectedToxicStock = ToxicStockIndicator(
      percent = 30,
      color = IndicatorColor.RED,
      modifiedAt = Some(modifiedAtTimestamp)
    )
    val expectedFastSell = FastSellIndicator(
      percent = 2,
      color = IndicatorColor.GREEN,
      modifiedAt = Some(modifiedAtTimestamp)
    )

    val expectedWidget = DealerWarehouseIndicatorsResponse(
      fastSell = Some(expectedFastSell),
      revaluation = Some(expectedRevaluation),
      toxicStock = Some(expectedToxicStock)
    )

    {
      for {
        _ <- WarehouseIndicatorDao.batchUpsert(indicators)
        response <- WarehouseIndicatorsManager.getDealerWarehouseIndicators(clientId1)
      } yield assertTrue(response == expectedWidget)
    }.provideCustomLayer(
      InMemoryWarehouseIndicatorDao.test ++ ZIO.succeed(warehouseConfig).toLayer >+> WarehouseIndicatorsManager.live
    )
  }

  val getDealerWarehouseIndicatorsSomeOfThem = testM("should return some indicators for client") {
    val clientId1 = ClientId(1)
    val clientId2 = ClientId(2)

    val indicators = List(
      Indicator(
        clientId = clientId1,
        indicatorType = IndicatorType.ToxicStock,
        indicatorValue = 5,
        modifiedAt = modifiedAt
      ),
      Indicator(
        clientId = clientId1,
        indicatorType = IndicatorType.FastSell,
        indicatorValue = 5,
        modifiedAt = modifiedAt
      ),
      Indicator(
        clientId = clientId2,
        indicatorType = IndicatorType.Revaluation,
        indicatorValue = 5,
        modifiedAt = modifiedAt
      )
    )

    val expectedRevaluation = RevaluationIndicator(
      score = "",
      color = IndicatorColor.GRAY,
      modifiedAt = None
    )
    val expectedToxicStock = ToxicStockIndicator(
      percent = 5,
      color = IndicatorColor.GREEN,
      modifiedAt = Some(modifiedAtTimestamp)
    )
    val expectedFastSell = FastSellIndicator(
      percent = 5,
      color = IndicatorColor.YELLOW,
      modifiedAt = Some(modifiedAtTimestamp)
    )

    {
      for {
        _ <- WarehouseIndicatorDao.batchUpsert(indicators)
        response <- WarehouseIndicatorsManager.getDealerWarehouseIndicators(clientId1)
      } yield assertTrue(response.getFastSell == expectedFastSell) &&
        assertTrue(response.getToxicStock == expectedToxicStock) &&
        assertTrue(response.getRevaluation == expectedRevaluation)
    }.provideCustomLayer(
      InMemoryWarehouseIndicatorDao.test ++ ZIO.succeed(warehouseConfig).toLayer >+> WarehouseIndicatorsManager.live
    )
  }

  private val modifiedAt = Instant.now()
  private val modifiedAtTimestamp = instantToTimestamp(modifiedAt)

  private val toxicStockConfig = IndicatorLimitConfig(
    greenLimit = 10,
    yellowLimit = 20,
    orangeLimit = None,
    redLimit = 100
  )

  private val fastSellConfig = IndicatorLimitConfig(
    greenLimit = 3,
    yellowLimit = 5,
    orangeLimit = None,
    redLimit = 100
  )

  private val revaluationConfig = IndicatorLimitConfig(
    greenLimit = 0,
    yellowLimit = 1,
    orangeLimit = Some(2),
    redLimit = 3
  )

  private val warehouseConfig = WarehouseIndicatorsConfig(
    toxicStockLimit = toxicStockConfig,
    fastSellLimit = fastSellConfig,
    revaluationLimit = revaluationConfig
  )
}
