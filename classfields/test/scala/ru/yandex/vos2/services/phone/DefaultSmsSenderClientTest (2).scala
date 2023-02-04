package ru.yandex.vos2.services.phone

import com.google.common.net.UrlEscapers
import org.apache.http.HttpResponse
import org.apache.http.client.methods.{HttpPost, HttpRequestBase, HttpUriRequest}
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vos2.util.HttpBlockingPool.Instance
import ru.yandex.vos2.util.http.MockHttpClientHelper
import org.mockito.Mockito._

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

  val mockedStaffClients = Map(
    200 -> mockHttpClient(200),
    404 -> mockHttpClient(404)
  )

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

  def getClient(code: Int,
                body: String,
                staffCode: Int = 404,
                phone: String = defaultPhone,
                isProduction: Boolean = true): DefaultSmsSenderClient = {
    new DefaultSmsSenderClient(host, port, sender, route, isProduction) {
      override protected def doRequest[T <: HttpRequestBase, R](name: String, request: T)(f: (HttpResponse) => R): R = {

        val smsText = if (isProduction) template.smsText else "[test]: " + template.smsText
        val text = UrlEscapers.urlFormParameterEscaper().escape(smsText)

        assert(request.getURI.toString == s"/sendsms/?route=$route&sender=$sender&utf8=1&phone=$phone&text=$text")

        val response = mockResponse(code, body)
        f(response)
      }

      override lazy val staffClient = new Instance(mockedStaffClients(staffCode))
    }
  }

  test("sms send not from production, success") {

    val client = this.getClient(200, xmlSuccess, isProduction = false)
    val res = client.send(template, defaultDelivery)

    res match {
      case Failure(_) => fail() //Тут ожидаем только Success
      case Success(v) => assert(v == "100600")
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

  test("sms send to phone not from white list but it is on staff") {
    val phone = "79168402330" // нет в white list
    val staffResponseCode = 200 // есть на стаффе

    val client = this.getClient(200, xmlSuccess, staffCode = staffResponseCode,
      phone = phone, isProduction = false)
    val res = client.send(template, ToPhoneDelivery(phone))

    val argCaptorRequest = ArgumentCaptor.forClass(classOf[HttpUriRequest])
    verify(mockedStaffClients(staffResponseCode)).execute(argCaptorRequest.capture())

    val expectedUrl =
      s"https://staff-api.yandex-team.ru/v3/persons?phones.number=%2B$phone&_one=1"
    assert(argCaptorRequest.getValue.asInstanceOf[HttpPost].getURI.toString
      == expectedUrl)

    res match {
      case Failure(_) => fail()
      case Success(v) => assert(v == "100600")
    }
  }

  test("sms send to phone not from staff but it is in white list") {
    val phone = "380956031342"  // есть в white list
    val staffResponseCode = 404 // нет на стаффе

    val client = this.getClient(200, xmlSuccess, staffCode = staffResponseCode,
      phone = phone, isProduction = false)
    val res = client.send(template, ToPhoneDelivery(phone))

    val argCaptorRequest = ArgumentCaptor.forClass(classOf[HttpUriRequest])
    verify(mockedStaffClients(staffResponseCode), never()).execute(argCaptorRequest.capture())

    res match {
      case Failure(_) => fail()
      case Success(v) => assert(v == "100600")
    }
  }

  test("sms send to phone not from staff and white list") {
    val phone = "79999999999"
    val staffResponseCode = 404

    val client = this.getClient(200, xmlSuccess, staffCode = staffResponseCode,
      phone = phone, isProduction = false)
    val res = client.send(template, ToPhoneDelivery(phone))

    val argCaptorRequest = ArgumentCaptor.forClass(classOf[HttpUriRequest])
    verify(mockedStaffClients(staffResponseCode)).execute(argCaptorRequest.capture())

    val expectedUrl =
      s"https://staff-api.yandex-team.ru/v3/persons?phones.number=%2B$phone&_one=1"
    assert(argCaptorRequest.getValue.asInstanceOf[HttpPost].getURI.toString
      == expectedUrl)

    res match {
      case Failure(_) => fail()
      case Success(v) => assert(v == "100500")
    }
  }

}
