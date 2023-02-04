package ru.yandex.vertis.billing.banker.notification.v1.service

import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.model.State.{
  BytesNotificationResponse,
  EmptyNotificationResponse,
  RedirectNotificationResponse
}
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds
import ru.yandex.vertis.billing.banker.notification.RootHandlerSpecBase

import scala.concurrent.Future

/**
  * Spec on [[Handler]]
  *
  * @author ruslansd
  */
class HandlerSpec extends AnyWordSpecLike with RootHandlerSpecBase with AsyncSpecBase {

  override def basePath: String = "/api/1.x/service/autoru"

  private val gate = PaymentSystemIds.Robokassa
  private val setup = backend.psregistry.get(gate).futureValue

  "post notification" should {
    val request = Post(url(s"/$gate/command"))
    val entity = "response notification"

    "successfully push (BytesNotificationResponse)" in {
      val response =
        BytesNotificationResponse(ContentTypes.`text/plain(UTF-8)`.toString(), entity.getBytes)
      when(setup.support.parse(?)(?))
        .thenReturn(Future.successful(response))
      request.withEntity("entity") ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
          val response = responseAs[String]
          response shouldBe entity
        }
    }

    "successfully push (EmptyNotificationesponse)" in {
      val response = EmptyNotificationResponse
      when(setup.support.parse(?)(?))
        .thenReturn(Future.successful(response))
      request.withEntity("entity") ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.OK
        }
    }

    "successfully push (Redirect)" in {
      val response = RedirectNotificationResponse("http://redirect")
      when(setup.support.parse(?)(?))
        .thenReturn(Future.successful(response))
      request.withEntity("entity") ~>
        defaultHeaders ~>
        route ~>
        check {
          status shouldBe StatusCodes.Found
          header(Location.name) shouldBe Some(Location(response.redirect))
        }
    }
  }

}
