package etc.dust.api.testkit

import common.zio.grpc.client.{GrpcClient, GrpcClientLive}
import common.zio.grpc.client.GrpcClient.GrpcClient
import io.grpc.stub.StreamObserver
import vertis.dust.dialogs_api._
import vertis.dust.dialogs_api.DialogsClusteringServiceGrpc.DialogsClusteringService
import vertis.dust.model._
import zio._

import scala.concurrent.Future

object TestDialogService {
  type TestDialogService = Has[Service]

  trait Service {
    def dialogs: Ref[Map[DialogId, DialogView]]
  }

  def dialogs: URIO[TestDialogService, Ref[Map[DialogId, DialogView]]] = ZIO.access[TestDialogService](_.get.dialogs)

  case class Stub(runtime: Runtime[Any], dialogs: Ref[Map[DialogId, DialogView]])
    extends DialogsClusteringService
    with Service {

    override def startDialogClustering(request: StartClusteringRequest): Future[StartClusteringResponse] =
      Future.successful(StartClusteringResponse(clusterTypes = request.clusterTypes))

    override def getClusteringResults(request: ClusteringResultsRequest): Future[ClusteringResultsResponse] =
      Future.successful(ClusteringResultsResponse())

    override def getClusterDialogs(
        request: ClusterDialogsRequest,
        responseObserver: StreamObserver[ClusterDialogsBatch]): Unit =
      runtime.unsafeRun {
        dialogs.get.map { dialogsMap =>
          responseObserver.onNext(ClusterDialogsBatch(dialogs = dialogsMap.values.toSeq))
          responseObserver.onCompleted()
        }
      }

    override def getClusterMeta(request: GetClusterMetaRequest): Future[GetClusterMetaResponse] = ???
  }

  val layer: ULayer[GrpcClient[DialogsClusteringService] with TestDialogService] = (for {
    dialogs <- Ref.make(Map.empty[DialogId, DialogView])
    runtime <- ZIO.runtime[Any]
    stub = Stub(runtime, dialogs)
    grpc = new GrpcClientLive[DialogsClusteringService](stub, null)
  } yield Has.allOf[GrpcClient.Service[DialogsClusteringService], Service](grpc, stub)).toLayerMany

}
