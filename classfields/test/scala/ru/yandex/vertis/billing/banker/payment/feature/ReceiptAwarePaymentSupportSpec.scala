package ru.yandex.vertis.billing.banker.payment.feature

import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.config.{
  ReceiptDeliverySettings,
  ReceiptEmailDeliverySettings,
  ReceiptSmsDeliverySettings,
  SmsClientSettings,
  UrlShortenerSettings
}
import ru.yandex.vertis.billing.banker.config.ReceiptDeliverySettings.SourceCheckerTypes
import ru.yandex.vertis.billing.banker.exceptions.Exceptions.EmailBadFormatException
import ru.yandex.vertis.billing.banker.mailing.SmtpConfig
import ru.yandex.vertis.billing.banker.model.PaymentRequest.{EmptyForm, ReceiptData, Source}
import ru.yandex.vertis.billing.banker.model.RefundPaymentRequest.SourceData
import ru.yandex.vertis.billing.banker.model.{Account, AccountId, User}
import ru.yandex.vertis.billing.banker.payment.feature.ReceiptAwarePaymentSupportSpec.{
  accountWithEmailAndPhone,
  accountWithEmailOnly,
  accountWithPhoneOnly,
  accountWithoutEmailAndPhone,
  mockReceiptAwareSupport,
  sourceDataWithEmailAndPhone,
  sourceDataWithEmailOnly,
  sourceDataWithPhoneOnly,
  sourceDataWithoutEmailAndPhone,
  sourceWithEmailAndPhone,
  sourceWithEmailOnly,
  sourceWithInvalidEmailAndValidPhone,
  sourceWithInvalidEmailOnly,
  sourceWithPhoneOnly,
  sourceWithoutEmailAndPhone,
  toExpected,
  Customer,
  ExpectedForm,
  PaymentMethodId
}
import ru.yandex.vertis.billing.banker.payment.util.PaymentSystemSupportMockProvider.BasePaymentSystemSupportMock
import ru.yandex.vertis.billing.banker.service.AccountService
import ru.yandex.vertis.billing.banker.util.{RequestContext, UserContext}
import ru.yandex.vertis.billing.banker.util.email.EmailValidator.isValid
import ru.yandex.vertis.billing.banker.model.gens.{
  paymentRequestGen,
  paymentRequestSourceGen,
  refundRequestSourceDataGen,
  AccountGen,
  PaymentRequestSourceParams,
  Producer
}

import scala.concurrent.{ExecutionContext, Future}

class ReceiptAwarePaymentSupportSpec extends AnyWordSpec with Matchers with AsyncSpecBase {

  private def checkRequestCall(source: Source, account: Account): Unit = {
    val form = makeCall(source, account).futureValue
    form shouldBe ExpectedForm: Unit
  }

  private def makeCall(source: Source, account: Account) = {
    val support = mockReceiptAwareSupport

    support.mockGetAccount(account)

    val expectedSource = toExpected(source, account)
    support.mockRequest(expectedSource, ExpectedForm)

    support.request(Customer, PaymentMethodId, source)
  }

  implicit val userContext: UserContext =
    UserContext("ReceiptAwarePaymentSupportSpec", "I am human. Trust me :)")

  private def checkRefundCall(sourceData: SourceData, account: Account): Unit = {
    val support = mockReceiptAwareSupport

    val request = paymentRequestGen().next
    support.mockGetPaymentRequest(request)
    support.mockGetAccount(account)

    val expectedSource = toExpected(sourceData, account)
    support.mockRefund(expectedSource)

    support.refund(Customer, PaymentMethodId, 100L, sourceData).futureValue
  }

  "ReceiptAwarePaymentSupport" should {
    "correctly enrich receipt in source on request method call" when {
      "source data without email and phone and account with email and phone" in {
        checkRequestCall(sourceWithoutEmailAndPhone, accountWithEmailAndPhone)
      }
      "source data without email and phone and account with email only" in {
        checkRequestCall(sourceWithoutEmailAndPhone, accountWithEmailOnly)
      }
      "source data without email and phone and account with phone only" in {
        checkRequestCall(sourceWithoutEmailAndPhone, accountWithPhoneOnly)
      }
      "source data without email and phone and account without email and phone" in {
        checkRequestCall(sourceWithoutEmailAndPhone, accountWithoutEmailAndPhone)
      }
      "source data with email only and account with email and phone" in {
        checkRequestCall(sourceWithEmailOnly, accountWithEmailAndPhone)
      }
      "source data with email only and account with email only" in {
        checkRequestCall(sourceWithEmailOnly, accountWithEmailOnly)
      }
      "source data with email only and account with phone only" in {
        checkRequestCall(sourceWithEmailOnly, accountWithPhoneOnly)
      }
      "source data with email only and account without email and phone" in {
        checkRequestCall(sourceWithEmailOnly, accountWithoutEmailAndPhone)
      }
      "source data with phone only and account with email and phone" in {
        checkRequestCall(sourceWithPhoneOnly, accountWithEmailAndPhone)
      }
      "source data with phone only and account with email only" in {
        checkRequestCall(sourceWithPhoneOnly, accountWithEmailOnly)
      }
      "source data with phone only and account with phone only" in {
        checkRequestCall(sourceWithPhoneOnly, accountWithPhoneOnly)
      }
      "source data with phone only and account without email and phone" in {
        checkRequestCall(sourceWithPhoneOnly, accountWithoutEmailAndPhone)
      }
      "source data with email and phone and account with email and phone" in {
        checkRequestCall(sourceWithEmailAndPhone, accountWithEmailAndPhone)
      }
      "source data with email and phone and account with email only" in {
        checkRequestCall(sourceWithEmailAndPhone, accountWithEmailOnly)
      }
      "source data with email and phone and account with phone only" in {
        checkRequestCall(sourceWithEmailAndPhone, accountWithPhoneOnly)
      }
      "source data with email and phone and account without email and phone" in {
        checkRequestCall(sourceWithEmailAndPhone, accountWithoutEmailAndPhone)
      }

      "fail on invalid email" in {
        intercept[EmailBadFormatException] {
          makeCall(sourceWithInvalidEmailOnly, accountWithoutEmailAndPhone).await
        }
      }
      "fallback to phone invalid email" in {
        checkRequestCall(sourceWithInvalidEmailAndValidPhone, accountWithoutEmailAndPhone)
      }
    }
    "correctly enrich receipt in source data data on request method call" when {
      "source data without email and phone and account with email and phone" in {
        checkRefundCall(sourceDataWithoutEmailAndPhone, accountWithEmailAndPhone)
      }
      "source data without email and phone and account with email only" in {
        checkRefundCall(sourceDataWithoutEmailAndPhone, accountWithEmailOnly)
      }
      "source data without email and phone and account with phone only" in {
        checkRefundCall(sourceDataWithoutEmailAndPhone, accountWithPhoneOnly)
      }
      "source data without email and phone and account without email and phone" in {
        checkRefundCall(sourceDataWithoutEmailAndPhone, accountWithoutEmailAndPhone)
      }
      "source data with email only and account with email and phone" in {
        checkRefundCall(sourceDataWithEmailOnly, accountWithEmailAndPhone)
      }
      "source data with email only and account with email only" in {
        checkRefundCall(sourceDataWithEmailOnly, accountWithEmailOnly)
      }
      "source data with email only and account with phone only" in {
        checkRefundCall(sourceDataWithEmailOnly, accountWithPhoneOnly)
      }
      "source data with email only and account without email and phone" in {
        checkRefundCall(sourceDataWithEmailOnly, accountWithoutEmailAndPhone)
      }
      "source data with phone only and account with email and phone" in {
        checkRefundCall(sourceDataWithPhoneOnly, accountWithEmailAndPhone)
      }
      "source data with phone only and account with email only" in {
        checkRefundCall(sourceDataWithPhoneOnly, accountWithEmailOnly)
      }
      "source data with phone only and account with phone only" in {
        checkRefundCall(sourceDataWithPhoneOnly, accountWithPhoneOnly)
      }
      "source data with phone only and account without email and phone" in {
        checkRefundCall(sourceDataWithPhoneOnly, accountWithoutEmailAndPhone)
      }
      "source data with email and phone and account with email and phone" in {
        checkRefundCall(sourceDataWithEmailAndPhone, accountWithEmailAndPhone)
      }
      "source data with email and phone and account with email only" in {
        checkRefundCall(sourceDataWithEmailAndPhone, accountWithEmailOnly)
      }
      "source data with email and phone and account with phone only" in {
        checkRefundCall(sourceDataWithEmailAndPhone, accountWithPhoneOnly)
      }
      "source data with email and phone and account without email and phone" in {
        checkRefundCall(sourceDataWithEmailAndPhone, accountWithoutEmailAndPhone)
      }
    }
  }
}

object ReceiptAwarePaymentSupportSpec extends Matchers {

  private val ExpectedForm = EmptyForm("expected_id")

  private val InvalidReceiptEmail = "rrdasdadmail.ru"

  private val ReceiptEmail = "receipt@mail.ru"
  private val ReceiptPhone = "88005553535"

  private val AccountEmail = "account@mail.ru"
  private val AccountPhone = "99006664646"

  private def sourceWithReceipt: Source = {
    paymentRequestSourceGen(PaymentRequestSourceParams(withReceipt = Some(true))).next
  }

  private def changeReceipt(source: Source)(action: ReceiptData => ReceiptData): Source = {
    source.optReceiptData match {
      case Some(receipt) =>
        val changedReceipt = action(receipt)
        source.copy(
          optReceiptData = Some(changedReceipt)
        )
      case None =>
        fail(s"Unexpected source data state $source")
    }
  }

  private def sourceWithoutEmailAndPhone: Source = {
    changeReceipt(sourceWithReceipt) { receipt =>
      receipt.copy(
        email = None,
        phone = None
      )
    }
  }

  private def sourceWithInvalidEmailOnly: Source = {
    changeReceipt(sourceWithReceipt) { receipt =>
      receipt.copy(
        email = Some(InvalidReceiptEmail),
        phone = None
      )
    }
  }

  private def sourceWithInvalidEmailAndValidPhone: Source = {
    changeReceipt(sourceWithReceipt) { receipt =>
      receipt.copy(
        email = Some(InvalidReceiptEmail),
        phone = Some(ReceiptPhone)
      )
    }
  }

  private def sourceWithEmailOnly: Source = {
    changeReceipt(sourceWithReceipt) { receipt =>
      receipt.copy(
        email = Some(ReceiptEmail),
        phone = None
      )
    }
  }

  private def sourceWithPhoneOnly: Source = {
    changeReceipt(sourceWithReceipt) { receipt =>
      receipt.copy(
        email = None,
        phone = Some(ReceiptPhone)
      )
    }
  }

  private def sourceWithEmailAndPhone: Source = {
    changeReceipt(sourceWithReceipt) { receipt =>
      receipt.copy(
        email = Some(ReceiptEmail),
        phone = Some(ReceiptPhone)
      )
    }
  }

  private def sourceDataWithReceipt: SourceData = {
    refundRequestSourceDataGen(withReceipt = Some(true)).next
  }

  private def changeReceipt(source: SourceData)(action: ReceiptData => ReceiptData): SourceData = {
    val changedReceipt = source.receipt.map(action)
    source.copy(
      receipt = changedReceipt
    )
  }

  private def sourceDataWithoutEmailAndPhone: SourceData = {
    changeReceipt(sourceDataWithReceipt) { receipt =>
      receipt.copy(
        email = None,
        phone = None
      )
    }
  }

  private def sourceDataWithEmailOnly: SourceData = {
    changeReceipt(sourceDataWithReceipt) { receipt =>
      receipt.copy(
        email = Some(ReceiptEmail),
        phone = None
      )
    }
  }

  private def sourceDataWithPhoneOnly: SourceData = {
    changeReceipt(sourceDataWithReceipt) { receipt =>
      receipt.copy(
        email = None,
        phone = Some(ReceiptPhone)
      )
    }
  }

  private def sourceDataWithEmailAndPhone: SourceData = {
    changeReceipt(sourceDataWithReceipt) { receipt =>
      receipt.copy(
        email = Some(ReceiptEmail),
        phone = Some(ReceiptPhone)
      )
    }
  }

  private def changeAccountProperties(account: Account)(action: Account.Properties => Account.Properties): Account = {
    val changedProperties = action(account.properties)
    account.copy(properties = changedProperties)
  }

  private def accountWithoutEmailAndPhone: Account = {
    changeAccountProperties(AccountGen.next) { properties =>
      properties.copy(
        email = None,
        phone = None
      )
    }
  }

  private def accountWithEmailAndPhone: Account = {
    changeAccountProperties(AccountGen.next) { properties =>
      properties.copy(
        email = Some(AccountEmail),
        phone = Some(AccountPhone)
      )
    }
  }

  private def accountWithEmailOnly: Account = {
    changeAccountProperties(AccountGen.next) { properties =>
      properties.copy(
        email = Some(AccountEmail),
        phone = None
      )
    }
  }

  private def accountWithPhoneOnly: Account = {
    changeAccountProperties(AccountGen.next) { properties =>
      properties.copy(
        email = None,
        phone = Some(AccountPhone)
      )
    }
  }

  private val Customer = "user"
  private val PaymentMethodId = "payment_method_id"

  private def toExpected(source: Source, account: Account): Source = {
    val changedSource = changeReceipt(source) { receipt =>
      receipt.copy(
        email = receipt.email.orElse(account.properties.email).filter(isValid),
        phone = receipt.phone.orElse(account.properties.phone)
      )
    }
    if (!changedSource.isReceiptEmailDefined && !changedSource.isReceiptPhoneDefined) {
      changeReceipt(changedSource) { receipt =>
        receipt.copy(
          email = Some(FallbackEmail)
        )
      }
    } else {
      changedSource
    }
  }

  private def toExpected(sourceData: SourceData, account: Account): SourceData = {
    val changedSource = changeReceipt(sourceData) { receipt =>
      receipt.copy(
        email = receipt.email.orElse(account.properties.email),
        phone = receipt.phone.orElse(account.properties.phone)
      )
    }
    if (!changedSource.receipt.forall(_.isReceiptEmailOrPhoneDefined)) {
      changeReceipt(changedSource) { receipt =>
        receipt.copy(
          email = Some(FallbackEmail)
        )
      }
    } else {
      changedSource
    }
  }

  private val FallbackEmail = "fallback@mail.ru"

  type MockedPaymentSupport = BasePaymentSystemSupportMock with ReceiptAwarePaymentSupportMock

  private def mockReceiptAwareSupport(implicit ec: ExecutionContext): MockedPaymentSupport = {
    new BasePaymentSystemSupportMock() with ReceiptAwarePaymentSupportMock
  }

  trait ReceiptAwarePaymentSupportMock extends ReceiptAwarePaymentSupport with MockFactory {

    override protected val accounts: AccountService = mock[AccountService]

    def mockGetAccount(account: Account): Unit = {
      (accounts
        .get(_: User, _: AccountId)(_: RequestContext))
        .expects(*, *, *)
        .returns(Future.successful(account)): Unit
    }

    def mockGetAccountById(account: Account): Unit = {
      (accounts
        .get(_: AccountId)(_: RequestContext))
        .expects(*, *)
        .returns(Future.successful(Seq(account))): Unit
    }

    override protected val receiptDeliverySettings: ReceiptDeliverySettings = {
      val mailSettings = ReceiptEmailDeliverySettings(
        mock[SmtpConfig],
        Some(FallbackEmail)
      )

      val smsSettings = ReceiptSmsDeliverySettings(
        mock[SmsClientSettings],
        mock[UrlShortenerSettings]
      )

      ReceiptDeliverySettings(
        SourceCheckerTypes.AllowAll,
        Some(mailSettings),
        Some(smsSettings)
      )
    }

  }

}
