package ru.yandex.vertis.billing.shop.domain.test

import billing.common_model.Project
import billing.log_model.{Platform, ProductEvent}
import billing.shop.model.{Basket, Money, PaymentGate, PaymentMarkup, UserId}
import billing.shop.purchase_service.PurchaseRequest
import cats.data.NonEmptyList
import common.zio.ydb.Ydb
import common.zio.ydb.Ydb.HasTxRunner
import common.zio.ydb.testkit.TestYdb
import ru.yandex.vertis.billing.shop.domain.{ActiveProductStore, PurchaseStore}
import ru.yandex.vertis.billing.shop.model.ActiveProductsFilter.{ExactProductsEntityFilter, ExactProductsFilter}
import ru.yandex.vertis.billing.shop.model.Constants.{OfferTargetType, RaiseFreeVasCode}
import ru.yandex.vertis.billing.shop.model.{ProductCode, PurchaseId, Target}
import ru.yandex.vertis.billing.shop.storage.ExportDao
import ru.yandex.vertis.billing.shop.storage.ExportDao.ExportDao
import ru.yandex.vertis.billing.shop.storage.ydb.YdbExportDao
import zio.ZIO
import zio.clock.Clock

import scala.util.Random

object PurchaseControllerSpecUtils {
  def fetchPurchaseFromStore(req: PurchaseRequest) = Ydb.runTx(PurchaseStore.get(PurchaseId(req.idempotencyId)))

  def fetchActiveProducts(req: PurchaseRequest) = for {
    basket <- ZIO.fromOption(req.basket)
    activeProducts <- Ydb.runTx(
      ActiveProductStore
        .select(
          ExactProductsFilter(
            Project.GENERAL,
            NonEmptyList.one(
              ExactProductsEntityFilter(
                userId = ru.yandex.vertis.billing.shop.model.UserId(req.userId.get.userId),
                target = Target(`type` = basket.rows.head.targetType, id = basket.rows.head.targetId),
                productCode = ProductCode(basket.rows.head.productCode)
              )
            )
          )
        )
    )
  } yield activeProducts

  def fetchEventLog(limit: Int) = {
    ZIO
      .foreach((0 until ExportDao.ShardsCount).toSet)(shardId => TestYdb.runTx(ExportDao.pull(shardId, limit)))
      .map(events => events.flatMap(e => e.toSet))
  }

  def purchaseRequest(
      token: String = generateRandomToken,
      targetId: String = generateRandomToken,
      userId: String = generateRandomToken,
      productCode: String = RaiseFreeVasCode): PurchaseRequest = PurchaseRequest(
    userId = Some(UserId(Project.GENERAL, userId)),
    basket =
      Some(Basket(Seq(Basket.Row(productCode = productCode, targetId = targetId, targetType = OfferTargetType)))),
    paymentMarkup = Some(
      PaymentMarkup(
        Seq(PaymentMarkup.Markup(Some(billing.shop.model.PaymentMethod(PaymentGate.TRUST)), Some(Money(1L))))
      )
    ),
    idempotencyId = token,
    returnPath = "http://o.yandex.ru",
    platform = Platform.DESKTOP
  )

  def generateRandomToken = Random.nextLong().toString

}
