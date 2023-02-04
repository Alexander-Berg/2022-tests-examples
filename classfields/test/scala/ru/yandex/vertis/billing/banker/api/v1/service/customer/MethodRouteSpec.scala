package ru.yandex.vertis.billing.banker.api.v1.service.customer

import akka.http.scaladsl.model.{ContentTypes, HttpRequest, StatusCodes}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.banker.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.banker.api.v1.view.PaymentMethodPatchView
import ru.yandex.vertis.billing.banker.model.{
  PaymentMethod,
  PaymentMethodRestriction,
  PaymentSystemId,
  PaymentSystemIds
}
import ru.yandex.vertis.billing.banker.service.PaymentSetup
import ru.yandex.vertis.billing.banker.service.PaymentSystemSupport.MethodFilter
import ru.yandex.vertis.external.yandexkassa.ApiModel.PaymentType
import spray.json.enrichAny

import scala.concurrent.Future

/**
  * Spec on [[MethodRoute]]
  *
  * @author alex-kovalenko
  */
class MethodRouteSpec extends AnyWordSpecLike with RootHandlerSpecBase {

  private lazy val customer = "test_customer"

  import ru.yandex.vertis.billing.banker.api.v1.view.PaymentMethodView.modelIterableUnmarshaller

  override def basePath: String = s"/api/1.x/service/autoru/customer/$customer"

  "/method/gate/{gate} get" should {
    def request(gate: String): HttpRequest = Get(url(s"/method/gate/$gate"))
    "return 404 on bad gate" in {
      request("bad") ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
    "return methods" in {
      val psId = PaymentSystemIds.FreeOfCharge
      val method = PaymentMethod(psId, "free")
      findPaymentSetup(psId).foreach { ps =>
        when(ps.support.getMethods(eq(customer), eq(MethodFilter.All))(?))
          .thenReturn(Future.successful(Seq(method)))
      }

      request(psId.toString) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Iterable[PaymentMethod]]
        response should (have size 1 and contain(method))
      }
    }
  }

  "/method/gate/{gate}/method/{method} get" should {
    def request(psId: String, methodId: String): HttpRequest =
      Get(url(s"/method/gate/$psId/method/$methodId"))
    "return 404 on bad gate" in {
      request("bad", "") ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
    "return methods" in {
      val psId = PaymentSystemIds.FreeOfCharge
      val methodId = "free"
      val method = PaymentMethod(psId, methodId)
      findPaymentSetup(psId).foreach { ps =>
        when(ps.support.getMethods(eq(customer), eq(MethodFilter.ForId(methodId)))(?))
          .thenReturn(Future.successful(Seq(method)))
      }
      request(psId.toString, methodId) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Iterable[PaymentMethod]]
        response should (have size 1 and contain(method))
      }
    }

    "return correct restrictions for corresponding methods of YandexKassaV3" in {
      val psId = PaymentSystemIds.YandexKassaV3
      val methodIds = PaymentType.values.toSeq.map(_.toString)
      val pms: Seq[PaymentMethod] = methodIds.map(PaymentMethod(psId, _))

      pms.foreach { pm =>
        findPaymentSetup(pm.ps).foreach { ps =>
          when(ps.support.getMethods(eq(customer), eq(MethodFilter.ForId(pm.id)))(?))
            .thenReturn(Future.successful(Seq(pm)))
        }

        request(pm.ps.toString, pm.id) ~> defaultHeaders ~> route ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[Iterable[PaymentMethod]]
          response.foreach {
            case method if method.id == PaymentType.sberbank.toString =>
              method.restriction shouldBe Some(PaymentMethodRestriction(Some(1000000)))

            case method if method.id == PaymentType.yandex_money.toString =>
              method.restriction shouldBe Some(PaymentMethodRestriction(Some(1500000)))

            case method if method.id == PaymentType.webmoney.toString =>
              method.restriction shouldBe Some(PaymentMethodRestriction(Some(6000000)))

            case method => method.restriction shouldBe None
          }
        }
      }
    }

    "return restriction None for corresponding methods of other PaymentSystemIds (not YandexKassaV3)" in {
      val ps2Methods = Seq(
        (PaymentSystemIds.YandexKassa, PaymentType.values.map(_.toString).toSeq),
        (PaymentSystemIds.AppStore, Seq("IAP")),
        (PaymentSystemIds.PlayMarket, Seq("IAP")),
        (PaymentSystemIds.DigitalWallet, Seq("ApplePay")),
        (PaymentSystemIds.FreeOfCharge, Seq("free"))
      )

      val pms = ps2Methods.flatMap { case (psId, methods) =>
        methods.map(PaymentMethod(psId, _))
      }

      pms.foreach { pm =>
        findPaymentSetup(pm.ps).foreach { ps =>
          when(ps.support.getMethods(eq(customer), eq(MethodFilter.ForId(pm.id)))(?))
            .thenReturn(Future.successful(Seq(pm)))
        }

        request(pm.ps.toString, pm.id) ~> defaultHeaders ~> route ~> check {
          status shouldBe StatusCodes.OK
          val response = responseAs[Iterable[PaymentMethod]]
          response.foreach(_.restriction shouldBe None)
        }
      }
    }
  }

  "/method/gate/{gate}/id/{method} post" should {
    "return 400 if patch is not present" in {
      val psId = PaymentSystemIds.FreeOfCharge
      val methodId = "free"
      Post(url(s"/method/gate/$psId/id/$methodId")) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }
    "return 501 if update is not supported" in {
      val psId = PaymentSystemIds.FreeOfCharge
      val methodId = "free"
      val patch = PaymentMethod.Patch(None)
      val entity = PaymentMethodPatchView.asView(patch).toJson.compactPrint
      findPaymentSetup(psId).foreach { ps =>
        when(ps.support.updateMethod(?, ?, ?)(?))
          .thenReturn(Future.failed(new UnsupportedOperationException("artificial")))
      }
      Post(url(s"/method/gate/$psId/id/$methodId"))
        .withEntity(ContentTypes.`application/json`, entity) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.NotImplemented
      }
    }
    "return 200" in {
      val psId = PaymentSystemIds.FreeOfCharge
      val methodId = "free"
      val patch = PaymentMethod.Patch(None)
      val entity = PaymentMethodPatchView.asView(patch).toJson.compactPrint
      findPaymentSetup(psId).foreach { ps =>
        when(ps.support.updateMethod(eq(customer), eq(methodId), eq(patch))(?))
          .thenReturn(Future.successful(()))
      }
      Post(url(s"/method/gate/$psId/id/$methodId"))
        .withEntity(ContentTypes.`application/json`, entity) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  "/method/gate/{gate}/id/{method} delete" should {
    "return 501 if deletion is not supported" in {
      val psId = PaymentSystemIds.FreeOfCharge
      val methodId = "free"
      findPaymentSetup(psId).foreach { ps =>
        when(ps.support.deleteMethod(eq(customer), eq(methodId))(?))
          .thenReturn(Future.failed(new UnsupportedOperationException("artificial")))
      }
      Delete(url(s"/method/gate/$psId/id/$methodId")) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.NotImplemented
      }
    }
    "return 200" in {
      val psId = PaymentSystemIds.FreeOfCharge
      val methodId = "free"
      findPaymentSetup(psId).foreach { ps =>
        when(ps.support.deleteMethod(eq(customer), eq(methodId))(?))
          .thenReturn(Future.successful(()))
      }
      Delete(url(s"/method/gate/$psId/id/$methodId")) ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }

  private def findPaymentSetup(psId: PaymentSystemId): Option[PaymentSetup] =
    allSetups.find(_.support.psId == psId)

}
