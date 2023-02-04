package ru.yandex.vertis.billing.balance.xmlrpc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.balance.xmlrpc.parse.Parsers

/**
  * Created by IntelliJ IDEA.
  * User: alesavin
  * Date: 17.09.14
  * Time: 16:09
  */
class ParsersSpec extends AnyFlatSpec with Matchers {

  "Request parsers" should "parse BalanceNotification" in {
    println(
      Parsers
        .asMethodCall(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_BalanceNotification.xml")))
    )
  }

  it should "parse BalanceNotification 2" in {
    println(
      Parsers
        .asMethodCall(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_BalanceNotification_2.xml")))
    )
  }

  it should "parse CreateClient" in {
    println(
      Parsers.asMethodCall(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_CreateClient.xml")))
    )
  }

  it should "parse GetClientByIdBatch" in {
    println(
      Parsers
        .asMethodCall(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_GetClientByIdBatch.xml")))
    )
  }

  it should "parse GetPassportByLogin" in {
    println(
      Parsers
        .asMethodCall(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_GetPassportByLogin.xml")))
    )
  }

  it should "parse ListClientPassports" in {
    println(
      Parsers
        .asMethodCall(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_ListClientPassports.xml")))
    )
  }

  it should "parse RequestPayment" in {
    println(
      Parsers.asMethodCall(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_RequestPayment.xml")))
    )
  }

  it should "parse GetClientPersons" in {
    println(
      Parsers.asMethodCall(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_GetClientPersons.xml")))
    )
  }

  "Response parsers" should "parse CreateClient" in {
    println(
      Parsers.asMethodResponse(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_CreateClient.xml")))
    )
  }

  it should "parse rs error as array" in {
    println(
      Parsers.asMethodResponse(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_ERROR_array.xml")))
    )
  }

  it should "throw illegal argument when skip array data tag" in {
    intercept[IllegalArgumentException] {
      println(
        Parsers.asMethodResponse(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_ERROR_bad.xml")))
      )
    }
  }

  it should "parse rs error as fault" in {
    println(
      Parsers.asMethodResponse(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_ERROR_fault.xml")))
    )
  }

  it should "parse rs error as fault with i4" in {
    println(
      Parsers
        .asMethodResponse(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_ERROR_fault_i4.xml")))
    )
  }

  it should "parse GetClientByIdBatch" in {
    println(
      Parsers
        .asMethodResponse(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_GetClientByIdBatch.xml")))
    )
  }

  it should "parse GetClientByIdBatch with multi results" in {
    val v = Parsers.asMethodResponse(
      scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_GetClientByIdBatch_multi.xml"))
    )
    println(v)
  }

  it should "parse GetPassportByLogin" in {
    println(
      Parsers
        .asMethodResponse(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_GetPassportByLogin.xml")))
    )
  }

  it should "parse ListClientPassports" in {
    println(
      Parsers.asMethodResponse(
        scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_ListClientPassports.xml"))
      )
    )
  }

  it should "parse RequestPayment" in {
    println(
      Parsers
        .asMethodResponse(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_RequestPayment.xml")))
    )
  }

  it should "throw illegal argument when parse rq" in {
    intercept[IllegalArgumentException] {
      println(
        Parsers
          .asMethodResponse(scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rq_CreateClient.xml")))
      )
    }
  }

  it should "parse GetClientPersons" in {
    println(
      Parsers.asMethodResponse(
        scala.xml.XML.load(getClass.getResourceAsStream("/balance/xmlrpc_rs_GetClientPersons.xml"))
      )
    )
  }
}
