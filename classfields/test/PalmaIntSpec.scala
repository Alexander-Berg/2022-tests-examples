package common.palma.test

import common.palma._
import common.zio.grpc.client.GrpcClientLive
import common.zio.grpc.client.GrpcClientLive
import common.zio.grpc.client.GrpcClientConfig
import common.zio.grpc.client.GrpcClientConfig
import ru.yandex.vertis.palma.services.proto_dictionary_service.ProtoDictionaryServiceGrpc

import zio.ZLayer
import zio.magic._

object PalmaIntSpec extends BasePalmaSpec {
  def config = ZLayer.succeed(GrpcClientConfig("palma-api-grpc-api.vrts-slb.test.vertis.yandex.net:80"))

  def grpcClient =
    GrpcClientLive.live(ProtoDictionaryServiceGrpc.stub(_): ProtoDictionaryServiceGrpc.ProtoDictionaryService)
  def palma = Palma.live

  def layer = ZLayer.fromMagic[Palma.Palma](
    config,
    grpcClient,
    palma
  )
}
