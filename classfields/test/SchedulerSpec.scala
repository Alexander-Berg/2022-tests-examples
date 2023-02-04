package ru.yandex.vertis.billing.shop.scheduler.test

import common.zio.logging.Logging
import common.zio.ydb.Ydb
import common.zio.ydb.Ydb.{HasTxRunner, Ydb}
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.billing.shop.billing_gates.trust.TrustManager.TrustManager
import ru.yandex.vertis.billing.shop.billing_gates.trust.model.{PaymentStatus, RefundStatus}
import ru.yandex.vertis.billing.shop.billing_gates.trust.testkit.TrustManagerMock
import ru.yandex.vertis.billing.shop.domain.impl.in_memory.InMemoryProductTypeRevisionRegistry
import ru.yandex.vertis.billing.shop.domain.impl.{
  DefaultActiveProductStore,
  DefaultEventLogger,
  DefaultPurchaseController,
  DefaultPurchaseStore
}
import ru.yandex.vertis.billing.shop.domain.test.PurchaseControllerSpecUtils.{
  fetchActiveProducts,
  fetchPurchaseFromStore,
  purchaseRequest
}
import ru.yandex.vertis.billing.shop.domain.{ProductTypeRevisionRegistry, PurchaseController, PurchaseStore}
import ru.yandex.vertis.billing.shop.model.Purchase.RefundDetails
import ru.yandex.vertis.billing.shop.model.{Money => _, Purchase, PurchaseId, UserId => _}
import ru.yandex.vertis.billing.shop.scheduler.tasks.RefreshStalePurchasesTask.Config
import ru.yandex.vertis.billing.shop.scheduler.tasks.{DefaultRefreshStalePurchasesTask, RefreshStalePurchasesTask}
import ru.yandex.vertis.billing.shop.storage.ActiveProductsDao.ActiveProductsDao
import ru.yandex.vertis.billing.shop.storage.ExportDao.ExportDao
import ru.yandex.vertis.billing.shop.storage.ProcessingQueueDao.ProcessingQueueDao
import ru.yandex.vertis.billing.shop.storage.PurchaseDao.PurchaseDao
import ru.yandex.vertis.billing.shop.storage.ydb.{
  YdbActiveProductsDao,
  YdbExportDao,
  YdbProcessingQueueDao,
  YdbPurchaseDao
}
import ru.yandex.vertis.ydb.zio.TxRunner
import zio._
import zio.clock.Clock
import zio.duration.Duration
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestClock

import java.time.{Instant, OffsetDateTime}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Random

object SchedulerSpec extends DefaultRunnableSpec with Logging {

  override def spec = suite("scheduler-tests")(
    testM("check nothing happens when there are no stale purchases") {
      val req = purchaseRequest()
      for {
        _ <- TestClock.setDateTime(OffsetDateTime.now())
        task <- ZIO.service[DefaultRefreshStalePurchasesTask]
        _ <- TrustManagerMock.setPurchase(req)
        _ <- TrustManagerMock.setHold(PaymentStatus.Authorized)
        _ <- PurchaseController.purchase(req)
        _ <- TestClock.adjust(Duration.fromScala(1201.seconds))
        now <- clock.instant
        stalePurchases <- Ydb.runTx(PurchaseStore.getStalePurchases(1, 0, now))
        _ <- task.run
        noStalePurchases <- Ydb.runTx(PurchaseStore.getStalePurchases(1, 0, now))
        activeProducts <- fetchActiveProducts(req)
      } yield assert(stalePurchases.size)(equalTo(0)) &&
        assert(noStalePurchases.size)(equalTo(0)) &&
        assert(activeProducts.size)(equalTo(1))
    },
    testM("check successfully refreshed stale purchases") {
      val req = purchaseRequest()
      for {
        task <- ZIO.service[DefaultRefreshStalePurchasesTask]
        _ <- TestClock.setDateTime(OffsetDateTime.now())
        _ <- TrustManagerMock.setPurchase(req)
        _ <- TrustManagerMock.setHold(PaymentStatus.Started)
        _ <- PurchaseController.purchase(req)
        _ <- TestClock.adjust(Duration.fromScala(1201.seconds))
        _ <- TrustManagerMock.setHold(PaymentStatus.Authorized)
        now <- clock.instant
        stalePurchases <- Ydb.runTx(PurchaseStore.getStalePurchases(1, 0, now))
        _ <- task.run
        noStalePurchases <- Ydb.runTx(PurchaseStore.getStalePurchases(1, 0, now))
        activeProducts <- fetchActiveProducts(req)
        purchase <- fetchPurchaseFromStore(req)
      } yield assert(stalePurchases.size)(equalTo(1)) &&
        assert(noStalePurchases.size)(equalTo(0)) &&
        assert(activeProducts.size)(equalTo(1)) &&
        assert(purchase.status)(equalTo(Purchase.Status.Applied))
    },
    testM("check failed stale purchases ") {
      val req = purchaseRequest()
      for {
        task <- ZIO.service[DefaultRefreshStalePurchasesTask]
        _ <- TestClock.setDateTime(OffsetDateTime.now())
        _ <- TrustManagerMock.setPurchase(req)
        _ <- TrustManagerMock.failCreateBasket
        _ <- PurchaseController.purchase(req).ignore
        _ <- TestClock.adjust(Duration.fromScala(1201.seconds))
        now <- clock.instant
        stalePurchases <- Ydb.runTx(PurchaseStore.getStalePurchases(1, 0, now))
        _ <- task.run
        noStalePurchases <- Ydb.runTx(PurchaseStore.getStalePurchases(1, 0, now))
        activeProducts <- fetchActiveProducts(req)
        purchase <- fetchPurchaseFromStore(req)
      } yield assert(activeProducts.size)(equalTo(0)) &&
        assert(purchase.status)(equalTo(Purchase.Status.Canceled)) &&
        assert(noStalePurchases.size)(equalTo(0)) &&
        assert(stalePurchases.size)(equalTo(1))

    },
    testM("check successfully refreshed stale refunds ") {
      val req = purchaseRequest()
      for {
        task <- ZIO.service[DefaultRefreshStalePurchasesTask]
        _ <- TestClock.setDateTime(OffsetDateTime.now())
        _ <- TrustManagerMock.setPurchase(req)
        _ <- TrustManagerMock.setPerformRefund(RefundStatus.WaitForNotification)
        _ <- PurchaseController.purchase(req)
        operatorLogin <- ZIO.effectTotal(Random.nextLong().toString)
        refundTime <- ZIO.effectTotal(Instant.now())
        refundDetails = Some(RefundDetails(operatorLogin, refundTime))
        _ <- PurchaseController.refund(PurchaseId(req.idempotencyId), refundDetails)
        purchaseBeforeRefund <- fetchPurchaseFromStore(req)
        _ <- TestClock.adjust(Duration.fromScala(1201.seconds))
        now <- clock.instant
        stalePurchases <- Ydb.runTx(PurchaseStore.getStalePurchases(1, 0, now))
        _ <- TrustManagerMock.setPerformRefund(RefundStatus.Success)
        _ <- task.run
        now <- clock.instant
        noStalePurchases <- Ydb.runTx(PurchaseStore.getStalePurchases(1, 0, now))
        activeProducts <- fetchActiveProducts(req)
        purchase <- fetchPurchaseFromStore(req)
      } yield assert(activeProducts.size)(equalTo(0)) &&
        assert(purchaseBeforeRefund.refundStatus)(equalTo(Purchase.RefundStatus.Required)) &&
        assert(purchaseBeforeRefund.refundDetails)(equalTo(refundDetails)) &&
        assert(purchase.refundStatus)(equalTo(Purchase.RefundStatus.Success)) &&
        assert(purchase.refundDetails)(equalTo(refundDetails)) &&
        assert(stalePurchases.size)(equalTo(1)) &&
        assert(noStalePurchases.size)(equalTo(0))

    },
    testM("check failed staled refunds (createRefund failure)") {
      val req = purchaseRequest()
      for {
        task <- ZIO.service[DefaultRefreshStalePurchasesTask]
        _ <- TestClock.setDateTime(OffsetDateTime.now())
        _ <- TrustManagerMock.setPurchase(req)
        _ <- TrustManagerMock.failCreateRefund
        _ <- PurchaseController.purchase(req)
        operatorLogin <- ZIO.effectTotal(Random.nextLong().toString)
        refundTime <- ZIO.effectTotal(Instant.now())
        refundDetails = Some(RefundDetails(operatorLogin, refundTime))
        _ <- PurchaseController.refund(PurchaseId(req.idempotencyId), refundDetails).ignore
        purchaseBeforeRefund <- fetchPurchaseFromStore(req)
        _ <- TestClock.adjust(Duration.fromScala(1201.seconds))
        now <- clock.instant
        stalePurchases <- Ydb.runTx(PurchaseStore.getStalePurchases(1, 0, now))
        _ <- task.run
        noStalePurchases <- Ydb.runTx(PurchaseStore.getStalePurchases(1, 0, now))
        activeProducts <- fetchActiveProducts(req)
        purchase <- fetchPurchaseFromStore(req)
      } yield assert(activeProducts.size)(equalTo(1)) &&
        assert(purchaseBeforeRefund.refundStatus)(equalTo(Purchase.RefundStatus.Required)) &&
        assert(purchaseBeforeRefund.refundDetails)(equalTo(refundDetails)) &&
        assert(purchase.refundStatus)(equalTo(Purchase.RefundStatus.Failed)) &&
        assert(purchase.refundDetails)(equalTo(refundDetails)) &&
        assert(stalePurchases.size)(equalTo(1)) &&
        assert(noStalePurchases.size)(equalTo(0))
    },
    testM("check failed staled refunds (performRefund error response)") {
      val req = purchaseRequest()
      for {
        task <- ZIO.service[DefaultRefreshStalePurchasesTask]
        _ <- TestClock.setDateTime(OffsetDateTime.now())
        _ <- TrustManagerMock.setPurchase(req)
        _ <- TrustManagerMock.failPerformRefund
        _ <- PurchaseController.purchase(req)
        operatorLogin <- ZIO.effectTotal(Random.nextLong().toString)
        refundTime <- ZIO.effectTotal(Instant.now())
        refundDetails = Some(RefundDetails(operatorLogin, refundTime))
        _ <- PurchaseController.refund(PurchaseId(req.idempotencyId), refundDetails).ignore
        purchaseBeforeRefund <- fetchPurchaseFromStore(req)
        _ <- TestClock.adjust(Duration.fromScala(1201.seconds))
        _ <- TrustManagerMock.setPerformRefund(RefundStatus.Error)
        now <- clock.instant
        stalePurchases <- Ydb.runTx(PurchaseStore.getStalePurchases(1, 0, now))
        _ <- task.run
        noStalePurchases <- Ydb.runTx(PurchaseStore.getStalePurchases(1, 0, now))
        activeProducts <- fetchActiveProducts(req)
        purchase <- fetchPurchaseFromStore(req)
      } yield assert(activeProducts.size)(equalTo(1)) &&
        assert(purchaseBeforeRefund.refundStatus)(equalTo(Purchase.RefundStatus.Required)) &&
        assert(purchaseBeforeRefund.refundDetails)(equalTo(refundDetails)) &&
        assert(purchase.refundStatus)(equalTo(Purchase.RefundStatus.Failed)) &&
        assert(purchase.refundDetails)(equalTo(refundDetails)) &&
        assert(stalePurchases.size)(equalTo(1)) &&
        assert(noStalePurchases.size)(equalTo(0))
    }
  ).provideCustomLayer {

    val ydb: ZLayer[Any, Nothing, Ydb] = TestYdb.ydb
    val txRunner: ZLayer[Any, Nothing, HasTxRunner] = ydb >>> Ydb.txRunner
    val clock: ZLayer[Clock with TestClock, Nothing, Clock with TestClock] = TestClock.any
    // Dao
    val activeProductsDao: ZLayer[Any, Nothing, ActiveProductsDao] = ydb >>> YdbActiveProductsDao.live
    val purchaseDao: ZLayer[Any, Nothing, PurchaseDao] = ydb >>> YdbPurchaseDao.live
    val exportDao: ZLayer[Any, Nothing, ExportDao] = ydb >>> YdbExportDao.live
    val processingDao: ZLayer[Any, Nothing, ProcessingQueueDao] = ydb >>> YdbProcessingQueueDao.live
    val eventLogger = clock ++ exportDao ++ purchaseDao >>> DefaultEventLogger.live
    val logging: ULayer[Logging.Logging] = Logging.live
    // Store
    val purchaseStore = purchaseDao ++ clock ++ logging >>> DefaultPurchaseStore.live
    val activeProductStore =
      (activeProductsDao ++ processingDao ++ exportDao ++ purchaseDao ++ clock ++ logging ++ eventLogger) >>> DefaultActiveProductStore.live

    val productRegistry: ULayer[Has[ProductTypeRevisionRegistry]] = InMemoryProductTypeRevisionRegistry.live
    val trustManager = TrustManagerMock.live

    val trustManagerLayer: ULayer[TrustManager] = trustManager.map(mock => Has(mock.get))

    val controller =
      (Clock.live ++ logging ++ txRunner ++ activeProductStore ++ productRegistry ++ trustManagerLayer ++ purchaseStore ++ eventLogger) >>> DefaultPurchaseController.live
    val mock: ULayer[Has[TrustManagerMock.Service]] = trustManager.map(mock => Has(mock.get))

    val config = ZLayer.succeed(
      RefreshStalePurchasesTask.Config(
        FiniteDuration(1, TimeUnit.MINUTES),
        FiniteDuration(20, TimeUnit.MINUTES),
        100
      )
    )

    val DefaultRefreshStalePurchasesTask =
      ZLayer.fromServices[
        PurchaseStore.Service,
        PurchaseController.Service,
        Config,
        Clock.Service,
        TxRunner,
        DefaultRefreshStalePurchasesTask
      ](new DefaultRefreshStalePurchasesTask(_, _, _, _, _))

    val refreshStalePurchasesTask =
      config ++ purchaseStore ++ controller ++ clock ++ txRunner >>> DefaultRefreshStalePurchasesTask

    refreshStalePurchasesTask ++ mock ++ controller ++ purchaseStore ++ txRunner ++ activeProductStore

  }
}
