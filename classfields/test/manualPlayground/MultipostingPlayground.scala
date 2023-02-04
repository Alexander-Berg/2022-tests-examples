package auto.dealers.loyalty.storage.test.manualPlayground

import auto.dealers.loyalty.storage.clients.DealerWarehouse
import common.zio.app.BaseApp
import common.zio.app.BaseApp.BaseEnvironment
import common.zio.grpc.client.{GrpcClientConfig, GrpcClientLive}
import common.zio.logging.Logging
import ru.auto.multiposting.warehouse_service.WarehouseServiceGrpc
import zio._

import java.time.LocalDate

object MultipostingPlayground extends BaseApp {
  override type Env = Logging.Logging with Has[DealerWarehouse]

  private val multipostingConfig = GrpcClientConfig("multiposting-api-grpc.vrts-slb.test.vertis.yandex.net")

  override def makeEnv: URLayer[BaseEnvironment, Env] = {
    ZLayer.requires[BaseEnvironment] ++ (ZLayer.succeed(multipostingConfig) >>>
      GrpcClientLive.live[WarehouseServiceGrpc.WarehouseService](WarehouseServiceGrpc.stub) >>>
      DealerWarehouse.live)
  }

  override def program: ZIO[Env, Throwable, Any] = for {
    list <- DealerWarehouse(_.getActiveStockByDay(20101L, LocalDate.now().minusDays(10), LocalDate.now().plusDays(5)))
    _ <- Logging.info(s"Warehouse:\n$list")
  } yield ()
}
