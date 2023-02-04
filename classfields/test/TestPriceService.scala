package ru.yandex.vertis.billing.emon.consumer

import billing.howmuch.price_service.PriceServiceGrpc.PriceService
import billing.howmuch.price_service.{GetPricesRequest, GetPricesResponse, PatchPricesRequest}
import zio.{Has, Ref, Runtime, Task, UIO, ULayer, ZIO, ZRef}
import com.google.protobuf.empty.Empty
import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.{GrpcClient, GrpcClientLive}
import zio.macros.accessible

import scala.concurrent.Future

@accessible
object TestPriceService {

  type TestPriceService = Has[Service]

  trait Service {
    def setPriceResponse(response: GetPricesRequest => Task[GetPricesResponse]): UIO[Unit]
  }

  private class ServiceImpl(effectRef: Ref[GetPricesRequest => Task[GetPricesResponse]]) extends Service {

    override def setPriceResponse(response: GetPricesRequest => Task[GetPricesResponse]): UIO[Unit] =
      effectRef.set(response)
  }

  private class PriceServiceImpl(
      runtime: Runtime[Any],
      effectRef: Ref[GetPricesRequest => Task[GetPricesResponse]])
    extends PriceService {

    override def getPrices(request: GetPricesRequest): Future[GetPricesResponse] =
      runtime.unsafeRunToFuture(effectRef.get.map(_(request)).flatten)

    override def patchPrices(request: PatchPricesRequest): Future[Empty] = ???
  }

  val layer: ULayer[GrpcClient[PriceService] with TestPriceService] = {
    val effect =
      for {
        runtime <- ZIO.succeed(zio.Runtime.default)
        effectRef <- ZRef.make[GetPricesRequest => Task[GetPricesResponse]](_ => ZIO.succeed(GetPricesResponse()))
        responseSetter: Service = new ServiceImpl(effectRef)
        grpcClient: GrpcClient.Service[PriceService] = new GrpcClientLive[PriceService](
          new PriceServiceImpl(runtime, effectRef),
          null
        )
      } yield Has.allOf(responseSetter, grpcClient)
    effect.toLayerMany
  }

}
