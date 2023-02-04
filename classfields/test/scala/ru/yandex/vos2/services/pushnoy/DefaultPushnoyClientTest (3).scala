package ru.yandex.vos2.services.pushnoy

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.model.PushnoyDelivery
import ru.yandex.vos2.util.http.MockHttpClientHelper

import scala.util.{Failure, Success}

/**
  * Created by sievmi on 23.08.17.
  */

@RunWith(classOf[JUnitRunner])
class DefaultPushnoyClientTest extends AnyFunSuite with MockHttpClientHelper {
  implicit val traced = Traced.empty

  def getMosckPushnoyCliet(code: Int, body: String): DefaultPushnoyClient = {
    new DefaultPushnoyClient("host", 123) {
      override def doRequest[T <: HttpRequestBase, R](name: String,
                                                      request: T)(f: (HttpResponse) => R)(implicit trace: Traced): R = {
        val response = mockResponse(code, body)
        f(response)
      }
    }
  }

  val pushParams = ToPushDelivery("userId", Some(PushnoyDelivery.ServicesAndDiscounts))

  val template = PushTemplateV1(
    "low_rating",
    "Промо поднятия в поиске",
    "deeplink",
    Some("title"),
    Some("title"),
    "android_body",
    "ios_body",
    Some(pushParams)
  )

  test("200 response") {
    val res = getMosckPushnoyCliet(200, """{"count": 2}""").pushToUser(template, pushParams)
    res match {
      case Failure(_) => fail()
      case Success(_) =>
    }
  }

  test("404 response") {
    val res = getMosckPushnoyCliet(404, "").pushToUser(template, pushParams)
    res match {
      case Success(_) => fail()
      case Failure(_) =>
    }
  }

}
