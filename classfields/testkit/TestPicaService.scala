package ru.yandex.vertis.general.gost.stages.photo.logic.testkit

import com.google.protobuf.empty.Empty
import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.GrpcClientLive
import ru.yandex.vertis.pica.model.model.ProcessingResult
import ru.yandex.vertis.pica.service.pica_service.ApiModel
import ru.yandex.vertis.pica.service.pica_service.ApiModel.GetOrPutRequest
import ru.yandex.vertis.pica.service.pica_service.PicaServiceGrpc.PicaService
import zio._

import scala.concurrent.Future

object TestPicaService {

  val layer: URLayer[Has[Map[GetOrPutRequest, ProcessingResult]], GrpcClient[PicaService]] = {
    for {
      testData <- ZIO.service[Map[GetOrPutRequest, ProcessingResult]]
      state <- ZRef.make[Map[GetOrPutRequest, ProcessingResult]](testData)
      runtime <- ZIO.runtime[Any]
      getOrPutPhoto = (req: GetOrPutRequest) => state.get.map(_.getOrElse(req, ProcessingResult.defaultInstance))
      grpcClient = new GrpcClientLive[PicaService](new PicaServiceImpl(runtime, getOrPutPhoto), null)
    } yield grpcClient
  }.toLayer

  private class PicaServiceImpl(runtime: Runtime[Any], getOrPutPhoto: GetOrPutRequest => UIO[ProcessingResult])
    extends PicaService {

    override def getOrPut(request: GetOrPutRequest): Future[ProcessingResult] =
      runtime.unsafeRunToFuture {
        getOrPutPhoto(request)
      }

    override def get(request: ApiModel.GetRequest): Future[ProcessingResult] =
      Future.successful(ProcessingResult.defaultInstance)

    override def reloadMeta(request: ApiModel.ReloadMetaRequest): Future[Empty] =
      Future.successful(Empty.defaultInstance)

    override def delete(request: ApiModel.DeleteRequest): Future[Empty] = Future.successful(Empty.defaultInstance)
  }
}
