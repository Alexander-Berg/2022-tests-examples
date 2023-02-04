package ru.yandex.vos2.services.phone

import com.google.common.net.UrlEscapers
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.vos2.util.http.MockHttpClientHelper

import scala.util.{Failure, Success}

/**
  * Created by ridrisov on 01.12.16.
  */
@RunWith(classOf[JUnitRunner])
class DefaultSmsSenderClientTest extends FunSuite with MockHttpClientHelper {

  val host = "localhost"
  val port = 8080
  val sender = "autoru"
  val route = "autoru"

  val template = new TypedSmsTemplate {
    override def id: String = "test_template"
    override def smsText: String = "This is a test!"
  }

  val defaultPhone = "79167133878" // есть в white list
  val defaultDelivery = ToPhoneDelivery(defaultPhone)

  private val xmlSuccess =
    """<?xml version="1.0" encoding="utf-8"?>
      |<doc>
      | <message-sent id="100600" />
      |</doc>""".stripMargin
  private val xmlFail =
    """<?xml version="1.0" encoding="utf-8"?>
      |<doc>
      | <error>User ID not specified</error>
      | <errorcode>NOUID</errorcode>
      |</doc>""".stripMargin
  private val xmlOther =
    """<?xml version="1.0" encoding="utf-8"?>
      |<doc>
      | <otherTag>something</otherTag>
      |</doc>""".stripMargin
  private val xmlEmptyID =
    """<?xml version="1.0" encoding="utf-8"?>
      |<doc>
      | <message-sent id="" />
      |</doc>""".stripMargin

  def getClient(
    code: Int,
    body: String,
    whitelist: Boolean = false,
    phone: String = defaultPhone,
    isProduction: Boolean = true
  ): DefaultSmsSenderClient = {
    new DefaultSmsSenderClient(
      host,
      port,
      sender,
      route,
      isProduction,
      new Provider[TestingSmsPhoneWhitelist] {
        override def get(): TestingSmsPhoneWhitelist = new TestingSmsPhoneWhitelist {
          override def isWhitelisted(normalizedPhone: String): Boolean = whitelist
        }
      },
      application = "vos2-realty-unittests"
    ) {
      override protected def doRequest[T <: HttpRequestBase, R](name: String, request: T)(f: (HttpResponse) => R): R = {

        val smsText = if (isProduction) template.smsText else "[test]: " + template.smsText
        val text = UrlEscapers.urlFormParameterEscaper().escape(smsText)

        assert(request.getURI.toString == s"/sendsms/?route=$route&sender=$sender&utf8=1&phone=$phone&text=$text")

        val response = mockResponse(code, body)
        f(response)
      }
    }
  }

  test("sms send success") {

    val client = this.getClient(200, xmlSuccess)
    val res = client.send(template, defaultDelivery)

    res match {
      case Failure(_) => fail() //Тут ожидаем только Success
      case Success(v) => assert(v == "100600")
    }

  }

  test("sms send failure") {

    val client = this.getClient(200, xmlFail)
    val res = client.send(template, defaultDelivery)

    res match {
      case Success(_) => fail() //Тут ожидаем только Failure
      case Failure(e) => assert(e.getMessage == "Response error: User ID not specified")
    }
  }

  test("sms send empty response") {

    val client = this.getClient(200, "  ")
    val res = client.send(template, defaultDelivery)

    res match {
      case Success(_) => fail() //Тут ожидаем только Failure
      case Failure(e) => assert(e.getMessage == "Premature end of file.")
    }
  }

  test("sms send other response") {

    val client = this.getClient(200, xmlOther)
    val res = client.send(template, defaultDelivery)

    res match {
      case Success(_) => fail() //Тут ожидаем только Failure
      case Failure(e) => assert(e.getMessage == "Cannot parse response")
    }
  }

  test("sms send empty ID") {

    val client = this.getClient(200, xmlEmptyID)
    val res = client.send(template, defaultDelivery)

    res match {
      case Success(_) => fail() //Тут ожидаем только Failure
      case Failure(e) => assert(e.getMessage == "Response ID is empty")
    }
  }

  test("sms send from testing to phone from white list") {
    val phone = "79168402330"
    val whitelist = true

    val client = this.getClient(200, xmlSuccess, whitelist, phone = phone, isProduction = false)
    val res = client.send(template, ToPhoneDelivery(phone))

    res match {
      case Failure(_) => fail()
      case Success(v) => assert(v == "100600")
    }
  }

  test("sms send from testing to phone not from white list") {
    val phone = "79999999999"
    val whitelist = false

    val client = this.getClient(200, xmlSuccess, whitelist, phone = phone, isProduction = false)
    val res = client.send(template, ToPhoneDelivery(phone))

    res match {
      case Failure(_) => fail()
      case Success(v) => assert(v == "100500")
    }
  }

}
