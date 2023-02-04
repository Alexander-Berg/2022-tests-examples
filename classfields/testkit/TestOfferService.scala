package ru.yandex.vertis.general.gost.testkit

import com.google.protobuf.empty.Empty
import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.{GrpcClient, GrpcClientLive}
import general.gost.offer_api.OfferServiceGrpc.OfferService
import general.gost.offer_api._
import general.gost.offer_model.OfferView
import io.grpc.stub.StreamObserver
import zio.macros.accessible
import zio.{Ref, Task, _}

import scala.concurrent.Future

@accessible
object TestOfferService {
  type TestOfferService = Has[Service]

  trait Service {
    def setGetOfferBatchResponse(response: GetOfferBatchRequest => Task[GetOfferBatchResponse]): UIO[Unit]
    def setGetOfferResponse(response: GetOfferRequest => Task[OfferView]): UIO[Unit]
    def setHideOffersResponse(response: HideOffersRequest => Task[Empty]): UIO[Unit]
    def setActivateOffersResponse(response: ActivateOffersRequest => Task[Empty]): UIO[Unit]
    def setDeleteOffersResponse(response: DeleteOffersRequest => Task[Empty]): UIO[Unit]
    def setCabinetOffersResponse(response: CabinetOffersRequest => Task[CabinetOffersResponse]): UIO[Unit]
    def setCountOffersResponse(response: CountOffersRequest => Task[CountOffersResponse]): UIO[Unit]
    def setGetEditableOfferResponse(response: GetEditableOfferRequest => Task[EditableOffer]): UIO[Unit]
    def setValidateOfferResponse(response: UpdateOfferRequest => Task[EditableOffer]): UIO[Unit]
    def setUpdateOfferResponse(response: UpdateOfferRequest => Task[UpdateOfferResponse]): UIO[Unit]
    def setGetPresetsCountResponse(response: PresetsCountRequest => Task[PresetsCountResponse]): UIO[Unit]
  }

  val layer: ULayer[GrpcClient[OfferService] with TestOfferService] = {
    val creationEffect = for {
      runtime <- ZIO.runtime[Any]
      getOfferBatchEffectRef <- ZRef.make[GetOfferBatchRequest => Task[GetOfferBatchResponse]] { _ =>
        ZIO.succeed(GetOfferBatchResponse())
      }
      getOfferEffectRef <- ZRef.make[GetOfferRequest => Task[OfferView]] { _ =>
        ZIO.succeed(OfferView())
      }
      hideOffersEffectRef <- ZRef.make[HideOffersRequest => Task[Empty]] { _ =>
        ZIO.succeed(Empty())
      }
      activateOffersEffectRef <- ZRef.make[ActivateOffersRequest => Task[Empty]] { _ =>
        ZIO.succeed(Empty())
      }
      deleteOffersEffectRef <- ZRef.make[DeleteOffersRequest => Task[Empty]] { _ =>
        ZIO.succeed(Empty())
      }
      cabinetOfferEffectRef <- ZRef.make[CabinetOffersRequest => Task[CabinetOffersResponse]] { _ =>
        ZIO.succeed(CabinetOffersResponse())
      }
      countOffersEffectRef <- ZRef.make[CountOffersRequest => Task[CountOffersResponse]] { _ =>
        ZIO.succeed(CountOffersResponse())
      }
      getEditableOfferEffectRef <- ZRef.make[GetEditableOfferRequest => Task[EditableOffer]] { _ =>
        ZIO.succeed(EditableOffer())
      }
      validateOfferEffectRef <- ZRef.make[UpdateOfferRequest => Task[EditableOffer]] { _ =>
        ZIO.succeed(EditableOffer())
      }
      updateOfferEffectRef <- ZRef.make[UpdateOfferRequest => Task[UpdateOfferResponse]] { _ =>
        ZIO.succeed(UpdateOfferResponse())
      }
      getPresetsCountEffectRef <- ZRef.make[PresetsCountRequest => Task[PresetsCountResponse]] { _ =>
        ZIO.succeed(PresetsCountResponse())
      }

      responseSetter: Service = new ServiceImpl(
        getOfferBatchEffectRef,
        getOfferEffectRef,
        hideOffersEffectRef,
        activateOffersEffectRef,
        deleteOffersEffectRef,
        cabinetOfferEffectRef,
        countOffersEffectRef,
        getEditableOfferEffectRef,
        validateOfferEffectRef,
        updateOfferEffectRef,
        getPresetsCountEffectRef
      )
      grpcClient: GrpcClient.Service[OfferService] = new GrpcClientLive[OfferService](
        new OfferServiceImpl(
          runtime,
          getOfferBatchEffectRef,
          getOfferEffectRef,
          hideOffersEffectRef,
          activateOffersEffectRef,
          deleteOffersEffectRef,
          cabinetOfferEffectRef,
          countOffersEffectRef,
          getEditableOfferEffectRef,
          validateOfferEffectRef,
          updateOfferEffectRef,
          getPresetsCountEffectRef
        ),
        null
      )
    } yield Has.allOf(responseSetter, grpcClient)
    creationEffect.toLayerMany
  }

  private class ServiceImpl(
      getOfferBatchEffectRef: Ref[GetOfferBatchRequest => Task[GetOfferBatchResponse]],
      getOfferEffectRef: Ref[GetOfferRequest => Task[OfferView]],
      hideOffersEffectRef: Ref[HideOffersRequest => Task[Empty]],
      activateOffersEffectRef: Ref[ActivateOffersRequest => Task[Empty]],
      deleteOffersEffectRef: Ref[DeleteOffersRequest => Task[Empty]],
      cabinetOfferEffectRef: Ref[CabinetOffersRequest => Task[CabinetOffersResponse]],
      countOffersEffectRef: Ref[CountOffersRequest => Task[CountOffersResponse]],
      getEditableOfferEffectRef: Ref[GetEditableOfferRequest => Task[EditableOffer]],
      validateOfferEffectRef: Ref[UpdateOfferRequest => Task[EditableOffer]],
      updateOfferEffectRef: Ref[UpdateOfferRequest => Task[UpdateOfferResponse]],
      getPresetsCountEffectRef: Ref[PresetsCountRequest => Task[PresetsCountResponse]])
    extends Service {

    override def setGetOfferBatchResponse(response: GetOfferBatchRequest => Task[GetOfferBatchResponse]): UIO[Unit] =
      getOfferBatchEffectRef.set(response)

    override def setGetOfferResponse(response: GetOfferRequest => Task[OfferView]): UIO[Unit] =
      getOfferEffectRef.set(response)

    override def setHideOffersResponse(response: HideOffersRequest => Task[Empty]): UIO[Unit] =
      hideOffersEffectRef.set(response)

    override def setActivateOffersResponse(response: ActivateOffersRequest => Task[Empty]): UIO[Unit] =
      activateOffersEffectRef.set(response)

    override def setDeleteOffersResponse(response: DeleteOffersRequest => Task[Empty]): UIO[Unit] =
      deleteOffersEffectRef.set(response)

    override def setCabinetOffersResponse(response: CabinetOffersRequest => Task[CabinetOffersResponse]): UIO[Unit] =
      cabinetOfferEffectRef.set(response)

    override def setCountOffersResponse(response: CountOffersRequest => Task[CountOffersResponse]): UIO[Unit] =
      countOffersEffectRef.set(response)

    override def setGetEditableOfferResponse(response: GetEditableOfferRequest => Task[EditableOffer]): UIO[Unit] =
      getEditableOfferEffectRef.set(response)

    override def setValidateOfferResponse(response: UpdateOfferRequest => Task[EditableOffer]): UIO[Unit] =
      validateOfferEffectRef.set(response)

    override def setUpdateOfferResponse(response: UpdateOfferRequest => Task[UpdateOfferResponse]): UIO[Unit] =
      updateOfferEffectRef.set(response)

    override def setGetPresetsCountResponse(response: PresetsCountRequest => Task[PresetsCountResponse]): UIO[Unit] =
      getPresetsCountEffectRef.set(response)
  }

  private class OfferServiceImpl(
      runtime: Runtime[Any],
      getOfferBatchEffectRef: Ref[GetOfferBatchRequest => Task[GetOfferBatchResponse]],
      getOfferEffectRef: Ref[GetOfferRequest => Task[OfferView]],
      hideOffersEffectRef: Ref[HideOffersRequest => Task[Empty]],
      activateOffersEffectRef: Ref[ActivateOffersRequest => Task[Empty]],
      deleteOffersEffectRef: Ref[DeleteOffersRequest => Task[Empty]],
      cabinetOfferEffectRef: Ref[CabinetOffersRequest => Task[CabinetOffersResponse]],
      countOffersEffectRef: Ref[CountOffersRequest => Task[CountOffersResponse]],
      getEditableOfferEffectRef: Ref[GetEditableOfferRequest => Task[EditableOffer]],
      validateOfferEffectRef: Ref[UpdateOfferRequest => Task[EditableOffer]],
      updateOfferEffectRef: Ref[UpdateOfferRequest => Task[UpdateOfferResponse]],
      getPresetsCountEffectRef: Ref[PresetsCountRequest => Task[PresetsCountResponse]])
    extends OfferService {

    override def getOfferBatch(request: GetOfferBatchRequest): Future[GetOfferBatchResponse] =
      runtime.unsafeRunToFuture(getOfferBatchEffectRef.get.map(_(request)).flatten)

    override def getOffer(request: GetOfferRequest): Future[OfferView] =
      runtime.unsafeRunToFuture(getOfferEffectRef.get.map(_(request)).flatten)

    override def getFeedOffer(request: GetFeedOfferRequest): Future[GetFeedOfferResponse] = ???

    override def hideOffers(request: HideOffersRequest): Future[Empty] =
      runtime.unsafeRunToFuture(hideOffersEffectRef.get.map(_(request)).flatten)

    override def activateOffers(request: ActivateOffersRequest): Future[Empty] =
      runtime.unsafeRunToFuture(activateOffersEffectRef.get.map(_(request)).flatten)

    override def deleteOffers(request: DeleteOffersRequest): Future[Empty] =
      runtime.unsafeRunToFuture(deleteOffersEffectRef.get.map(_(request)).flatten)

    override def cabinetOffers(request: CabinetOffersRequest): Future[CabinetOffersResponse] =
      runtime.unsafeRunToFuture(cabinetOfferEffectRef.get.map(_(request)).flatten)

    override def countOffers(request: CountOffersRequest): Future[CountOffersResponse] =
      runtime.unsafeRunToFuture(countOffersEffectRef.get.map(_(request)).flatten)

    override def getEditableOffer(request: GetEditableOfferRequest): Future[EditableOffer] =
      runtime.unsafeRunToFuture(getEditableOfferEffectRef.get.map(_(request)).flatten)

    override def validateOffer(request: UpdateOfferRequest): Future[EditableOffer] =
      runtime.unsafeRunToFuture(validateOfferEffectRef.get.map(_(request)).flatten)

    override def updateOffer(request: UpdateOfferRequest): Future[UpdateOfferResponse] =
      runtime.unsafeRunToFuture(updateOfferEffectRef.get.map(_(request)).flatten)

    override def getPresetsCount(request: PresetsCountRequest): Future[PresetsCountResponse] =
      runtime.unsafeRunToFuture(getPresetsCountEffectRef.get.map(_(request)).flatten)

    override def streamOffers(request: StreamOffersRequest, responseObserver: StreamObserver[StreamOffersBatch]): Unit =
      ()

    override def streamActiveOfferIds(
        request: StreamActiveOfferIdsRequest,
        responseObserver: StreamObserver[StreamActiveOfferIdsBatch]): Unit = ()

    override def streamOfferIdsForSeller(
        request: StreamOfferIdsForSellerRequest,
        responseObserver: StreamObserver[StreamOfferIdsForSellerResponse]): Unit = ()
  }
}
