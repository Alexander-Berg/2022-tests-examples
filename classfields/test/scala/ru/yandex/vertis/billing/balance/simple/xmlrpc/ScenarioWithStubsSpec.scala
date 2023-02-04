package ru.yandex.vertis.billing.balance.simple.xmlrpc

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FeatureSpec
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.billing.balance.simple.model.Basket.{ByBasketId, ByPurchaseToken}
import ru.yandex.vertis.billing.balance.simple.model._
import ru.yandex.vertis.billing.balance.simple.xmlrpc.ScenarioWithStubsSpec._

import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class ScenarioWithStubsSpec extends FeatureSpec {

  val ServiceToken = "realty_pay_93ad97c77eaf24df828f4955e73b8a9e"
  val TicketsServiceToken = "tickets_f4ac4122ee48c213eec816f4d7944ea6"

  implicit val ip: UserIP = "127.0.0.1"
  val UserIdentity = Uid(225767981)

  val ProductId = "test_001"
  val OrderId = 5L

  feature("SimpleBalance") {

    scenario("Payment methods") {
      val balance = new XmlRpcSimpleBalance(SimpleBalanceUrl) with Recorder {
        def getPath = s"$BaseScenariosDir/payment-methods"
      }
      info("get payment methods")
      balance.listPaymentMethods(ServiceToken, Uid(3000327428L)) match {
        case Success(r) => info(s"Done $r")
        case other => fail(s"Unexpected $other")
      }
    }

    scenario("Create product") {
      val balance = new XmlRpcSimpleBalance(SimpleBalanceUrl) with Recorder {
        def getPath = s"$BaseScenariosDir/product"
      }
      info("create product")
      val source =
        Product.Source(ProductId, s"Product $ProductId")
      val source2 =
        Product.Source(
          "test_002",
          s"Product test_002",
          prices = Seq(Product.Price(225L, DateTime.now(), 10.1, "RUB"))
        )
      balance.createServiceProduct(ServiceToken, source) match {
        case Success(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      info("create product twice")
      balance.createServiceProduct(ServiceToken, source) match {
        case Success(_) => info("Done")
        case other => fail(s"Unexpected $other")
      }
      info("create product with changes cause error")
      balance.createServiceProduct(ServiceToken, source.copy(productType = Some(Product.Types.App))) match {
        case Success(_) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }
      info("create product with prices")
      balance.createServiceProduct(ServiceToken, source2) match {
        case Success(_) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }

    }

    scenario("Base payment") {
      val balance = new XmlRpcSimpleBalance(SimpleBalanceUrl) with Recorder {
        def getPath = s"$BaseScenariosDir/payment"
      }

      var basket: BasketId = null

      info("create order")
      balance.createOrderOrSubscription(
        ServiceToken,
        UserIdentity,
        Order.Source(ProductId, Some(OrderId), regionId = Some(225L))
      ) match {
        case Success(r) => info(s"Done $r")
        case other => fail(s"Unexpected $other")
      }
      info("create basket")
      balance.createBasket(
        ServiceToken,
        UserIdentity,
        Basket.Source(
          Seq(Order.Request(OrderId, price = Some(111))),
          Basket.PayProperties(
            paymentMethod = Some(PaymentMethods.TrustWebPage),
            paymentTimeout = Some(600),
            currency = Some("RUB"),
            returnPath = Some("http://csback2ft.yandex.ru:34100/"),
            backUrl = Some("http://localhost")
          )
        )
      ) match {
        case Success(r) =>
          info(s"Done $r")
          basket = r
        case other => fail(s"Unexpected $other")
      }
      info("check basket")
      balance.checkBasket(ServiceToken, ByBasketId(basket)) match {
        case Success(r) => info(s"Done $r")
        case other => fail(s"Unexpected $other")
      }
      info("pay basket")
      balance.payBasket(
        ServiceToken,
        UserIdentity,
        ByBasketId(basket)
      ) match {
        case Success(r) => info(s"Done $r")
        case other => fail(s"Unexpected $other")
      }
    }

    scenario("Check paid basket") {
      val balance = new XmlRpcSimpleBalance(SimpleBalanceUrl) with Recorder {
        def getPath = s"$BaseScenariosDir/paid-basket"
      }

      val paidBasket: BasketId = "55f6f769795be256e70f36fc"
      var purchaseToken: PurchaseToken = null

      info("check basket")
      balance.checkBasket(TicketsServiceToken, ByBasketId(paidBasket)) match {
        case Success(r) =>
          info(s"Done $r")
          purchaseToken = r.purchaseToken.get
        case other => fail(s"Unexpected $other")
      }

      info("pay already paid basket")
      balance.payBasket(
        TicketsServiceToken,
        UserIdentity,
        ByBasketId(paidBasket)
      ) match {
        case Success(r) => info(s"Done $r")
        case other => fail(s"Unexpected $other")
      }

      info("check basket by purchase token")
      balance.checkBasket(TicketsServiceToken, ByPurchaseToken(purchaseToken)) match {
        case Success(r) => info(s"Done $r")
        case other => fail(s"Unexpected $other")
      }
    }

    scenario("Refund") {
      val balance = new XmlRpcSimpleBalance(SimpleBalanceUrl) with Recorder {
        def getPath = s"$BaseScenariosDir/refund"
      }

      val paidBasket: BasketId = "55f6f769795be256e70f36fc"
      val source = Refund.Source(
        "Test refund",
        paidBasket,
        Seq(Order.Delta(OrderId, 2, 0))
      )
      var refundId: RefundId = null

      info("create refund without deltas")
      balance.createRefund(TicketsServiceToken, UserIdentity, source.copy(orders = Seq())) match {
        case Failure(_) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }
      info("create refund with bad delta_qty")
      balance.createRefund(
        TicketsServiceToken,
        UserIdentity,
        source.copy(orders = Seq(Order.Delta(OrderId, 2, 3)))
      ) match {
        case Failure(_) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }
      info("create refund")
      balance.createRefund(TicketsServiceToken, UserIdentity, source) match {
        case Success(r) =>
          info(s"Done $r")
          refundId = r
        case other => fail(s"Unexpected $other")
      }
      info("do refund and WaitForNotification")
      balance.doRefund(TicketsServiceToken, UserIdentity, refundId) match {
        case Success(Refund.Statuses.WaitForNotification) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }
      info("do refund and success")
      balance.doRefund(TicketsServiceToken, UserIdentity, refundId) match {
        case Success(Refund.Statuses.Success) => info(s"Done")
        case other => fail(s"Unexpected $other")
      }
    }

    scenario("Partner") {
      val balance = new XmlRpcSimpleBalance(SimpleBalanceUrl) with Recorder {
        def getPath = s"$BaseScenariosDir/partner"
      }

      var pid: PartnerId = null

      val request = Partner.Request(
        name = "test_partner",
        operatorUid = 1L,
        partnerUid = Some(2L),
        email = Some("email@yandex.ru"),
        phone = Some("11111"),
        fax = Some("2222"),
        url = Some("site.ru"),
        city = Some("Spb"),
        regionId = Some(167L),
        shopParams = Map("ym_shop_id" -> "24534", "ym_shop_article_id" -> "243234")
      )
      info("create partner")
      balance.createPartner(ServiceToken, request) match {
        case Success(r) =>
          info(s"Done $r")
          pid = r
        case other => fail(s"Unexpected $other")
      }
      info("change partner data")
      balance.createPartner(ServiceToken, request.copy(partnerId = Some(pid), name = "test_partner_changed")) match {
        case Success(r) =>
          info(s"Done $r")
        case other => fail(s"Unexpected $other")
      }
    }

    scenario("Promocodes") {
      val balance = new XmlRpcSimpleBalance(SimpleBalanceUrl) with Recorder {
        def getPath = s"$BaseScenariosDir/promocodes"
      }

      val request = Promocodes.Request(
        "test_series",
        5,
        Some(44)
      )

      info("create amount promocodes")
      balance.getPromocode(ServiceToken, request) match {
        case Success(r) =>
          info(s"Done $r")
        case other => fail(s"Unexpected $other")
      }
      info("create discount promocodes")
      balance.getPromocode(ServiceToken, request.copy(discount = Some(55), amount = None)) match {
        case Success(r) =>
          info(s"Done $r")
        case other => fail(s"Unexpected $other")
      }
      info("check promocodes status")
      balance.checkPromocodeStatus(ServiceToken, "test_series") match {
        case Success(r) => info(s"Done $r")
        case other => fail(s"Unexpected $other")
      }
    }

  }

}

object ScenarioWithStubsSpec {

  val BaseScenariosDir = "src/test/resources/scenario"
  val SimpleBalanceUrl = "http://greed-tm1f.yandex.ru:8018/simple/xmlrpc"
//  val SimpleBalanceUrl = "http://greed-ts1f.yandex.ru:8018/simple/xmlrpc"
//  val SimpleBalanceUrl = "http://localhost:8018/simple/xmlrpc"
}
