package ru.yandex.vertis.billing.banker.api.v1.service.methods

import akka.http.scaladsl.model.{ContentTypes, HttpRequest, StatusCodes}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.banker.api.base_admin.RootHandlerSpecBase
import ru.yandex.vertis.billing.banker.api.view.MethodDowntimePatch
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds
import ru.yandex.vertis.billing.banker.service.DowntimePaymentService

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class HandlerSpec extends AnyWordSpecLike with RootHandlerSpecBase {

  private val psId = PaymentSystemIds.YandexKassaV3
  private val downtimeService = mock[DowntimePaymentService]
  private val method = "bank_card"

  when(backend.paymentSystemRegistry.get(eq(psId)))
    .thenReturn(Future.successful(downtimeService))

  override def basePath: String = "/api/1.x/service/autoru/methods"

  private val commonPathPrefix = s"/gate/${psId.toString}/method/$method"

  "disable" should {
    "disable method" in {
      when(downtimeService.disable(eq(method)))
        .thenReturn(Future.successful(true))

      Put(url(commonPathPrefix + "/disable")) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "downtime" should {
    "disable method temporarily" in {
      import spray.json.enrichAny
      val timeMinutes = 60
      when(downtimeService.downtime(eq(method), eq(timeMinutes.minutes)))
        .thenReturn(Future.successful(true))

      val downtime = MethodDowntimePatch.asView(60.minutes).toJson.compactPrint
      Put(url(commonPathPrefix + s"/downtime")).withEntity(ContentTypes.`application/json`, downtime) ~>
        route ~> check {
          status shouldBe StatusCodes.OK
        }
    }
  }

  "enable" should {
    "enable method" in {
      when(downtimeService.enable(eq(method)))
        .thenReturn(Future.successful(true))

      Put(url(commonPathPrefix + "/enable")) ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }
}
