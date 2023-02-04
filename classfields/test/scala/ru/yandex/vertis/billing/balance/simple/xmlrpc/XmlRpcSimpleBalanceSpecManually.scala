package ru.yandex.vertis.billing.balance.simple.xmlrpc

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.vertis.billing.balance.simple.model.Basket.ByBasketId
import ru.yandex.vertis.billing.balance.simple.model._
import ru.yandex.vertis.billing.balance.simple.xmlrpc.XmlRpcSimpleBalanceSpecManually._

import scala.util.{Failure, Success}

/**
  * Created by IntelliJ IDEA.
  * User: alesavin
  * Date: 17.09.14
  * Time: 16:09
  */
@RunWith(classOf[JUnitRunner])
class XmlRpcSimpleBalanceSpecManually extends FlatSpec with Matchers {

  var order: OrderId = 0L
  var basket: BasketId = null
  var refund: RefundId = "111111"

  /*  "CheckBasket predefined" should "return Basket.State" in {
    balance.checkBasket(
      ServiceToken,
      UserIdentity,
      Basket("55f0616b5d83e420abae47ec","de3b9a60b32abebf6719b68fac9440d0")) match {
      case Success(r) => info(s"Done $r")
      println(r)
      case other => fail(s"Unexpected $other")
    }
  }*/

  "ListPaymentMethods" should "return PaymentMethods" in {
    println(ISODateTimeFormat.dateTime().print(DateTime.now()))

    balance.listPaymentMethods(ServiceToken, UserIdentity) match {
      case Success(r) => info(s"Done $r")
      case other => fail(s"Unexpected $other")
    }
  }

  "CreateServiceProduct" should "create product" in {
    balance.createServiceProduct(
      ServiceToken,
      Product.Source(
        ProductId,
        s"Test product $ProductId"
      )
    ) match {
      case Success(_) => info(s"Done")
      case other => fail(s"Unexpected $other")
    }
  }

  "GetPurchasedServiceProducts" should "return products" in {
    balance.getPurchasedServiceProducts(ServiceToken, UserIdentity, Product.Request()) match {
      case Success(r) => info(s"Done $r")
      case other => fail(s"Unexpected $other")
    }
  }

  "CreateOrderOrSubscription" should "return OrderResult" in {
    balance.createOrderOrSubscription(
      ServiceToken,
      UserIdentity,
      Order.Source(ProductId, Some(OrderId), regionId = Some(225L))
    ) match {
      case Success(r) => info(s"Done $r")
      case other => fail(s"Unexpected $other")
    }
  }

  "AlterOrder" should "change Order" in {
    balance.alterOrder(
      ServiceToken,
      UserIdentity,
      OrderId,
      Order.Patch(paymentMethod = Some(PaymentMethods.TrustWebPage))
    ) match {
      case Success(_) => info(s"Done")
      case other => fail(s"Unexpected $other")
    }
  }

  "CancelOrder" should "cancel Order" in {
    balance.cancelOrder(ServiceToken, UserIdentity, OrderId, preserveFunds = false) match {
      case Success(_) => info(s"Done")
      case other => fail(s"Unexpected $other")
    }
  }

  "CreateOrderOrSubscription 2" should "return OrderResult" in {
    balance.createOrderOrSubscription(
      ServiceToken,
      UserIdentity,
      Order.Source(ProductId, Some(OrderId2), regionId = Some(225L))
    ) match {
      case Success(r) =>
        info(s"Done $r")
        order = r
      case other => fail(s"Unexpected $other")
    }
  }

  "CreateBasket" should "return Basket" in {
    balance.createBasket(ServiceToken, UserIdentity, Basket.Source(Seq(Order.Request(order, price = Some(1))))) match {
      case Success(r) =>
        info(s"Done $r")
        basket = r
      case other => fail(s"Unexpected $other")
    }
  }

  "PayBasket" should "return Basket.State" in {
    balance.payBasket(
      ServiceToken,
      UserIdentity,
      ByBasketId(basket)
    ) match {
      case Success(r) => info(s"Done $r")
      case other => fail(s"Unexpected $other")
    }
  }

  "CheckBasket" should "return Basket.State" in {
    balance.checkBasket(ServiceToken, ByBasketId(basket)) match {
      case Success(r) =>
        info(s"Done $r")
        println(r)
      case other => fail(s"Unexpected $other")
    }
  }

  "CreateRefund" should "return Refund" in {
    balance.createRefund(ServiceToken, UserIdentity, Refund.Source("Test", basket, Seq.empty)) match {
      case Success(r) =>
        info(s"Done $r")
        refund = r
      case other => fail(s"Unexpected $other")
    }
  }

  "DoRefund" should "apply Refund" in {
    balance.doRefund(ServiceToken, UserIdentity, refund) match {
      case Success(r) =>
        info(s"Done $r")
        r should be(basket)
      case other => fail(s"Unexpected $other")
    }
  }

  "CreatePartner" should "create partner" in {
    balance.createPartner(ServiceToken, Partner.Request("test_partner", 1L)) match {
      case Success(r) =>
        info(s"Done $r")
      case other => fail(s"Unexpected $other")
    }
  }

  "GetPromocode" should "return promocodes" in {
    balance.getPromocode(ServiceToken, Promocodes.Request("test_series", 3, amount = Some(1))) match {
      case Success(r) =>
        info(s"Done $r")
      case other => fail(s"Unexpected $other")
    }
  }

  "CheckPromocodeStatus" should "return promo statistics" in {
    balance.checkPromocodeStatus(ServiceToken, "test_series") match {
      case Success(r) =>
        info(s"Done $r")
      case other => fail(s"Unexpected $other")
    }
  }

}

object XmlRpcSimpleBalanceSpecManually {

  val ServiceToken = "realty_pay_93ad97c77eaf24df828f4955e73b8a9e"
  val UserIdentity = Uid(225767981)

  implicit val ip: UserIP = "127.0.0.1"

  val ProductId = "test_003"
  val OrderId = 8L
  val OrderId2 = OrderId + 1L

  val SimpleBalanceUrl = "http://greed-tm1f.yandex.ru:8018/simple/xmlrpc"
//  val SimpleBalanceUrl = "http://greed-ts1f.yandex.ru:8018/simple/xmlrpc"
//  val SimpleBalanceUrl = "http://localhost:8018/simple/xmlrpc"

  val balance = new XmlRpcSimpleBalance(SimpleBalanceUrl)
}
