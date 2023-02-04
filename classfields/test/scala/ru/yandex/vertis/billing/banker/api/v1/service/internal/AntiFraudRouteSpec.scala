package ru.yandex.vertis.billing.banker.api.v1.service.internal

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import com.google.protobuf.util.JsonFormat
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.banker.model.ApiModel.{GetPaymentReportsRequest, GetPaymentReportsResponse}
import ru.yandex.vertis.billing.banker.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.banker.dao.YandexKassaV3AntiFraudDao
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport._

import scala.jdk.CollectionConverters._

/**
 * @author vitya-smirnov
 */
class AntiFraudRouteSpec extends AnyWordSpecLike with RootHandlerSpecBase {

  override def basePath: String = s"/api/1.x/service/autoru/internal/anti-fraud"

  private val jsonPrinter = JsonFormat.printer().omittingInsignificantWhitespace()

  "/paymentReports POST" should {
    "return BadRequest (400)" when {
      "empty body provided" in {
        Post(url("/paymentReports")) ~> defaultHeaders ~> route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
      "no necessary headers passed" in {
        Post(url("/paymentReports")) ~> route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
      "no external payment request ids was given" in {
        post() ~> defaultHeaders ~> route ~> check {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }
    "return OK (200)" when {
      "at least one external payment request was given even if it does not exist" in {
        post("non-existing-external-payment-request-id") ~> route ~> check {
          status shouldBe StatusCodes.OK
        }
      }
    }
    "returns exactly what you want" when {
      "some ids was found, but some not" in {
        val expectedInvoiceIds = List("1", "3").map(actualReport(_).invoiceId)
        post("1", "3", "la-la-la") ~> route ~> check {
          status.shouldBe(StatusCodes.OK)
          val result = responseAs[GetPaymentReportsResponse]
          result.getFoundList.asScala
            .map(_.getPayment.getInvoiceId)
            .should(
              contain theSameElementsAs expectedInvoiceIds
            )
          result.getNonExistingExternalPaymentRequestIdsList.asScala
            .should(
              contain theSameElementsAs List("la-la-la")
            )
        }
      }
    }
    "item was found" in {
      val expected = actualReport("11")
      post("11") ~> route ~> check {
        status.shouldBe(StatusCodes.OK)
        val result = responseAs[GetPaymentReportsResponse]
        result.getFoundList.size() shouldBe 1
        result.getNonExistingExternalPaymentRequestIdsList shouldBe empty
        result.getFound(0).getPayment.getInvoiceId shouldBe expected.invoiceId
      }
    }
  }

  private def post(externalPaymentRequestIds: String*) =
    Post(url("/paymentReports")).withEntity(
      ContentTypes.`application/json`,
      jsonPrinter.print {
        GetPaymentReportsRequest
          .newBuilder()
          .setYandexkassaV3Request(
            GetPaymentReportsRequest.YandexKassaRequest
              .newBuilder()
              .addAllExternalPaymentRequestIds(externalPaymentRequestIds.asJava)
          )
      }
    ) ~> defaultHeaders

  private def actualReport(externalPaymentRequestId: String) =
    YandexKassaV3AntiFraudDao.Fake.reportsByExternalPaymentRequestId
      .find(_.externalPaymentRequestId == externalPaymentRequestId)
      .get
}
