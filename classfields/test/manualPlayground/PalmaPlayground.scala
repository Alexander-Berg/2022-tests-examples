package auto.dealers.loyalty.storage.test.manualPlayground

import auto.dealers.loyalty.storage.clients.DiscountSettings
import common.palma.Palma
import common.zio.app.BaseApp
import common.zio.app.BaseApp.BaseEnvironment
import common.zio.grpc.client.{GrpcClientConfig, GrpcClientLive}
import common.zio.logging.Logging
import ru.yandex.vertis.palma.services.proto_dictionary_service.ProtoDictionaryServiceGrpc
import zio.{Has, ZIO, ZLayer}

object PalmaPlayground extends BaseApp {
  override type Env = Has[DiscountSettings] with Logging.Logging

  private val grpcClient = (
    ZLayer.succeed(GrpcClientConfig("palma-api-grpc-api.vrts-slb.test.vertis.yandex.net")) ++
      ZLayer.requires[BaseEnvironment]
  ) >>> GrpcClientLive.operated(
    ProtoDictionaryServiceGrpc.stub(_): ProtoDictionaryServiceGrpc.ProtoDictionaryService
  )

  override def makeEnv: ZLayer[BaseEnvironment, Throwable, Env] =
    (grpcClient >>> Palma.live >>> DiscountSettings.live) ++ Logging.any

  override def program: ZIO[Env, Throwable, Any] = {
    for {
      _ <- Logging.info("Start to load polices.")
      all <- DiscountSettings(_.getAllPolicies())
      _ <- Logging.info("All policies:\n" + all.map(_.toProtoString).mkString("\n"))
    } yield ()

  }
}
