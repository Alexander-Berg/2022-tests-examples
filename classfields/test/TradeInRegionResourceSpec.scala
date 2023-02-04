package auto.dealers.trade_in_notifier.storage.test

import common.palma.Palma
import common.zio.resources.{DefaultResourceLoader, ResourceLoader}
import common.zio.logging.Logging
import auto.dealers.trade_in_notifier.storage.TradeInRegionRepository
import auto.dealers.trade_in_notifier.storage.TradeInRegionRepository.TradeInRegionRepository
import auto.dealers.trade_in_notifier.storage.resources.TradeInRegionResource
import auto.dealers.trade_in_notifier.storage.resources.TradeInRegionResource.TradeInRegionResourceConfig
import auto.dealers.trade_in_notifier.storage.testkit.TradeInRegionPalmaMock
import ru.auto.trade_in_notifier.palma.proto.region_palma_model.Open_for
import zio.clock.Clock
import zio.test.Assertion._
import zio.test._
import zio.{Has, ZIO, ZLayer}

import scala.concurrent.duration._

object TradeInRegionResourceSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    suite("TradeInRegionResourceSpec")(
      testM("get true - if Palma has TradeInRegion == 1") {
        for {
          res <- TradeInRegionRepository.exists(regionId => ZIO.succeed(regionId.id == 1))
        } yield assert(res)(equalTo(true))
      },
      testM("get false - if Palm doesn't have TradeInRegion == -1") {
        for {
          res <- TradeInRegionRepository.exists(regionId => ZIO.succeed(regionId.id == -1))
        } yield assert(res)(equalTo(false))
      },
      testM("get list of regions") {
        for {
          res <- TradeInRegionRepository.get
        } yield assert(res)(hasSameElements(Seq(1, 2, 3, 4, 5, 6, 7)))
      }
    ).provideCustomLayerShared(createEnvironment)
  }

  private def createEnvironment: ZLayer[Any, TestFailure[RuntimeException], TradeInRegionRepository] = {
    val palma: ZLayer[Any, Palma.PalmaError, Has[Palma.Service]] =
      TradeInRegionPalmaMock.layer
    val config: ZLayer[Any, Nothing, Has[TradeInRegionResourceConfig]] =
      ZIO.succeed(TradeInRegionResourceConfig(2.seconds)).toLayer
    val resourceLoader: ZLayer[Any, Nothing, Has[ResourceLoader.Service]] =
      Clock.live ++ Logging.live >>> ZLayer.fromManaged(DefaultResourceLoader.create)

    val layer =
      environment.testEnvironment ++ resourceLoader ++ config ++ palma ++ Logging.live >>>
        TradeInRegionResource.live(Open_for.TRADE_IN)

    layer.mapError(TestFailure.fail)
  }
}
