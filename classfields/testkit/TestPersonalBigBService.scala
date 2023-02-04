package ru.yandex.vertis.general.personal.testkit

import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.{GrpcClient, GrpcClientLive}
import general.personal.bigb_api.{GetUserInfoRequest, GetUserInfoResponse}
import general.personal.bigb_api.PersonalBigBServiceGrpc.PersonalBigBService
import zio.{Has, Ref, Runtime, Task, UIO, ZIO, ZLayer, ZRef}
import zio.macros.accessible

import scala.concurrent.Future

@accessible
object TestPersonalBigBService {

  type TestPersonalBigBService = Has[Service]

  trait Service {
    def setUserInfoResponse(response: GetUserInfoRequest => Task[GetUserInfoResponse]): UIO[Unit]
  }

  val layer: ZLayer[Any, Nothing, TestPersonalBigBService with GrpcClient[PersonalBigBService]] = {
    val creationEffect = for {
      runtime <- ZIO.runtime[Any]
      listNearRegionsEffectRef <- ZRef.make[GetUserInfoRequest => Task[GetUserInfoResponse]] { _ =>
        ZIO.succeed(GetUserInfoResponse())
      }

      responseSetter: Service = new ServiceImpl(
        listNearRegionsEffectRef
      )

      grpcClient: GrpcClient.Service[PersonalBigBService] = new GrpcClientLive[PersonalBigBService](
        new PersonalBigBServiceImpl(
          runtime,
          listNearRegionsEffectRef
        ),
        null
      )
    } yield Has.allOf(responseSetter, grpcClient)
    creationEffect.toLayerMany
  }

  private class ServiceImpl(
      userInfoEffectRef: Ref[GetUserInfoRequest => Task[GetUserInfoResponse]])
    extends Service {

    override def setUserInfoResponse(response: GetUserInfoRequest => Task[GetUserInfoResponse]): UIO[Unit] =
      userInfoEffectRef.set(response)

  }

  private class PersonalBigBServiceImpl(
      runtime: Runtime[Any],
      getUserInfoRef: Ref[GetUserInfoRequest => Task[GetUserInfoResponse]])
    extends PersonalBigBService {

    override def getUserInfo(request: GetUserInfoRequest): Future[GetUserInfoResponse] = {
      runtime.unsafeRunToFuture(getUserInfoRef.get.flatMap(_(request)))
    }
  }
}
