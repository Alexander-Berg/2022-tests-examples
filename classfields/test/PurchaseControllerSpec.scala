package ru.yandex.vertis.billing.shop.domain.test

import billing.shop.model.Purchase.PurchaseStatus
import billing.shop.purchase_service.PurchaseResponse.Conflict.ConflictType
import common.zio.logging.Logging
import common.zio.ydb.Ydb
import common.zio.ydb.Ydb.{HasTxRunner, Ydb}
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.billing.shop.billing_gates.trust.TrustManager.TrustManager
import ru.yandex.vertis.billing.shop.billing_gates.trust.model._
import ru.yandex.vertis.billing.shop.billing_gates.trust.testkit._
import ru.yandex.vertis.billing.shop.domain.ActiveProductStore.ActiveProductStore
import ru.yandex.vertis.billing.shop.domain.PurchaseController.PurchaseController
import ru.yandex.vertis.billing.shop.domain.PurchaseStore.PurchaseStore
import ru.yandex.vertis.billing.shop.domain._
import ru.yandex.vertis.billing.shop.domain.impl._
import ru.yandex.vertis.billing.shop.domain.impl.in_memory.InMemoryProductTypeRevisionRegistry
import ru.yandex.vertis.billing.shop.domain.test.PurchaseControllerSpecUtils._
import ru.yandex.vertis.billing.shop.model.Constants.RaiseFreeVasCode
import ru.yandex.vertis.billing.shop.model.Purchase.RefundDetails
import ru.yandex.vertis.billing.shop.model.{Money => _, UserId => _, _}
import ru.yandex.vertis.billing.shop.storage.ActiveProductsDao.ActiveProductsDao
import ru.yandex.vertis.billing.shop.storage.ExportDao.ExportDao
import ru.yandex.vertis.billing.shop.storage.ProcessingQueueDao.ProcessingQueueDao
import ru.yandex.vertis.billing.shop.storage.PurchaseDao.PurchaseDao
import ru.yandex.vertis.billing.shop.storage.ydb._
import zio.Exit.{Failure, Success}
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.nonFlaky
import zio.test.{assert, _}
import zio.{Has, Layer, ULayer, ZIO, ZLayer}

import java.time.Instant
import scala.util.Random

object PurchaseControllerSpec extends DefaultRunnableSpec with Logging {

  override def spec = {
    suite("DefaultPurchaseController all tests")(
      testM("#1 hold: paymentStatus = started with url") {
        val status = PaymentStatus.Started
        val request = purchaseRequest()
        for {
          _ <- TrustManagerMock.setPurchase(request)
          _ <- TrustManagerMock.setHold(status, true)
          response <- PurchaseController.purchase(request)
          purchaseFromStore <- fetchPurchaseFromStore(request)
          activeProducts <- fetchActiveProducts(request)
        } yield assertTrue(response.result.isPaymentForm) &&
          assert(purchaseFromStore.status)(equalTo(Purchase.Status.PaymentRequired)) &&
          assert(request.idempotencyId)(equalTo(purchaseFromStore.id.id)) &&
          assert(activeProducts.size)(equalTo(0))
      },
      testM("#2 hold: paymentStatus = started without url") {
        val status = PaymentStatus.Started
        val request = purchaseRequest()
        for {
          _ <- TrustManagerMock.setPurchase(request)
          _ <- TrustManagerMock.setHold(status)
          response <- PurchaseController.purchase(request)
          responseResult <- ZIO.fromOption(response.result.done)
          purchase <- ZIO.fromOption(responseResult.purchase)
          purchaseFromStore <- fetchPurchaseFromStore(request)
          activeProducts <- fetchActiveProducts(request)
        } yield assertTrue(response.result.isDone) &&
          assert(purchase.status)(equalTo(PurchaseStatus.HOLD)) &&
          assert(request.idempotencyId)(equalTo(purchaseFromStore.id.id)) &&
          assert(purchaseFromStore.status)(equalTo(Purchase.Status.Hold)) &&
          assert(activeProducts.size)(equalTo(1))
      },
      testM("#3 hold: paymentStatus = authorized/cleared") {
        checkAllM(Gen.fromIterable(Seq(PaymentStatus.Cleared, PaymentStatus.Authorized))) { status =>
          val request = purchaseRequest()

          for {
            _ <- TrustManagerMock.setPurchase(request)
            _ <- TrustManagerMock.setHold(status)
            response <- PurchaseController.purchase(request)
            responseResult <- ZIO.fromOption(response.result.done)
            purchase <- ZIO.fromOption(responseResult.purchase)
            purchaseFromStore <- fetchPurchaseFromStore(request)
            activeProducts <- fetchActiveProducts(request)
          } yield assertTrue(response.result.isDone) &&
            assert(request.idempotencyId)(equalTo(purchaseFromStore.id.id)) &&
            assert(purchaseFromStore.status)(equalTo(Purchase.Status.Applied)) &&
            assert(activeProducts.size)(equalTo(1)) &&
            assert(purchase.status)(equalTo(PurchaseStatus.APPLIED)) &&
            assert(activeProducts.head.productCode.code.name)(equalTo(RaiseFreeVasCode))
        }
      },
      testM("#4 hold: paymentStatus = not authorized/ canceled") {
        checkAllM(Gen.fromIterable(Seq(PaymentStatus.Canceled, PaymentStatus.NotAuthorized))) { status =>
          val request = purchaseRequest()
          for {
            _ <- TrustManagerMock.setPurchase(request)
            _ <- TrustManagerMock.setHold(status)
            response <- PurchaseController.purchase(request)
            responseResult <- ZIO.fromOption(response.result.done)
            purchase <- ZIO.fromOption(responseResult.purchase)
            purchaseFromStore <- fetchPurchaseFromStore(request)
            activeProducts <- fetchActiveProducts(request)
          } yield assertTrue(response.result.isDone) &&
            assert(request.idempotencyId)(equalTo(purchaseFromStore.id.id)) &&
            assert(purchaseFromStore.status)(equalTo(Purchase.Status.Canceled)) &&
            assert(activeProducts.size)(equalTo(0)) &&
            assert(purchase.status)(equalTo(PurchaseStatus.CANCELED))
        }
      },
      //  Started/NotStarted tested in#14 and #15
      testM("#7 clearPayment(success) from statuses Authorized") {
        val req = purchaseRequest()
        val status = PaymentStatus.Authorized
        for {
          _ <- TrustManagerMock.setPurchase(req)
          _ <- TrustManagerMock.setHold(status)
          response <- PurchaseController.purchase(req)
          purchaseFromStore <- fetchPurchaseFromStore(req)
          activeProducts <- fetchActiveProducts(req)

        } yield assertTrue(response.result.isDone) &&
          assert(purchaseFromStore.status)(equalTo(Purchase.Status.Applied)) &&
          assert(activeProducts.size)(equalTo(1)) && assert(activeProducts.head.productCode.code.name)(
            equalTo(RaiseFreeVasCode)
          )
      },
      testM("#8 clearPayment(failure) from status Authorized") {

        val req = purchaseRequest()
        val status = PaymentStatus.Authorized
        for {
          _ <- TrustManagerMock.setPurchase(req)
          _ <- TrustManagerMock.setHold(status)
          _ <- TrustManagerMock.setClearResult(ResponseStatus.Error)
          response <- PurchaseController.purchase(req)
          purchaseFromStore <- fetchPurchaseFromStore(req)
          activeProducts <- fetchActiveProducts(req)

        } yield assertTrue(response.result.isDone) &&
          assert(purchaseFromStore.status)(equalTo(Purchase.Status.Failed)) &&
          assert(activeProducts.size)(equalTo(0))

      },
      // All of these should not happen in practice
      testM("#9 handlePurchaseHook(Refund): with purchase in status Started with no url present") {
        val status = PaymentStatus.Started
        val req = purchaseRequest()
        for {
          _ <- TrustManagerMock.setPurchase(req)
          _ <- TrustManagerMock.setHold(status)
          response <- PurchaseController.purchase(req)
          responseOpt <- ZIO.fromOption(response.result.done)
          purchase <- ZIO.fromOption(responseOpt.purchase)
          purchaseFromStoreBeforeHook <- fetchPurchaseFromStore(req)
          _ <- PurchaseController.handleTrustWebhook(
            PurchaseId(req.idempotencyId),
            PurchaseMock.purchaseResult(
              req.idempotencyId,
              TrustWebHookPaymentStatus.Refund
            )
          )
          purchaseFromStoreAfterHook <- fetchPurchaseFromStore(req)
          activeProducts <- fetchActiveProducts(req)
        } yield assertTrue(response.result.isDone) &&
          assert(purchase.status)(equalTo(PurchaseStatus.HOLD)) &&
          assert(purchaseFromStoreBeforeHook.status)(equalTo(Purchase.Status.Hold)) &&
          assert(purchaseFromStoreAfterHook.status)(equalTo(Purchase.Status.Hold)) &&
          assert(activeProducts.size)(equalTo(1))
      },
      // should not happen since there is already a hold on user's card
      testM("#10 handlePurchaseHook(NoPayment): with purchase in status Authorized/Cleared") {
        checkAllM(Gen.fromIterable(Seq(PaymentStatus.Authorized, PaymentStatus.Cleared))) { status =>
          val req = purchaseRequest()

          for {
            _ <- TrustManagerMock.setPurchase(req)
            _ <- TrustManagerMock.setHold(status)
            response <- PurchaseController.purchase(req)
            responseOpt <- ZIO.fromOption(response.result.done)
            purchase <- ZIO.fromOption(responseOpt.purchase)
            purchaseFromStoreBeforeHook <- fetchPurchaseFromStore(req)
            activeProductsBeforeHook <- fetchActiveProducts(req)
            error <- PurchaseController
              .handleTrustWebhook(
                PurchaseId(req.idempotencyId),
                PurchaseMock.purchaseResult(
                  req.idempotencyId,
                  TrustWebHookPaymentStatus.NoPayment
                )
              )
              .run
            purchaseFromStoreAfterHook <- fetchPurchaseFromStore(req)
            activeProductsAfterHook <- fetchActiveProducts(req)
          } yield assert(error)(
            fails(
              isSubtype[UnexpectedError](anything)
            )
          ) && assertTrue(response.result.isDone) &&
            assert(purchase.status)(equalTo(PurchaseStatus.APPLIED)) &&
            assert(purchaseFromStoreBeforeHook.status)(equalTo(Purchase.Status.Applied)) &&
            assert(purchaseFromStoreAfterHook.status)(equalTo(Purchase.Status.Applied)) &&
            assert(activeProductsBeforeHook.size)(equalTo(1)) &&
            assert(activeProductsAfterHook.size)(equalTo(1)) &&
            assert(activeProductsBeforeHook.head.productCode.code.name)(equalTo(RaiseFreeVasCode)) &&
            assert(activeProductsAfterHook.head.productCode.code.name)(equalTo(RaiseFreeVasCode))

        }
      },
      testM("#11 handlePurchaseHook(Cancelled): with purchase in status Authorized/Cleared") {
        checkAllM(Gen.fromIterable(Seq(PaymentStatus.Authorized, PaymentStatus.Cleared))) { status =>
          val req = purchaseRequest()

          for {
            _ <- TrustManagerMock.setPurchase(req)
            _ <- TrustManagerMock.setHold(status)
            response <- PurchaseController.purchase(req)
            responseOpt <- ZIO.fromOption(response.result.done)
            purchase <- ZIO.fromOption(responseOpt.purchase)
            purchaseFromStoreBeforeHook <- fetchPurchaseFromStore(req)
            activeProductsBeforeHook <- fetchActiveProducts(req)
            error <- PurchaseController
              .handleTrustWebhook(
                PurchaseId(req.idempotencyId),
                PurchaseMock.purchaseResult(
                  req.idempotencyId,
                  TrustWebHookPaymentStatus.Cancelled
                )
              )
              .run
            purchaseFromStoreAfterHook <- fetchPurchaseFromStore(req)
            activeProductsAfterHook <- fetchActiveProducts(req)
          } yield assert(error)(
            fails(
              isSubtype[UnexpectedError](anything)
            )
          ) && assertTrue(response.result.isDone) &&
            assert(purchase.status)(equalTo(PurchaseStatus.APPLIED)) &&
            assert(purchaseFromStoreBeforeHook.status)(equalTo(Purchase.Status.Applied)) &&
            assert(purchaseFromStoreAfterHook.status)(equalTo(Purchase.Status.Applied)) &&
            assert(activeProductsBeforeHook.size)(equalTo(1)) &&
            assert(activeProductsAfterHook.size)(equalTo(1)) &&
            assert(activeProductsBeforeHook.head.productCode.code.name)(equalTo(RaiseFreeVasCode)) &&
            assert(activeProductsAfterHook.head.productCode.code.name)(equalTo(RaiseFreeVasCode))

        }
      },
      testM("#12 handlePurchaseHook(Refund): with purchase in status NotAuthorized/Canceled") {
        checkAllM(Gen.fromIterable(Seq(PaymentStatus.NotAuthorized, PaymentStatus.Canceled))) { status =>
          val req = purchaseRequest()

          for {
            _ <- TrustManagerMock.setPurchase(req)
            _ <- TrustManagerMock.setHold(status)
            response <- PurchaseController.purchase(req)
            responseOpt <- ZIO.fromOption(response.result.done)
            purchase <- ZIO.fromOption(responseOpt.purchase)
            purchaseFromStoreBeforeHook <- fetchPurchaseFromStore(req)
            activeProductsBeforeHook <- fetchActiveProducts(req)

            _ <- PurchaseController
              .handleTrustWebhook(
                PurchaseId(req.idempotencyId),
                PurchaseMock.purchaseResult(
                  req.idempotencyId,
                  TrustWebHookPaymentStatus.Refund
                )
              )
            purchaseFromStoreAfterHook <- fetchPurchaseFromStore(req)
            activeProductsAfterHook <- fetchActiveProducts(req)
          } yield assertTrue(response.result.isDone) &&
            assert(purchase.status)(equalTo(PurchaseStatus.CANCELED)) &&
            assert(purchaseFromStoreBeforeHook.status)(equalTo(Purchase.Status.Canceled)) &&
            assert(purchaseFromStoreAfterHook.status)(equalTo(Purchase.Status.Canceled)) &&
            assert(activeProductsBeforeHook.size)(equalTo(0)) &&
            assert(activeProductsAfterHook.size)(equalTo(0))
        }
      },
      testM("#30 clearPayment from statuses NotAuthorized/Canceled") {
        checkAllM(Gen.fromIterable(Seq(PaymentStatus.NotAuthorized, PaymentStatus.Canceled))) { status =>
          val req = purchaseRequest()
          for {
            _ <- TrustManagerMock.setPurchase(req)
            _ <- TrustManagerMock.setHold(status)
            _ <- PurchaseController.purchase(req)
            error <- PurchaseController
              .handleTrustWebhook(
                PurchaseId(req.idempotencyId),
                PurchaseMock.purchaseResult(req.idempotencyId, TrustWebHookPaymentStatus.Success)
              )
              .run
            purchaseFromStore <- fetchPurchaseFromStore(req)
            activeProducts <- fetchActiveProducts(req)

          } yield assert(error)(
            fails(
              isSubtype[UnexpectedError](anything)
            )
          ) && assert(purchaseFromStore.status)(equalTo(Purchase.Status.Canceled)) &&
            assert(activeProducts.size)(equalTo(0))
        }
      },
      testM("#13 handlePurchaseHook(NoPayment): with purchase in status Started with no url present") {
        val status = PaymentStatus.Started
        val req = purchaseRequest()
        for {
          _ <- TrustManagerMock.setPurchase(req)
          _ <- TrustManagerMock.setHold(status)
          response <- PurchaseController.purchase(req)
          responseOpt <- ZIO.fromOption(response.result.done)
          purchase <- ZIO.fromOption(responseOpt.purchase)
          purchaseFromStoreBeforeHook <- fetchPurchaseFromStore(req)
          _ <- PurchaseController.handleTrustWebhook(
            PurchaseId(req.idempotencyId),
            PurchaseMock.purchaseResult(
              req.idempotencyId,
              TrustWebHookPaymentStatus.NoPayment
            )
          )
          purchaseFromStoreAfterHook <- fetchPurchaseFromStore(req)
          activeProducts <- fetchActiveProducts(req)
        } yield assertTrue(response.result.isDone) &&
          assert(purchase.status)(equalTo(PurchaseStatus.HOLD)) &&
          assert(purchaseFromStoreBeforeHook.status)(equalTo(Purchase.Status.Hold)) &&
          assert(purchaseFromStoreAfterHook.status)(equalTo(Purchase.Status.Failed)) &&
          assert(activeProducts.size)(equalTo(0))
      },
      testM("#14 handlePurchaseHook(Success): with purchase in status Started with no url present") {
        val status = PaymentStatus.Started
        val req = purchaseRequest()
        for {
          _ <- TrustManagerMock.setPurchase(req)
          _ <- TrustManagerMock.setHold(status)
          response <- PurchaseController.purchase(req)
          responseOpt <- ZIO.fromOption(response.result.done)
          purchase <- ZIO.fromOption(responseOpt.purchase)
          purchaseFromStoreBeforeHook <- fetchPurchaseFromStore(req)
          _ <- PurchaseController.handleTrustWebhook(
            PurchaseId(req.idempotencyId),
            PurchaseMock.purchaseResult(
              req.idempotencyId,
              TrustWebHookPaymentStatus.Success
            )
          )
          purchaseFromStoreAfterHook <- fetchPurchaseFromStore(req)
          activeProducts <- fetchActiveProducts(req)
        } yield assertTrue(response.result.isDone) &&
          assert(purchase.status)(equalTo(PurchaseStatus.HOLD)) &&
          assert(purchaseFromStoreBeforeHook.status)(equalTo(Purchase.Status.Hold)) &&
          assert(purchaseFromStoreAfterHook.status)(equalTo(Purchase.Status.Applied)) &&
          assert(activeProducts.size)(equalTo(1)) &&
          assert(activeProducts.head.productCode.code.name)(equalTo(RaiseFreeVasCode))
      },
      testM("#15 handlePurchaseHook(Cancelled): with purchase in status Started with no url present") {
        val status = PaymentStatus.Started
        val req = purchaseRequest()
        for {
          _ <- TrustManagerMock.setPurchase(req)
          _ <- TrustManagerMock.setHold(status)
          response <- PurchaseController.purchase(req)
          responseOpt <- ZIO.fromOption(response.result.done)
          purchase <- ZIO.fromOption(responseOpt.purchase)
          purchaseFromStoreBeforeHook <- fetchPurchaseFromStore(req)
          _ <- PurchaseController.handleTrustWebhook(
            PurchaseId(req.idempotencyId),
            PurchaseMock.purchaseResult(
              req.idempotencyId,
              TrustWebHookPaymentStatus.Cancelled
            )
          )
          purchaseFromStoreAfterHook <- fetchPurchaseFromStore(req)
          activeProducts <- fetchActiveProducts(req)
        } yield assertTrue(response.result.isDone) &&
          assert(purchase.status)(equalTo(PurchaseStatus.HOLD)) &&
          assert(purchaseFromStoreBeforeHook.status)(equalTo(Purchase.Status.Hold)) &&
          assert(purchaseFromStoreAfterHook.status)(equalTo(Purchase.Status.Canceled)) &&
          assert(activeProducts.size)(equalTo(0))
      },
      testM("#16 handlePurchaseHook(Success): with purchase in status Authorized/Cleared") {
        checkAllM(Gen.fromIterable(Seq(PaymentStatus.Authorized, PaymentStatus.Cleared))) { status =>
          val req = purchaseRequest()

          for {
            _ <- TrustManagerMock.setPurchase(req)
            _ <- TrustManagerMock.setHold(status)
            response <- PurchaseController.purchase(req)
            responseOpt <- ZIO.fromOption(response.result.done)
            purchase <- ZIO.fromOption(responseOpt.purchase)
            purchaseFromStoreBeforeHook <- fetchPurchaseFromStore(req)
            activeProductsBeforeHook <- fetchActiveProducts(req)
            _ <- PurchaseController.handleTrustWebhook(
              PurchaseId(req.idempotencyId),
              PurchaseMock.purchaseResult(
                req.idempotencyId,
                TrustWebHookPaymentStatus.Success
              )
            )
            purchaseFromStoreAfterHook <- fetchPurchaseFromStore(req)
            activeProductsAfterHook <- fetchActiveProducts(req)
          } yield assertTrue(response.result.isDone) &&
            assert(purchase.status)(equalTo(PurchaseStatus.APPLIED)) &&
            assert(purchaseFromStoreBeforeHook.status)(equalTo(Purchase.Status.Applied)) &&
            assert(purchaseFromStoreAfterHook.status)(equalTo(Purchase.Status.Applied)) &&
            assert(activeProductsBeforeHook.size)(equalTo(1)) &&
            assert(activeProductsAfterHook.size)(equalTo(1)) &&
            assert(activeProductsAfterHook.head.productCode.code.name)(equalTo(RaiseFreeVasCode)) &&
            assert(activeProductsBeforeHook.head.productCode.code.name)(equalTo(RaiseFreeVasCode))

        }
      },
      testM("#17 handlePurchaseHook(Refund): with purchase in status Authorized/Cleared") {
        checkAllM(Gen.fromIterable(Seq(PaymentStatus.Authorized, PaymentStatus.Cleared))) { status =>
          val req = purchaseRequest()
          for {
            _ <- TrustManagerMock.setPurchase(req)
            _ <- TrustManagerMock.setHold(status)
            response <- PurchaseController.purchase(req)
            responseOpt <- ZIO.fromOption(response.result.done)
            purchase <- ZIO.fromOption(responseOpt.purchase)
            purchaseFromStoreBeforeHook <- fetchPurchaseFromStore(req)
            activeProductsBeforeHook <- fetchActiveProducts(req)
            _ <- PurchaseController.handleTrustWebhook(
              PurchaseId(req.idempotencyId),
              PurchaseMock.purchaseResult(
                req.idempotencyId,
                TrustWebHookPaymentStatus.Refund
              )
            )
            purchaseFromStoreAfterHook <- fetchPurchaseFromStore(req)
            activeProductsAfterHook <- fetchActiveProducts(req)
          } yield assertTrue(response.result.isDone) &&
            assert(purchase.status)(equalTo(PurchaseStatus.APPLIED)) &&
            assert(purchaseFromStoreBeforeHook.status)(equalTo(Purchase.Status.Applied)) &&
            assert(purchaseFromStoreAfterHook.status)(equalTo(Purchase.Status.Applied)) &&
            assert(activeProductsBeforeHook.size)(equalTo(1)) &&
            assert(activeProductsAfterHook.size)(equalTo(1)) &&
            assert(activeProductsBeforeHook.head.productCode.code.name)(equalTo(RaiseFreeVasCode)) &&
            assert(activeProductsAfterHook.head.productCode.code.name)(equalTo(RaiseFreeVasCode))
        }
      },
      testM("#18 handlePurchaseHook(Cancelled): with purchase in status NotAuthorized/Cancelled") {
        checkAllM(Gen.fromIterable(Seq(PaymentStatus.NotAuthorized, PaymentStatus.Canceled))) { status =>
          val req = purchaseRequest()

          for {
            _ <- TrustManagerMock.setPurchase(req)
            _ <- TrustManagerMock.setHold(status)
            response <- PurchaseController.purchase(req)
            responseOpt <- ZIO.fromOption(response.result.done)
            purchase <- ZIO.fromOption(responseOpt.purchase)
            purchaseFromStoreBeforeHook <- fetchPurchaseFromStore(req)
            activeProductsBeforeHook <- fetchActiveProducts(req)

            _ <- PurchaseController
              .handleTrustWebhook(
                PurchaseId(req.idempotencyId),
                PurchaseMock.purchaseResult(
                  req.idempotencyId,
                  TrustWebHookPaymentStatus.Cancelled
                )
              )
            purchaseFromStoreAfterHook <- fetchPurchaseFromStore(req)
            activeProductsAfterHook <- fetchActiveProducts(req)
          } yield assertTrue(response.result.isDone) &&
            assert(purchase.status)(equalTo(PurchaseStatus.CANCELED)) &&
            assert(purchaseFromStoreBeforeHook.status)(equalTo(Purchase.Status.Canceled)) &&
            assert(purchaseFromStoreAfterHook.status)(equalTo(Purchase.Status.Canceled)) &&
            assert(activeProductsBeforeHook.size)(equalTo(0)) &&
            assert(activeProductsAfterHook.size)(equalTo(0))
        }
      },
      testM("#19 handlePurchaseHook(NoPayment): with purchase in status NotAuthorized/Canceled") {
        checkAllM(Gen.fromIterable(Seq(PaymentStatus.NotAuthorized, PaymentStatus.Canceled))) { status =>
          val req = purchaseRequest()

          for {
            _ <- TrustManagerMock.setPurchase(req)
            _ <- TrustManagerMock.setHold(status)
            _ <- PurchaseController.purchase(req)
            purchaseFromStoreBeforeHook <- fetchPurchaseFromStore(req)
            activeProductsBeforeHook <- fetchActiveProducts(req)

            error <- PurchaseController
              .handleTrustWebhook(
                PurchaseId(req.idempotencyId),
                PurchaseMock.purchaseResult(
                  req.idempotencyId,
                  TrustWebHookPaymentStatus.NoPayment
                )
              )
              .run
            purchaseFromStoreAfterHook <- fetchPurchaseFromStore(req)
            activeProductsAfterHook <- fetchActiveProducts(req)
          } yield assert(error)(
            fails(
              isSubtype[UnexpectedError](anything)
            )
          ) &&
            assert(purchaseFromStoreBeforeHook.status)(equalTo(Purchase.Status.Canceled)) &&
            assert(purchaseFromStoreAfterHook.status)(equalTo(Purchase.Status.Canceled)) &&
            assert(activeProductsBeforeHook.size)(equalTo(0)) &&
            assert(activeProductsAfterHook.size)(equalTo(0))
        }
      },
      testM("#20 purchase: fail from create basket") {
        val req = purchaseRequest()

        for {
          _ <- TrustManagerMock.setPurchase(req)
          _ <- TrustManagerMock.failCreateBasket
          response <- PurchaseController.purchase(req).run
        } yield assert(response)(
          fails(
            isSubtype[TrustSttpError](anything)
          )
        )
      },
      testM("#21 purchase: fail from hold") {
        val req = purchaseRequest()

        for {
          _ <- TrustManagerMock.setPurchase(req)
          _ <- TrustManagerMock.failHold
          response <- PurchaseController.purchase(req).run
        } yield assert(response)(
          fails(
            isSubtype[TrustSttpError](anything)
          )
        )
      },
      testM("#22 purchase: fail from clear") {
        val req = purchaseRequest()

        for {
          _ <- TrustManagerMock.setPurchase(req)
          _ <- TrustManagerMock.failClear
          response <- PurchaseController.purchase(req).run
        } yield assert(response)(
          fails(
            isSubtype[TrustSttpError](anything)
          )
        )
      },
      testM("#23 purchase: fail create refund") {
        val req = purchaseRequest()

        for {
          _ <- TrustManagerMock.setPurchase(req)
          _ <- TrustManagerMock.failCreateRefund
          _ <- PurchaseController.purchase(req)
          response <- PurchaseController.refund(PurchaseId(req.idempotencyId), refundDetails = None).run
        } yield assert(response)(
          fails(
            isSubtype[TrustSttpError](anything)
          )
        )
      },
      testM("#24 purchase: fail perform refund") {
        val req = purchaseRequest()

        for {
          _ <- TrustManagerMock.setPurchase(req)
          _ <- TrustManagerMock.failCreateRefund
          _ <- PurchaseController.purchase(req)
          response <- PurchaseController.refund(PurchaseId(req.idempotencyId), refundDetails = None).run
        } yield assert(response)(
          fails(
            isSubtype[TrustSttpError](anything)
          )
        )
      },
      testM("#25 refund success") {
        val req = purchaseRequest()
        for {
          req <- ZIO.effectTotal(req.copy(idempotencyId = Random.nextLong().toString))
          _ <- TrustManagerMock.setPurchase(req)
          _ <- PurchaseController.purchase(req)
          purchaseBeforeRefund <- fetchPurchaseFromStore(req)
          activeProductsBeforeRefund <- fetchActiveProducts(req)
          operatorLogin <- ZIO.effectTotal(Random.nextLong().toString)
          refundTime <- ZIO.effectTotal(Instant.now())
          refundDetails = Some(RefundDetails(operatorLogin, refundTime))
          _ <- PurchaseController.refund(PurchaseId(req.idempotencyId), refundDetails)
          purchase <- fetchPurchaseFromStore(req)
          activeProducts <- fetchActiveProducts(req)
        } yield assert(purchaseBeforeRefund.status)(equalTo(Purchase.Status.Applied)) &&
          assert(purchaseBeforeRefund.refundStatus)(equalTo(Purchase.RefundStatus.NotRequired)) &&
          assert(purchaseBeforeRefund.refundDetails)(equalTo(None)) &&
          assert(purchase.refundStatus)(equalTo(Purchase.RefundStatus.Success)) &&
          assert(purchase.refundDetails)(equalTo(refundDetails)) &&
          assert(activeProductsBeforeRefund.size)(equalTo(1)) &&
          assert(activeProducts.size)(equalTo(0))
      },
      testM("#26 refund failure") {
        checkAllM(Gen.fromIterable(Seq(RefundStatus.Error, RefundStatus.Failed))) { status =>
          val req = purchaseRequest()
          for {
            req <- ZIO.effectTotal(req.copy(idempotencyId = Random.nextLong().toString))
            _ <- TrustManagerMock.setPurchase(req)
            _ <- TrustManagerMock.setPerformRefund(status)
            _ <- PurchaseController.purchase(req)
            purchaseBeforeRefund <- fetchPurchaseFromStore(req)
            activeProductsBeforeRefund <- fetchActiveProducts(req)
            operatorLogin <- ZIO.effectTotal(Random.nextLong().toString)
            refundTime <- ZIO.effectTotal(Instant.now())
            refundDetails = Some(RefundDetails(operatorLogin, refundTime))
            _ <- PurchaseController.refund(PurchaseId(req.idempotencyId), refundDetails)
            purchase <- fetchPurchaseFromStore(req)
            activeProducts <- fetchActiveProducts(req)
          } yield assert(purchaseBeforeRefund.status)(equalTo(Purchase.Status.Applied)) &&
            assert(purchaseBeforeRefund.refundStatus)(equalTo(Purchase.RefundStatus.NotRequired)) &&
            assert(purchaseBeforeRefund.refundDetails)(equalTo(None)) &&
            assert(purchase.refundStatus)(equalTo(Purchase.RefundStatus.Failed)) &&
            assert(purchase.refundDetails)(equalTo(refundDetails)) &&
            assert(activeProductsBeforeRefund.size)(equalTo(1)) &&
            assert(activeProducts.size)(equalTo(1)) &&
            assert(activeProducts.head.productCode.code.name)(equalTo(RaiseFreeVasCode))
        }
      },
      testM("#27 refund success hook") {
        checkAllM(Gen.fromIterable(Seq(TrustWebHookRefundStatus.Failed, TrustWebHookRefundStatus.Error))) { status =>
          val req = purchaseRequest()
          for {
            req <- ZIO.effectTotal(req.copy(idempotencyId = Random.nextLong().toString))
            _ <- TrustManagerMock.setPurchase(req)
            _ <- PurchaseController.purchase(req)
            purchaseFromStoreBeforeRefund <- fetchPurchaseFromStore(req)
            activeProductsBeforeRefund <- fetchActiveProducts(req)
            operatorLogin <- ZIO.effectTotal(Random.nextLong().toString)
            refundTime <- ZIO.effectTotal(Instant.now())
            refundDetails = Some(RefundDetails(operatorLogin, refundTime))
            _ <- PurchaseController.refund(PurchaseId(req.idempotencyId), refundDetails)
            error <- PurchaseController
              .handleTrustWebhook(
                PurchaseId(req.idempotencyId),
                PurchaseMock.refundResult(req.idempotencyId, status)
              )
              .run
            purchaseFromStore <- fetchPurchaseFromStore(req)
            activeProducts <- fetchActiveProducts(req)
          } yield assert(error)(
            fails(
              isSubtype[UnexpectedError](anything)
            )
          ) && assert(purchaseFromStoreBeforeRefund.status)(equalTo(Purchase.Status.Applied)) &&
            assert(purchaseFromStoreBeforeRefund.refundStatus)(equalTo(Purchase.RefundStatus.NotRequired)) &&
            assert(purchaseFromStoreBeforeRefund.refundDetails)(equalTo(None)) &&
            assert(purchaseFromStore.refundStatus)(equalTo(Purchase.RefundStatus.Success)) &&
            assert(purchaseFromStore.refundDetails)(equalTo(refundDetails)) &&
            assert(activeProductsBeforeRefund.size)(equalTo(1)) &&
            assert(activeProducts.size)(equalTo(0))
        }
      },
      testM("#29 attempt purchase with status Refunded") {
        val req = purchaseRequest()
        val status = PaymentStatus.Refunded
        for {
          _ <- TrustManagerMock.setPurchase(req)
          _ <- TrustManagerMock.setHold(status)
          error <- PurchaseController.purchase(req).run
        } yield assert(error)(
          fails(
            isSubtype[RequestMalformed](anything)
          )
        )
      },
      testM("#31 Too many active purchases ") {
        val reqs = (0 to 101).map(e => purchaseRequest(userId = "single"))
        for {
          error <- ZIO.foreach(reqs) { req =>
            for {
              _ <- TrustManagerMock.setPurchase(req)
              _ <- TrustManagerMock.setHold(PaymentStatus.Started)
              error <- PurchaseController.purchase(req).run
            } yield error
          }

        } yield error(100) match {
          case Success(value) => assertTrue(true)
          case Failure(cause) =>
            assert(error(100))(
              fails(
                isSubtype[TooManyPurchases](anything)
              )
            )
        }
      },
      testM("#32 Active product conflict") {
        val req1 = purchaseRequest(userId = "uniq_user_id_1", targetId = "uniq_target_id_1")
        val req2 = purchaseRequest(userId = "uniq_user_id_1", targetId = "uniq_target_id_1")
        for {
          _ <- TrustManagerMock.setPurchase(req1)
          purchaseResult1 <- PurchaseController.purchase(req1)
          _ <- TrustManagerMock.setPurchase(req2)
          purchaseResult2 <- PurchaseController.purchase(req2)
          purchaseFromStoreReq1 <- fetchPurchaseFromStore(req1)
          purchaseFromStoreReq2 <- fetchPurchaseFromStore(req2).run
          activeProductsReq1 <- fetchActiveProducts(req1)
          activeProductsReq2 <- fetchActiveProducts(req2)

        } yield assertTrue(purchaseResult1.result.isDone) &&
          assert(purchaseFromStoreReq2)(
            fails(
              isSubtype[PurchaseNotFound](anything)
            )
          ) && assertTrue(purchaseResult2.result.isError) &&
          assert(purchaseResult2.getError.typedError.conflict.get.`type`)(equalTo(ConflictType.ACTIVE_PRODUCT)) &&
          assert(activeProductsReq1.size)(equalTo(1)) && assert(
            activeProductsReq2.size
          )(equalTo(1)) &&
          assert(purchaseFromStoreReq1.status)(equalTo(Purchase.Status.Applied)) &&
          assert(activeProductsReq1.head.productCode.code.name)(equalTo(RaiseFreeVasCode))

      },
      testM("#33 Active purchase conflict ") {
        val req1 = purchaseRequest(userId = "single1", targetId = "single1")
        val req2 = purchaseRequest(userId = "single1", targetId = "single1")
        for {
          _ <- TrustManagerMock.setPurchase(req1)
          _ <- TrustManagerMock.setHold(PaymentStatus.Started)
          _ <- PurchaseController.purchase(req1)
          _ <- TrustManagerMock.setPurchase(req2)
          result <- PurchaseController.purchase(req2)

        } yield assertTrue(result.result.isError) &&
          assert(result.getError.typedError.conflict.get.`type`)(equalTo(ConflictType.PURCHASE))
      },
      testM("#34 stress test") {
        def checkState(status: Purchase.Status, size: Int) =
          if (status == Purchase.Status.Applied || status == Purchase.Status.Hold) {
            assert(size)(equalTo(1))
          } else assert(size)(equalTo(0))

        def randomHook() = Random.between(0, 5) match {
          case 0 => TrustWebHookPaymentStatus.Success
          case 1 => TrustWebHookPaymentStatus.Cancelled
          case 2 => TrustWebHookPaymentStatus.NoPayment
          case 3 => TrustWebHookPaymentStatus.Refund
          case 4 => TrustWebHookPaymentStatus.WaitForNotification
        }
        val req = purchaseRequest()
        checkAllMPar(Gen.fromIterable(1 to 70), 70) { num =>
          for {
            _ <- TrustManagerMock.setPurchase(req)
            _ <- TrustManagerMock.createRandomPurchaseTest()
            result <- PurchaseController.purchase(req).run
            hook <- PurchaseController
              .handleTrustWebhook(
                PurchaseId(req.idempotencyId),
                PurchaseMock.purchaseResult(
                  req.idempotencyId,
                  randomHook()
                )
              )
              .run
            purchaseFromStore <- fetchPurchaseFromStore(req)
            activeProducts <- fetchActiveProducts(req)
          } yield (result, hook) match {
            case (Success(value), Success(hook)) => checkState(purchaseFromStore.status, activeProducts.size)
            case (Failure(cause), Success(hook)) =>
              assert(result)(
                fails(
                  isSubtype[ShopError](anything)
                )
              ) && checkState(purchaseFromStore.status, activeProducts.size)

            case (Success(result), Failure(cause)) => {
              assert(hook)(
                fails(
                  isSubtype[ShopError](anything)
                )
              ) && checkState(purchaseFromStore.status, activeProducts.size)

            }
            case _ =>
              assert(result)(
                fails(
                  isSubtype[ShopError](anything)
                )
              ) && assert(hook)(
                fails(
                  isSubtype[ShopError](anything)
                )
              ) && checkState(purchaseFromStore.status, activeProducts.size)
          }
        }
      } @@ nonFlaky(10)
    ).provideCustomLayerShared {
      val ydb: ZLayer[Any, Nothing, Ydb] = TestYdb.ydb
      val txRunner: ZLayer[Any, Nothing, HasTxRunner] = ydb >>> Ydb.txRunner
      val clock: Layer[Nothing, Clock] = Clock.live
      // Dao
      val activeProductsDao: ZLayer[Any, Nothing, ActiveProductsDao] = ydb >>> YdbActiveProductsDao.live
      val purchaseDao: ZLayer[Any, Nothing, PurchaseDao] = ydb >>> YdbPurchaseDao.live
      val exportDao: ZLayer[Any, Nothing, ExportDao] = ydb >>> YdbExportDao.live
      val processingDao: ZLayer[Any, Nothing, ProcessingQueueDao] = ydb >>> YdbProcessingQueueDao.live

      val logging: ULayer[Logging.Logging] = Logging.live
      val eventLogger = clock ++ exportDao ++ purchaseDao >>> DefaultEventLogger.live
      // Store
      val purchaseStore: ZLayer[Any, Nothing, PurchaseStore] =
        logging ++ purchaseDao ++ clock >>> DefaultPurchaseStore.live
      val activeProductStore: ZLayer[Any, Nothing, ActiveProductStore] =
        (activeProductsDao ++ processingDao ++ exportDao ++ eventLogger ++ purchaseDao ++ clock ++ logging) >>> DefaultActiveProductStore.live

      val productRegistry: ULayer[Has[ProductTypeRevisionRegistry]] = InMemoryProductTypeRevisionRegistry.live
      val trustManager = TrustManagerMock.live

      val trustManagerLayer: ULayer[TrustManager] = trustManager.map(mock => Has(mock.get))

      val controller: ZLayer[Any, Nothing, PurchaseController] =
        (clock ++ logging ++ txRunner ++ activeProductStore ++ productRegistry ++ trustManagerLayer ++ purchaseStore ++ eventLogger) >>> DefaultPurchaseController.live
      val mock: ULayer[Has[TrustManagerMock.Service]] = trustManager.map(mock => Has(mock.get))

      controller ++ mock ++ activeProductStore ++ purchaseStore ++ txRunner
    }

  }
}
