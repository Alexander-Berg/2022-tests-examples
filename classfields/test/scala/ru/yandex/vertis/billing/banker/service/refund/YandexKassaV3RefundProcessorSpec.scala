package ru.yandex.vertis.billing.banker.service.refund

import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.gens.ExternalModelGens.{
  paymentGen => kassaPaymentGen,
  refundGen => kassaRefundGen,
  PaymentParams,
  RefundParams
}
import ru.yandex.vertis.billing.banker.exceptions.Exceptions.{
  PaymentAlreadyChargedBackException,
  YandexKassaV3BadRequest
}
import ru.yandex.vertis.billing.banker.model.Funds
import ru.yandex.vertis.billing.banker.model.State.StateStatuses
import ru.yandex.vertis.billing.banker.model.gens.{BooleanGen, Producer}
import ru.yandex.vertis.billing.banker.service.refund.YandexKassaV3RefundProcessorSpec.{
  asKassaAmount,
  YandexKassaApiExceptions,
  YandexKassaChargebackApiException
}
import ru.yandex.vertis.billing.banker.service.refund.processor.RefundProcessingGens.{
  onCorrectSource,
  onPaymentWithoutInvoiceId
}
import ru.yandex.vertis.billing.banker.service.refund.processor.RefundProcessor.RefundProcessingException
import ru.yandex.vertis.billing.banker.service.refund.processor.YandexKassaV3RefundProcessor
import ru.yandex.vertis.billing.banker.util.FundUtils
import ru.yandex.vertis.billing.yandexkassa.api.YandexKassaApiV3
import ru.yandex.vertis.billing.yandexkassa.api.YandexKassaApiV3._
import ru.yandex.vertis.billing.yandexkassa.api.model.{IdempotencyKey, PaymentId}
import ru.yandex.vertis.external.yandexkassa.ApiModel
import ru.yandex.vertis.external.yandexkassa.ApiModel.RefundRequest
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future

class YandexKassaV3RefundProcessorSpec
  extends MockFactory
  with Matchers
  with AnyWordSpecLike
  with AsyncSpecBase
  with ScalaCheckPropertyChecks
  with ShrinkLowPriority {

  private val yandexKassa = mock[YandexKassaApiV3]

  private val processor = new YandexKassaV3RefundProcessor(yandexKassa)

  private val RefundInvoiceId = "refund_invoice_id"

  def mockRefund(amount: Funds, success: Boolean = true): Unit = {
    (yandexKassa.refund(_: RefundRequest)(_: IdempotencyKey, _: Traced)).expects(*, *, *).returns {
      val status =
        if (success) {
          Set(ApiModel.Refund.RefundStatus.succeeded)
        } else {
          Set(ApiModel.Refund.RefundStatus.canceled)
        }
      val params = RefundParams(
        id = Some(RefundInvoiceId),
        amount = Some(asKassaAmount(amount)),
        status = status
      )

      Future.successful(kassaRefundGen(params).next)
    }: Unit
  }

  def mockRefundWithKassaApiError(): Unit = {
    (yandexKassa.refund(_: RefundRequest)(_: IdempotencyKey, _: Traced)).expects(*, *, *).returns {
      val exception = Gen.oneOf(YandexKassaApiExceptions).next
      Future.failed(exception)
    }: Unit
  }

  def mockRefundWithChargebackKassaApiError(): Unit = {
    (yandexKassa.refund(_: RefundRequest)(_: IdempotencyKey, _: Traced)).expects(*, *, *).returns {
      Future.failed(YandexKassaChargebackApiException)
    }: Unit
  }

  def mockGetPayment(
      refundedAmount: Option[Option[ApiModel.Amount]] = None,
      status: ApiModel.Payment.PaymentStatus = ApiModel.Payment.PaymentStatus.succeeded): Unit = {
    (yandexKassa.get(_: PaymentId)(_: Traced)).expects(*, *).returns {
      val params = PaymentParams(
        refundedAmount = refundedAmount,
        status = Set(status)
      )
      val payment = kassaPaymentGen(params).next
      Future.successful(Some(payment))
    }: Unit
  }

  def mockGetPaymentMiss(): Unit = {
    (yandexKassa.get(_: PaymentId)(_: Traced)).expects(*, *).returns {
      Future.successful(None)
    }: Unit
  }

  "YandexKassaV3RefundProcessor" should {

    "process" when {
      "all data is correct" in {
        onCorrectSource { source =>
          val success = BooleanGen.next
          mockRefund(source.payment.amount, success)

          val refund = processor.process(source).futureValue
          refund.amount shouldBe source.refundAmount
          refund.invoiceId shouldBe Some(RefundInvoiceId)
          val expectedStatus =
            if (success) {
              StateStatuses.Valid
            } else {
              StateStatuses.Cancelled
            }
          refund.stateStatus shouldBe expectedStatus: Unit
        }
      }
    }

    "fail process" when {
      "payment without invoice id" in {
        onPaymentWithoutInvoiceId { source =>
          intercept[RefundProcessingException] {
            processor.process(source).await
          }
          ()
        }
      }
      "kassa api answer with error" in {
        onCorrectSource { source =>
          mockRefundWithKassaApiError()
          intercept[YandexKassaV3BadRequest] {
            processor.process(source).await
          }
          ()
        }
      }
      "user has already requested a chargeback" in {
        onCorrectSource { source =>
          mockRefundWithChargebackKassaApiError()
          intercept[PaymentAlreadyChargedBackException] {
            processor.process(source).await
          }
          ()
        }
      }
    }

    "sync" when {
      "all data is correct" in {
        onCorrectSource { source =>
          val success = BooleanGen.next
          val refundedAmount =
            if (success) {
              source.expectedRefundedAmount + source.refundAmount
            } else {
              source.expectedRefundedAmount
            }
          mockGetPayment(
            refundedAmount = Some(Some(asKassaAmount(refundedAmount)))
          )

          val refund = processor.sync(source).futureValue
          refund.amount shouldBe source.refundAmount
          refund.invoiceId shouldBe None
          val expectedStatus =
            if (success) {
              StateStatuses.Valid
            } else {
              StateStatuses.Cancelled
            }
          refund.stateStatus shouldBe expectedStatus: Unit
        }
      }
    }

    "not sync" when {
      "payment without invoice id" in {
        onPaymentWithoutInvoiceId { source =>
          intercept[IllegalArgumentException] {
            processor.sync(source).await
          }
          ()
        }
      }
      "without payment" in {
        onCorrectSource { source =>
          mockGetPaymentMiss()
          intercept[IllegalArgumentException] {
            processor.sync(source).await
          }
          ()
        }
      }
      "on cancelled payment" in {
        onCorrectSource { source =>
          mockGetPayment(
            status = ApiModel.Payment.PaymentStatus.canceled
          )
          intercept[IllegalArgumentException] {
            processor.sync(source).await
          }
          ()
        }
      }
      "on unexpected kassa refunded amount" in {
        onCorrectSource { source =>
          val less = BooleanGen.next
          val spoiledRefundedAmount =
            if (less) {
              Gen.chooseNum(0L, source.expectedRefundedAmount - 1)
            } else {
              val total = source.expectedRefundedAmount + source.refundAmount
              Gen.chooseNum(total + 1, total + 10000L)
            }

          mockGetPayment(
            refundedAmount = Some(Some(asKassaAmount(spoiledRefundedAmount.next)))
          )

          intercept[IllegalArgumentException] {
            processor.sync(source).await
          }
          ()
        }
      }
    }

  }

}

object YandexKassaV3RefundProcessorSpec {

  private def asKassaAmount(amount: Funds): ApiModel.Amount =
    ApiModel.Amount
      .newBuilder()
      .setCurrency(FundUtils.RUB)
      .setValue(FundUtils.fromFunds(amount).toDouble)
      .build()

  private val KassaApiError =
    ApiModel.ApiError
      .newBuilder()
      .setType("test error")
      .setId("0")
      .setCode("666")
      .setDescription("test error")
      .setParameter("test parameter")
      .build()

  private val YandexKassaApiExceptions: Seq[YandexKassaApiException] = Seq(
    InvalidCredentialsException(KassaApiError),
    AccessDenyException(KassaApiError),
    TooManyRequestException(KassaApiError),
    InternalServerException(KassaApiError),
    BadRequestException(KassaApiError),
    NotFoundResourceException(KassaApiError)
  )

  private val YandexKassaChargebackApiException = BadRequestException(
    ApiModel.ApiError
      .newBuilder()
      .setType("error")
      .setId("8a802d0f-c089-43e8-a15b-481f93d7428d")
      .setCode("invalid_request")
      .setDescription("This payment can't be refunded: user has requested a chargeback (canceled the transaction)")
      .setParameter("payment_id")
      .build()
  )
}
