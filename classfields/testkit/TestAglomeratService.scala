package ru.yandex.vertis.general.aglomerat.testkit

import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.{GrpcClient, GrpcClientLive}
import general.aglomerat.api.AglomeratServiceGrpc.AglomeratService
import general.aglomerat.api._
import zio.macros.accessible
import zio.{Has, Ref, Runtime, Task, UIO, ULayer, ZIO, ZRef}

import scala.concurrent.Future

@accessible
object TestAglomeratService {
  type TestAglomeratService = Has[Service]

  trait Service {
    def setGetClustersResponse(response: GetClustersRequest => Task[GetClustersResponse]): UIO[Unit]
    def setGetOffersResponse(response: GetOffersRequest => Task[GetOffersResponse]): UIO[Unit]
    def setGetClusteringMetaResponse(response: GetClusteringMetaRequest => Task[GetClusteringMetaResponse]): UIO[Unit]

    def setGetClusteringAggregationResponse(
        response: GetClusteringAggregationRequest => Task[GetClusteringAggregationResponse]
      ): UIO[Unit]
    def setBindOfferResponse(response: BindOfferRequest => Task[BindOfferResponse]): UIO[Unit]
    def setUnbindOfferResponse(response: UnbindOfferRequest => Task[UnbindOfferResponse]): UIO[Unit]
  }

  val layer: ULayer[GrpcClient[AglomeratService] with TestAglomeratService] = {
    val creationEffect = for {
      runtime <- ZIO.runtime[Any]
      getClustersEffectRef <- ZRef.make[GetClustersRequest => Task[GetClustersResponse]] { request =>
        ZIO.succeed(GetClustersResponse())
      }
      getOffersEffectRef <- ZRef.make[GetOffersRequest => Task[GetOffersResponse]] { _ =>
        ZIO.succeed(GetOffersResponse())
      }
      getClusteringMetaEffectRef <- ZRef.make[GetClusteringMetaRequest => Task[GetClusteringMetaResponse]] { _ =>
        ZIO.succeed(GetClusteringMetaResponse())
      }
      getClusteringAggregationEffectRef <- ZRef
        .make[GetClusteringAggregationRequest => Task[GetClusteringAggregationResponse]] { _ =>
          ZIO.succeed(GetClusteringAggregationResponse())
        }
      bindOfferEffectRef <- ZRef.make[BindOfferRequest => Task[BindOfferResponse]] { _ =>
        ZIO.succeed(BindOfferResponse())
      }
      unbindOfferEffectRef <- ZRef.make[UnbindOfferRequest => Task[UnbindOfferResponse]] { _ =>
        ZIO.succeed(UnbindOfferResponse())
      }
      responseSetter: Service = new ServiceImpl(
        getClustersEffectRef,
        getOffersEffectRef,
        getClusteringMetaEffectRef,
        getClusteringAggregationEffectRef,
        bindOfferEffectRef,
        unbindOfferEffectRef
      )
      grpcClient: GrpcClient.Service[AglomeratService] = new GrpcClientLive[AglomeratService](
        new AglomeratServiceImpl(
          runtime,
          getClustersEffectRef,
          getOffersEffectRef,
          getClusteringMetaEffectRef,
          getClusteringAggregationEffectRef,
          bindOfferEffectRef,
          unbindOfferEffectRef
        ),
        null
      )
    } yield Has.allOf(responseSetter, grpcClient)
    creationEffect.toLayerMany
  }

  private class ServiceImpl(
      getClustersEffectRef: Ref[GetClustersRequest => Task[GetClustersResponse]],
      getOffersEffectRef: Ref[GetOffersRequest => Task[GetOffersResponse]],
      getClusteringMetaEffectRef: Ref[GetClusteringMetaRequest => Task[GetClusteringMetaResponse]],
      getClusteringAggregationEffectRef: Ref[GetClusteringAggregationRequest => Task[GetClusteringAggregationResponse]],
      bindOfferEffectRef: Ref[BindOfferRequest => Task[BindOfferResponse]],
      unbindOfferEffectRef: Ref[UnbindOfferRequest => Task[UnbindOfferResponse]])
    extends Service {

    override def setGetClustersResponse(response: GetClustersRequest => Task[GetClustersResponse]): UIO[Unit] =
      getClustersEffectRef.set(response)

    override def setGetOffersResponse(response: GetOffersRequest => Task[GetOffersResponse]): UIO[Unit] =
      getOffersEffectRef.set(response)

    override def setGetClusteringMetaResponse(
        response: GetClusteringMetaRequest => Task[GetClusteringMetaResponse]): UIO[Unit] =
      getClusteringMetaEffectRef.set(response)

    override def setGetClusteringAggregationResponse(
        response: GetClusteringAggregationRequest => Task[GetClusteringAggregationResponse]): UIO[Unit] =
      getClusteringAggregationEffectRef.set(response)

    override def setBindOfferResponse(response: BindOfferRequest => Task[BindOfferResponse]): UIO[Unit] =
      bindOfferEffectRef.set(response)

    override def setUnbindOfferResponse(response: UnbindOfferRequest => Task[UnbindOfferResponse]): UIO[Unit] =
      unbindOfferEffectRef.set(response)
  }

  private class AglomeratServiceImpl(
      runtime: Runtime[Any],
      getClustersEffectRef: Ref[GetClustersRequest => Task[GetClustersResponse]],
      getOffersEffectRef: Ref[GetOffersRequest => Task[GetOffersResponse]],
      getClusteringMetaEffectRef: Ref[GetClusteringMetaRequest => Task[GetClusteringMetaResponse]],
      getClusteringAggregationEffectRef: Ref[GetClusteringAggregationRequest => Task[GetClusteringAggregationResponse]],
      bindOfferEffectRef: Ref[BindOfferRequest => Task[BindOfferResponse]],
      unbindOfferEffectRef: Ref[UnbindOfferRequest => Task[UnbindOfferResponse]])
    extends AglomeratService {

    override def getClusters(request: GetClustersRequest): Future[GetClustersResponse] =
      runtime.unsafeRunToFuture(getClustersEffectRef.get.flatMap(_(request)))

    override def getOffers(request: GetOffersRequest): Future[GetOffersResponse] =
      runtime.unsafeRunToFuture(getOffersEffectRef.get.flatMap(_(request)))

    override def getClusteringMeta(request: GetClusteringMetaRequest): Future[GetClusteringMetaResponse] =
      runtime.unsafeRunToFuture(getClusteringMetaEffectRef.get.flatMap(_(request)))

    override def getClusteringAggregation(
        request: GetClusteringAggregationRequest): Future[GetClusteringAggregationResponse] =
      runtime.unsafeRunToFuture(getClusteringAggregationEffectRef.get.flatMap(_(request)))

    override def bindOffer(request: BindOfferRequest): Future[BindOfferResponse] =
      runtime.unsafeRunToFuture(bindOfferEffectRef.get.flatMap(_(request)))

    override def unbindOffer(request: UnbindOfferRequest): Future[UnbindOfferResponse] =
      runtime.unsafeRunToFuture(unbindOfferEffectRef.get.flatMap(_(request)))
  }
}
