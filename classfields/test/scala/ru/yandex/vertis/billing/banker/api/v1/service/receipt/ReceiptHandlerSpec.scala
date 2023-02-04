package ru.yandex.vertis.billing.banker.api.v1.service.receipt

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.billing.banker.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.banker.api.v1.view.{ReceiptCheckUrlView, RefundReceiptCheckUrlsView}
import ru.yandex.vertis.billing.banker.model.{ReceiptCheckUrl, RefundReceiptCheckUrls}
import ru.yandex.vertis.billing.banker.service.impl.ReceiptServiceImpl.NoCheckUrlsFoundException

import scala.concurrent.Future

class ReceiptHandlerSpec extends AnyWordSpecLike with RootHandlerSpecBase {

  override def basePath: String = s"/api/1.x/service/autoru/receipt"

  private val paymentId = "paymentId123"
  private val checkUrl = "url"

  "/ GET" should {
    "return not empty check url" in {
      when(backend.receiptService.getCheckUrl(eq(paymentId))(?))
        .thenReturn(Future.successful(ReceiptCheckUrl(checkUrl)))
      Get(basePath + s"/payment/$paymentId") ~> defaultHeaders ~> route ~> check {
        responseAs[ReceiptCheckUrlView] shouldBe ReceiptCheckUrlView(checkUrl)
      }
    }

    "return empty check url" in {
      when(backend.receiptService.getCheckUrl(eq(paymentId))(?))
        .thenReturn(Future.failed(NoCheckUrlsFoundException("123")))
      Get(basePath + s"/payment/$paymentId") ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

  "/refund GET" should {
    "return not empty check url" in {
      when(backend.receiptService.getRefundCheckUrls(eq(paymentId))(?))
        .thenReturn(Future.successful(RefundReceiptCheckUrls(List(checkUrl))))
      Get(basePath + s"/refund/payment/$paymentId") ~> defaultHeaders ~> route ~> check {
        responseAs[RefundReceiptCheckUrlsView] shouldBe RefundReceiptCheckUrlsView(List(checkUrl))
      }
    }

    "return 404 for empty check url" in {
      when(backend.receiptService.getRefundCheckUrls(eq(paymentId))(?))
        .thenReturn(Future.failed(NoCheckUrlsFoundException("123")))
      Get(basePath + s"/refund/payment/$paymentId") ~> defaultHeaders ~> route ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

}
