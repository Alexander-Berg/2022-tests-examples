package ru.yandex.vertis.billing.balance.simple.xmlrpc

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.vertis.billing.balance.simple.xmlrpc.parse.Parsers

/**
  * Created by IntelliJ IDEA.
  * User: alesavin
  * Date: 17.09.14
  * Time: 16:09
  */
@RunWith(classOf[JUnitRunner])
class ParsersSpec extends FlatSpec with Matchers {

  "Request parsers" should "parse CheckBasket" in {
    println(
      Parsers.asMethodCall(
        scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_BalanceSimple.CheckBasket.xml"))
      )
    )
  }

  it should "parse CreateOrderOrSubscription" in {
    println(
      Parsers.asMethodCall(
        scala.xml.XML
          .load(getClass.getResourceAsStream("/balance/xmlrpc_rq_BalanceSimple.CreateOrderOrSubscription.xml"))
      )
    )
  }

  it should "parse CreateBasket" in {
    println(
      Parsers.asMethodCall(
        scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_BalanceSimple.CreateBasket.xml"))
      )
    )
  }

  it should "parse CreateServiceProduct" in {
    println(
      Parsers.asMethodCall(
        scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_BalanceSimple.CreateServiceProduct.xml"))
      )
    )
  }

  it should "parse ListPaymentMethods" in {
    println(
      Parsers.asMethodCall(
        scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_BalanceSimple.ListPaymentMethods.xml"))
      )
    )
  }

  it should "parse PayBasket" in {
    println(
      Parsers.asMethodCall(
        scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_BalanceSimple.PayBasket.xml"))
      )
    )
  }

  "Response parsers" should "parse CheckBasket" in {
    println(
      Parsers.asMethodResponse(
        scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_BalanceSimple.CheckBasket.xml"))
      )
    )
  }

  it should "parse CheckBasket 2" in {
    println(
      Parsers.asMethodResponse(
        scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_BalanceSimple.CheckBasket2.xml"))
      )
    )
  }

  it should "parse CreateOrderOrSubscription" in {
    println(
      Parsers.asMethodResponse(
        scala.xml.XML
          .load(getClass.getResourceAsStream("/balance/xmlrpc_rs_BalanceSimple.CreateOrderOrSubscription.xml"))
      )
    )
  }

  it should "parse CreateBasket" in {
    println(
      Parsers.asMethodResponse(
        scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_BalanceSimple.CreateBasket.xml"))
      )
    )
  }

  it should "parse CreateBasket that paid" in {
    println(
      Parsers.asMethodResponse(
        scala.xml.XML
          .load(getClass.getResourceAsStream("/balance/xmlrpc_rs_BalanceSimple.CreateBasket_already_paid.xml"))
      )
    )
  }

  it should "parse CreateServiceProduct" in {
    println(
      Parsers.asMethodResponse(
        scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_BalanceSimple.CreateServiceProduct.xml"))
      )
    )
  }

  it should "parse ListPaymentMethods" in {
    println(
      Parsers.asMethodResponse(
        scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_BalanceSimple.ListPaymentMethods.xml"))
      )
    )
  }

  it should "parse PayBasket" in {
    val v = Parsers.asMethodResponse(
      scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_BalanceSimple.PayBasket.xml"))
    )
    println(v)
  }

  it should "parse PayBasket 2" in {
    val v = Parsers.asMethodResponse(
      scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_BalanceSimple.PayBasket2.xml"))
    )
    println(v)
  }

  it should "throw illegal argument for non-format response" in {
    intercept[IllegalArgumentException] {
      println(
        Parsers.asMethodResponse(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_ERROR.xml")))
      )
    }
  }

}
