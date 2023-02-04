package ru.yandex.vertis.general.favorites.testkit

import com.google.protobuf.empty.Empty
import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.{GrpcClient, GrpcClientLive}
import ru.yandex.vertis.spamalot.service.{CancelRequest, MassSendRequest, SendRequest, SendResponse}
import ru.yandex.vertis.spamalot.service.SendingServiceGrpc.SendingService
import zio.{Has, Ref, Runtime, Task, UIO, ULayer, ZIO, ZRef}

import scala.concurrent.Future
import zio.macros.accessible

@accessible
object TestSendingService {

  type TestSendingService = Has[Service]

  trait Service {
    def setSendResponse(response: SendRequest => Task[SendResponse]): UIO[Unit]
  }

  private class ServiceImpl(
      sendEffectRef: Ref[SendRequest => Task[SendResponse]])
    extends Service {
    override def setSendResponse(response: SendRequest => Task[SendResponse]): UIO[Unit] = sendEffectRef.set(response)
  }

  private class SendingServiceImpl(
      runtime: Runtime[Any],
      sendEffectRef: Ref[SendRequest => Task[SendResponse]])
    extends SendingService {

    override def send(request: SendRequest): Future[SendResponse] =
      runtime.unsafeRunToFuture(sendEffectRef.get.map(_(request)).flatten)

    override def massSend(request: MassSendRequest): Future[Empty] = ???

    override def cancel(request: CancelRequest): Future[Empty] = ???
  }

  val layer: ULayer[GrpcClient[SendingService] with TestSendingService] = {
    val effect =
      for {
        runtime <- ZIO.succeed(zio.Runtime.default)
        sendRef <- ZRef.make[SendRequest => Task[SendResponse]](_ => ZIO.succeed(SendResponse()))
        responseSetter: Service = new ServiceImpl(sendRef)
        grpcClient: GrpcClient.Service[SendingService] = new GrpcClientLive[SendingService](
          new SendingServiceImpl(runtime, sendRef),
          null
        )
      } yield Has.allOf(responseSetter, grpcClient)
    effect.toLayerMany
  }

}
