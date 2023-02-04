package ru.yandex.vertis.billing.shop.billing_gates.trust.testkit

import billing.shop.purchase_service.{PurchaseRequest, PurchaseResponse}
import ru.yandex.vertis.billing.shop.billing_gates.trust.TrustManager
import ru.yandex.vertis.billing.shop.billing_gates.trust.model._
import ru.yandex.vertis.billing.shop.model.{Purchase, _}
import zio._
import zio.macros.accessible

class TrustManagerMock(var mockRef: FiberRef[Option[PurchaseMock]])
  extends TrustManager.Service
  with TrustManagerMock.Service {

  override def setPurchase(purchase: PurchaseRequest): UIO[Unit] = {
    mockRef.set(Some(PurchaseMock(purchase.idempotencyId, purchase.idempotencyId.reverse)))
  }

  override def failCreateBasket: UIO[Unit] = {
    mockRef.update(_.map(_.failFromCreateBasket))
  }

  override def failHold: UIO[Unit] = {
    mockRef.update(_.map(_.failFromHold))
  }

  override def failClear: UIO[Unit] = {
    mockRef.update(_.map(_.failFromClear))
  }

  override def failCreateRefund: UIO[Unit] = {
    mockRef.update(_.map(_.failFromCreateRefund))
  }

  override def failPerformRefund: UIO[Unit] = {
    mockRef.update(_.map(_.failPerformRefund))
  }

  override def setHold(status: PaymentStatus, urlPresent: Boolean) = {
    mockRef.update(_.map(_.setHold(status, urlPresent)))
  }

  override def setClearResult(status: ResponseStatus): UIO[Unit] = {
    mockRef.update(_.map(_.setClear(status)))
  }

  override def setPerformRefund(status: RefundStatus): UIO[Unit] = {
    mockRef.update(_.map(_.setPerformRefund(status)))
  }

  override def createRandomPurchaseTest() =
    for {
      refMock <- mockRef.get
      mock <- ZIO.fromOption(refMock)
      _ = mock.createRandomPurchaseTest()
    } yield ()

  override def createBasket(
      purchaseId: PurchaseId,
      userId: UserId,
      amount: Money,
      product: ProductType,
      returnPath: String,
      formTemplate: PaymentTemplate): IO[TrustClientError, PurchaseToken] = {
    mockRef.get
      .flatMap(optMock =>
        ZIO
          .fromOption(optMock.filter(_.token == purchaseId.id))
          .orElseFail(
            TrustSttpError(s"${optMock.get.token} != ${purchaseId.id} криво форкаешь бро", new RuntimeException)
          )
      )
      .flatMap(mock => ZIO.fromEither(mock.createBasket))
  }

  override def hold(purchaseToken: PurchaseToken): IO[TrustClientError, BasketResponse] =
    mockRef.get
      .flatMap(optMock =>
        ZIO
          .fromOption(optMock.filter(_.token == purchaseToken.token))
          .orElseFail(TrustSttpError("purchase mock isn't set", new RuntimeException))
      )
      .flatMap(mock => ZIO.fromEither(mock.hold))

  override def clear(purchaseToken: PurchaseToken): IO[TrustClientError, ClearResult] =
    mockRef.get
      .flatMap(optMock =>
        ZIO
          .fromOption(optMock.filter(_.token == purchaseToken.token))
          .orElseFail(TrustSttpError("purchase mock isn't set", new RuntimeException))
      )
      .flatMap(mock => ZIO.fromEither(mock.clear))

  override def createRefund(details: Purchase.TrustDetails, userId: UserId): IO[TrustClientError, RefundId] =
    for {
      mockRef <- mockRef.get
      token <- ZIO
        .fromOption(details.maybePurchaseToken)
        .orElseFail(TrustSttpError("missing purchase token", new RuntimeException))
      mock <- ZIO
        .fromOption(mockRef.filter(_.token == token.token))
        .orElseFail(TrustSttpError("purchase mock isn't set", new RuntimeException))
      refundId <- ZIO.fromEither(mock.createRefund)
    } yield refundId

  override def performRefund(details: Purchase.TrustDetails, userId: UserId): IO[TrustClientError, RefundResponse] =
    for {
      mockRef <- mockRef.get
      token <- ZIO
        .fromOption(details.maybePurchaseToken)
        .orElseFail(TrustSttpError("missing purchase token", new RuntimeException))
      mock <- ZIO
        .fromOption(mockRef.filter(_.token == token.token))
        .orElseFail(TrustSttpError("purchase mock isn't set", new RuntimeException))
      refundResult <- ZIO.fromEither(mock.performRefund)
    } yield refundResult

  override def createProduct(id: String, name: String, fiscalTitle: String): ZIO[Any, Nothing, PurchaseResponse] = ???

}

@accessible
object TrustManagerMock {

  trait Service {

    def setPurchase(purchase: PurchaseRequest): UIO[Unit]

    def failCreateBasket: UIO[Unit]

    def failHold: UIO[Unit]

    def setHold(status: PaymentStatus, urlPresent: Boolean = false): UIO[Unit]

    def setClearResult(status: ResponseStatus): UIO[Unit]

    def failPerformRefund: UIO[Unit]

    def failCreateRefund: UIO[Unit]

    def failClear: UIO[Unit]

    def setPerformRefund(status: RefundStatus): UIO[Unit]

    def createRandomPurchaseTest(): IO[Any, Unit]

  }

  def makeService = for {
    ref <- FiberRef.make[Option[PurchaseMock]](None)

  } yield new TrustManagerMock(ref)

  val live: ULayer[Has[TrustManagerMock]] = makeService.toLayer

}
