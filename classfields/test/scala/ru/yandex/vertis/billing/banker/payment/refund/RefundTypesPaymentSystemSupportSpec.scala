package ru.yandex.vertis.billing.banker.payment.refund

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{ReceiptData, Targets}
import ru.yandex.vertis.billing.banker.model.RefundPaymentRequest.SourceData
import ru.yandex.vertis.billing.banker.model.{Funds, PaymentRequest, PaymentSystemIds}
import ru.yandex.vertis.billing.banker.payment.feature.{FullAndWalletPartlyRefundOnlySupport, FullRefundOnlySupport}
import ru.yandex.vertis.billing.banker.payment.util.PaymentSystemSupportMockProvider.BasePaymentSystemSupportMock
import ru.yandex.vertis.billing.banker.model.gens.{
  paymentRequestGen,
  PaymentRequestParams,
  PaymentRequestSourceParams,
  Producer,
  StateParams
}
import ru.yandex.vertis.billing.banker.util.UserContext

class RefundTypesPaymentSystemSupportSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  private def validPaymentRequest(amount: Funds): PaymentRequest = {
    val sourceParams = PaymentRequestSourceParams(amount = Some(amount))
    val stateParams = StateParams(amount = Some(amount))
    val requestParams = PaymentRequestParams(
      source = sourceParams,
      state = Some(stateParams)
    ).withReceipt
    val requestGen = paymentRequestGen(requestParams)
    requestGen.next
  }

  implicit private val Context = UserContext("0", "user")

  private def getSourceData(receiptData: ReceiptData): SourceData = {
    SourceData("Full refund", None, None, Some(receiptData))
  }

  "FullRefundOnlySupportSpec" should {
    "process refund" when {
      "call refund method with correct amount" in {
        val pss = new BasePaymentSystemSupportMock() with FullRefundOnlySupport

        val amount = 1000L
        val request = validPaymentRequest(amount)

        pss.mockPsId(PaymentSystemIds.YandexKassaV3)
        pss.mockGetPaymentRequest(request)

        val sourceData = getSourceData(request.source.optReceiptData.get)
        pss.mockRefund(sourceData)

        pss.refund("user", request.id, amount, sourceData).futureValue
      }
      "call fullRefund method with correct amount" in {
        val pss = new BasePaymentSystemSupportMock() with FullRefundOnlySupport

        val amount = 1000L
        val request = validPaymentRequest(amount)

        pss.mockPsId(PaymentSystemIds.YandexKassaV3)
        pss.mockGetPaymentRequest(request)
        pss.mockGetPaymentRequest(request)

        val sourceData = getSourceData(request.source.optReceiptData.get)
        pss.mockRefund(sourceData)

        pss.fullRefund("user", request.id, Some(sourceData.comment), None).futureValue
      }
      "call refund method with correct amount (FullAndWalletPartlyRefundOnlySupport)" in {
        val pss = new BasePaymentSystemSupportMock() with FullAndWalletPartlyRefundOnlySupport

        val amount = 1000L
        val request = validPaymentRequest(amount)

        pss.mockPsId(PaymentSystemIds.YandexKassaV3)
        pss.mockGetPaymentRequest(request)

        val sourceData = getSourceData(request.source.optReceiptData.get)
        pss.mockRefund(sourceData)

        pss.refund("user", request.id, amount, sourceData).futureValue
      }
      "call refund method partly refund of wallet payment" in {
        val pss = new BasePaymentSystemSupportMock() with FullAndWalletPartlyRefundOnlySupport

        val amount = 1000L
        val half = amount / 2
        val request = validPaymentRequest(amount)
        val walletRequest = {
          val context = request.source.context.copy(target = Targets.Wallet)
          val source = request.source.copy(context = context)
          request.copy(source = source)
        }

        pss.mockPsId(PaymentSystemIds.YandexKassaV3)
        pss.mockGetPaymentRequest(walletRequest)

        val sourceData = getSourceData(walletRequest.source.optReceiptData.get)
        pss.mockRefund(sourceData)

        pss.refund("user", walletRequest.id, half, sourceData).futureValue
      }
    }
    "not process refund" when {
      "call fullRefund method without receipt state" in {
        val pss = new BasePaymentSystemSupportMock() with FullRefundOnlySupport

        val amount = 1000L
        val valuableTargets = Targets.values.toSeq.filter(_ != Targets.SecurityDeposit)
        val context = Gen.oneOf(valuableTargets).map(PaymentRequest.Context).next
        val sourceParams = PaymentRequestSourceParams(amount = Some(amount), context = Some(context))
        val requestParams = PaymentRequestParams(
          state = None,
          source = sourceParams
        ).withoutReceipt
        val requestGen = paymentRequestGen(requestParams)
        val request = requestGen.next

        pss.mockGetPaymentRequest(request)

        intercept[IllegalArgumentException] {
          pss.fullRefund("user", request.id, Some("comment"), None).await
        }
      }
      "call fullRefund method with SecurityDeposit target and receipt" in {
        val pss = new BasePaymentSystemSupportMock() with FullRefundOnlySupport

        val amount = 1000L
        val context = PaymentRequest.Context(Targets.SecurityDeposit)
        val sourceParams = PaymentRequestSourceParams(amount = Some(amount), context = Some(context))
        val requestParams = PaymentRequestParams(
          state = None,
          source = sourceParams
        ).withReceipt
        val requestGen = paymentRequestGen(requestParams)
        val request = requestGen.next

        pss.mockGetPaymentRequest(request)
        pss.mockGetPaymentRequest(request)

        intercept[IllegalArgumentException] {
          pss.fullRefund("user", request.id, Some("comment"), None).await
        }
      }
      "call fullRefund method without payment state" in {
        val pss = new BasePaymentSystemSupportMock() with FullRefundOnlySupport

        val amount = 1000L
        val sourceParams = PaymentRequestSourceParams(amount = Some(amount))
        val requestParams = PaymentRequestParams(
          state = None,
          source = sourceParams
        ).withReceipt
        val requestGen = paymentRequestGen(requestParams)
        val request = requestGen.next

        pss.mockGetPaymentRequest(request)
        pss.mockGetPaymentRequest(request)

        intercept[IllegalArgumentException] {
          pss.fullRefund("user", request.id, Some("comment"), None).await
        }
      }
      "call refund method without payment state" in {
        val pss = new BasePaymentSystemSupportMock() with FullRefundOnlySupport

        val amount = 1000L
        val sourceParams = PaymentRequestSourceParams(amount = Some(amount))
        val requestParams = PaymentRequestParams(
          state = None,
          source = sourceParams
        ).withReceipt
        val requestGen = paymentRequestGen(requestParams)
        val request = requestGen.next

        pss.mockGetPaymentRequest(request)

        val sourceData = getSourceData(request.source.optReceiptData.get)

        intercept[IllegalArgumentException] {
          pss.refund("user", request.id, amount, sourceData).await
        }
      }
      "call refund method with invalid amount" in {
        val pss = new BasePaymentSystemSupportMock() with FullRefundOnlySupport

        val amount = 1000L
        val half = amount / 2
        val request = validPaymentRequest(amount)

        pss.mockPsId(PaymentSystemIds.YandexKassaV3)
        pss.mockGetPaymentRequest(request)

        val sourceData = getSourceData(request.source.optReceiptData.get)
        pss.mockRefund(sourceData)

        intercept[IllegalArgumentException] {
          pss.refund("user", request.id, half, sourceData).await
        }
      }
      "call refund method for non wallet with FullAndWalletPartlyRefundOnlySupport" in {
        val pss = new BasePaymentSystemSupportMock() with FullAndWalletPartlyRefundOnlySupport

        val amount = 1000L
        val half = amount / 2
        val request = validPaymentRequest(amount)
        val nonWalletRequest = {
          val context = request.source.context.copy(target = Targets.Purchase)
          val source = request.source.copy(context = context)
          request.copy(source = source)
        }

        pss.mockPsId(PaymentSystemIds.YandexKassaV3)
        pss.mockGetPaymentRequest(nonWalletRequest)

        val sourceData = getSourceData(nonWalletRequest.source.optReceiptData.get)
        pss.mockRefund(sourceData)

        intercept[IllegalArgumentException] {
          pss.refund("user", nonWalletRequest.id, half, sourceData).await
        }
      }
    }
  }

}
