package ru.yandex.vertis.billing.shop.domain.test

import billing.log_model.EventType
import common.zio.ydb.Ydb
import common.zio.ydb.Ydb.{HasTxRunner, Ydb}
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.billing.shop.billing_gates.trust.TrustManager.TrustManager
import ru.yandex.vertis.billing.shop.billing_gates.trust.testkit.TrustManagerMock
import ru.yandex.vertis.billing.shop.domain.ActiveProductStore.ActiveProductStore
import ru.yandex.vertis.billing.shop.domain.PurchaseController.PurchaseController
import ru.yandex.vertis.billing.shop.domain.PurchaseStore.PurchaseStore
import ru.yandex.vertis.billing.shop.domain.impl.in_memory.InMemoryProductTypeRevisionRegistry
import ru.yandex.vertis.billing.shop.domain.impl.{
  DefaultActiveProductStore,
  DefaultEventLogger,
  DefaultPurchaseController,
  DefaultPurchaseStore
}
import ru.yandex.vertis.billing.shop.domain.test.PurchaseControllerSpecUtils.{fetchEventLog, purchaseRequest}
import ru.yandex.vertis.billing.shop.domain.{ProductTypeRevisionRegistry, PurchaseController}
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
import common.zio.logging.Logging
import zio.clock.Clock
import zio.test.Assertion._
import _root_.zio.test.{assert, DefaultRunnableSpec}
import zio.{Has, Layer, ULayer, ZLayer}

object PurchaseControllerLoggingSpec extends DefaultRunnableSpec with Logging {

  override def spec =
    suite("DefaultPurchaseController logging tests")(
      testM("Purchase request should logging with EventType.INITIALIZATION and EventType.ACTIVATION") {
        val req1 = purchaseRequest(userId = "uniq_user_id_1", targetId = "uniq_target_id_1")
        for {
          _ <- TrustManagerMock.setPurchase(req1)
          _ <- PurchaseController.purchase(req1)
          /* выбираем по 2 эвента для каждого значения от 0 до ShardsCount,
           т.к. помимо INITIALIZATION идёт ещё ACTIVATION
           */
          events <- fetchEventLog(2)
        } yield assert(events)(isNonEmpty) &&
          assert(events.map(_.eventType))(hasSameElements(Seq(EventType.INITIALIZATION, EventType.ACTIVATION))) &&
          assert(events.size)(equalTo(2))

      }
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

      controller ++ mock ++ activeProductStore ++ purchaseStore ++ txRunner ++ exportDao
    }
}
