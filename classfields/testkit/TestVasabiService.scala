package ru.yandex.vertis.general.vasabi.testkit

import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.{GrpcClient, GrpcClientLive}
import general.vasabi.api.VasesServiceGrpc.VasesService
import general.vasabi.api._
import zio.macros.accessible
import zio.{Ref, Task, _}

import scala.concurrent.Future

@accessible
object TestVasabiService {
  type TestVasabiService = Has[Service]

  trait Service {
    def setGetOfferVases(response: GetOffersVasesRequest => Task[GetOffersVasesResponse]): UIO[Unit]

    def setGetPurchasableOfferVases(
        response: GetPurchasableOfferVasesRequest => Task[GetPurchasableOfferVasesResponse]
      ): UIO[Unit]
    def setGetPaymentMethods(response: GetPaymentMethodsRequest => Task[GetPaymentMethodsResponse]): UIO[Unit]
    def setPurchaseOfferVas(response: PurchaseOfferVasRequest => Task[PurchaseOfferVasResponse]): UIO[Unit]
  }

  val layer: ULayer[GrpcClient[VasesService] with TestVasabiService] = {
    val creationEffect = for {
      runtime <- ZIO.runtime[Any]
      getOfferVasesRef <- ZRef.make[GetOffersVasesRequest => Task[GetOffersVasesResponse]] { _ =>
        ZIO.succeed(GetOffersVasesResponse())
      }
      getPurchasableOfferVasesRef <- ZRef
        .make[GetPurchasableOfferVasesRequest => Task[GetPurchasableOfferVasesResponse]] { _ =>
          ZIO.succeed(GetPurchasableOfferVasesResponse())
        }
      getPaymentMethodsRef <- ZRef.make[GetPaymentMethodsRequest => Task[GetPaymentMethodsResponse]] { _ =>
        ZIO.succeed(GetPaymentMethodsResponse())
      }
      purchaseOfferVas <- ZRef.make[PurchaseOfferVasRequest => Task[PurchaseOfferVasResponse]] { _ =>
        ZIO.succeed(PurchaseOfferVasResponse())
      }

      responseSetter: Service = new ServiceImpl(
        getOfferVasesRef,
        getPurchasableOfferVasesRef,
        getPaymentMethodsRef,
        purchaseOfferVas
      )
      grpcClient: GrpcClient.Service[VasesService] = new GrpcClientLive[VasesService](
        new VasesServiceImpl(
          runtime,
          getOfferVasesRef,
          getPurchasableOfferVasesRef,
          getPaymentMethodsRef,
          purchaseOfferVas
        ),
        null
      )
    } yield Has.allOf(responseSetter, grpcClient)
    creationEffect.toLayerMany
  }

  private class ServiceImpl(
      getOfferVasesRef: Ref[GetOffersVasesRequest => Task[GetOffersVasesResponse]],
      getPurchasableOfferVasesRef: Ref[GetPurchasableOfferVasesRequest => Task[GetPurchasableOfferVasesResponse]],
      getPaymentMethodsRef: Ref[GetPaymentMethodsRequest => Task[GetPaymentMethodsResponse]],
      purchaseOfferVasRef: Ref[PurchaseOfferVasRequest => Task[PurchaseOfferVasResponse]])
    extends Service {

    override def setGetOfferVases(response: GetOffersVasesRequest => Task[GetOffersVasesResponse]): UIO[Unit] =
      getOfferVasesRef.set(response)

    override def setGetPurchasableOfferVases(
        response: GetPurchasableOfferVasesRequest => Task[GetPurchasableOfferVasesResponse]): UIO[Unit] =
      getPurchasableOfferVasesRef.set(response)

    override def setGetPaymentMethods(
        response: GetPaymentMethodsRequest => Task[GetPaymentMethodsResponse]): UIO[Unit] =
      getPaymentMethodsRef.set(response)

    override def setPurchaseOfferVas(response: PurchaseOfferVasRequest => Task[PurchaseOfferVasResponse]): UIO[Unit] =
      purchaseOfferVasRef.set(response)
  }

  private class VasesServiceImpl(
      runtime: Runtime[Any],
      getOfferVasesRef: Ref[GetOffersVasesRequest => Task[GetOffersVasesResponse]],
      getPurchasableOfferVasesRef: Ref[GetPurchasableOfferVasesRequest => Task[GetPurchasableOfferVasesResponse]],
      getPaymentMethodsRef: Ref[GetPaymentMethodsRequest => Task[GetPaymentMethodsResponse]],
      purchaseOfferVasRef: Ref[PurchaseOfferVasRequest => Task[PurchaseOfferVasResponse]])
    extends VasesService {

    override def getOffersVases(request: GetOffersVasesRequest): Future[GetOffersVasesResponse] =
      runtime.unsafeRunToFuture(getOfferVasesRef.get.map(_(request)).flatten)

    override def getPurchasableOfferVases(
        request: GetPurchasableOfferVasesRequest): Future[GetPurchasableOfferVasesResponse] =
      runtime.unsafeRunToFuture(getPurchasableOfferVasesRef.get.map(_(request)).flatten)

    override def getPaymentMethods(request: GetPaymentMethodsRequest): Future[GetPaymentMethodsResponse] =
      runtime.unsafeRunToFuture(getPaymentMethodsRef.get.map(_(request)).flatten)

    override def purchaseOfferVas(request: PurchaseOfferVasRequest): Future[PurchaseOfferVasResponse] =
      runtime.unsafeRunToFuture(purchaseOfferVasRef.get.map(_(request)).flatten)
  }
}
