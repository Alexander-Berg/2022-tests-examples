package ru.yandex.vertis.billing.banker.service.impl

import com.google.common.base.Charsets
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.config.ReceiptParameters
import ru.yandex.vertis.billing.banker.model.ReceiptSentStatuses
import ru.yandex.vertis.billing.banker.mailing.JavaMailer
import ru.yandex.vertis.billing.receipt.BalanceReceipt
import ru.yandex.vertis.billing.banker.sms.SmsClient
import ru.yandex.vertis.mockito.MockitoSupport
import org.mockito.Mockito.{never, reset, times, verify}
import ru.yandex.vertis.billing.banker.config.ReceiptSettings.StaticReceiptParametersProvider
import ru.yandex.vertis.billing.banker.model.gens.{receiptGen, Producer}
import ru.yandex.vertis.billing.banker.service.ReceiptDeliveryService.{
  SendReceiptByEmailException,
  SendReceiptBySmsException
}
import ru.yandex.vertis.billing.banker.service.{ReceiptService, ReceiptSmsRenderService}
import ru.yandex.vertis.billing.receipt.model.{TaxTypes, TaxationTypes}
import ru.yandex.vertis.billing.banker.sms.SmsClient.{SmsClientResponse, SmsClientResponseStatuses}

import scala.concurrent.Future

class ReceiptApiSendMethodSpec
  extends AnyWordSpec
  with Matchers
  with AsyncSpecBase
  with MockitoSupport
  with BeforeAndAfterEach {

  val balanceApiMock: BalanceReceipt =
    mock[BalanceReceipt]

  def mockBalanceApiRender(): Unit =
    when(balanceApiMock.render(?, ?))
      .thenReturn(Future.successful("test".getBytes(Charsets.UTF_8))): Unit

  val receiptSmsRenderService: ReceiptSmsRenderService =
    mock[ReceiptSmsRenderService]

  def mockShortUrl(throwable: Throwable): Unit =
    when(receiptSmsRenderService.shortUrl(?))
      .thenReturn(Future.failed(throwable)): Unit

  def mockShortUrl(): Unit =
    when(receiptSmsRenderService.shortUrl(?))
      .thenReturn(Future.successful("test")): Unit

  def mockRender(): Unit =
    when(receiptSmsRenderService.render(?, ?))
      .thenReturn(Future.successful("test")): Unit

  val receiptService: ReceiptService =
    mock[ReceiptService]

  def mockSetShortUrl(): Unit =
    when(receiptService.setShortUrl(?, ?)(?))
      .thenReturn(Future.successful(())): Unit

  val mailerMock: JavaMailer =
    mock[JavaMailer]

  def mockEmailSendFail(throwable: Throwable): Unit = {
    when(mailerMock.send(?)).thenReturn {
      Future.failed(throwable)
    }: Unit
  }

  def mockEmailSendSuccess(): Unit = {
    when(mailerMock.send(?)).thenReturn {
      Future.successful(())
    }: Unit
  }

  val smsClientMock: SmsClient =
    mock[SmsClient]

  def mockSmsSendFail(throwable: Throwable): Unit = {
    when(smsClientMock.sendText(?, ?, ?)).thenReturn {
      Future.failed(throwable)
    }: Unit
  }

  def mockSmsSend(response: SmsClientResponse): Unit = {
    when(smsClientMock.sendText(?, ?, ?)).thenReturn {
      Future.successful(response)
    }: Unit
  }

  def mockSmsSendSuccess(): Unit =
    mockSmsSend(SmsClientResponse(SmsClientResponseStatuses.OK))

  val smsDeliveryService = new ReceiptSmsDeliveryServiceImpl(
    receiptService,
    receiptSmsRenderService,
    smsClientMock
  )

  val emailDeliveryService =
    new ReceiptEmailDeliveryServiceImpl(balanceApiMock, mailerMock)

  val receiptDeliveryService =
    new ReceiptDeliveryServiceImpl(Some(emailDeliveryService), Some(smsDeliveryService))

  val receiptParameters = ReceiptParameters(
    "0",
    TaxationTypes.OSN,
    TaxTypes.NdsNone,
    TaxTypes.NdsNone,
    "test",
    "test"
  )

  val provider = new StaticReceiptParametersProvider(receiptParameters)

  val ReceiptWoEmailAndPhone = receiptGen().next.copy(email = None, phone = None)
  val ReceiptWoPhone = receiptGen().next.copy(email = Some("test"), phone = None)
  val ReceiptWoEmail = receiptGen().next.copy(email = None, phone = Some("123"))
  val ReceiptWithEmailAndPhone = receiptGen().next.copy(email = Some("test"), phone = Some("123"))

  val FailSmsClientResponses = Seq(
    SmsClientResponseStatuses.BadPhone,
    SmsClientResponseStatuses.PermanentBlock,
    SmsClientResponseStatuses.PhoneBlocked
  ).map { status =>
    SmsClientResponse(status, Some("error"))
  }

  val UnexpectedSmsClientResponses = Seq(
    SmsClientResponseStatuses.DontKnowYou,
    SmsClientResponseStatuses.NoRights,
    SmsClientResponseStatuses.NoPhone,
    SmsClientResponseStatuses.NoCurrent,
    SmsClientResponseStatuses.NoText,
    SmsClientResponseStatuses.NoUid,
    SmsClientResponseStatuses.NoRoute,
    SmsClientResponseStatuses.IntError,
    SmsClientResponseStatuses.LimitExceed
  ).map { status =>
    SmsClientResponse(status)
  }

  override def beforeEach(): Unit = {
    reset[Any](receiptSmsRenderService, receiptService, balanceApiMock, mailerMock, smsClientMock)
    mockBalanceApiRender()
    super.beforeEach()
  }

  val receiptApi = new ReceiptApiImpl(
    balanceApiMock,
    receiptDeliveryService,
    provider
  )

  "ReceiptApi" should {

    "send email when have phone and email" in {
      mockEmailSendSuccess()
      receiptApi.send(ReceiptWithEmailAndPhone).await
      verify(balanceApiMock).render(?, ?)
      verify(balanceApiMock, never()).receipt(?)
      verify(balanceApiMock, never()).commit(?)
      verify(mailerMock).send(?)
      verify(smsClientMock, never()).sendText(?, ?, ?)
      verify(receiptSmsRenderService, never()).shortUrl(?)
      verify(receiptSmsRenderService, never()).render(?, ?)
      verify(receiptService, never()).setShortUrl(?, ?)(?)
    }

    "send email when have only email" in {
      mockEmailSendSuccess()
      receiptApi.send(ReceiptWoPhone).await
      verify(balanceApiMock).render(?, ?)
      verify(balanceApiMock, never()).receipt(?)
      verify(balanceApiMock, never()).commit(?)
      verify(mailerMock).send(?)
      verify(smsClientMock, never()).sendText(?, ?, ?)
      verify(receiptSmsRenderService, never()).shortUrl(?)
      verify(receiptSmsRenderService, never()).render(?, ?)
      verify(receiptService, never()).setShortUrl(?, ?)(?)
    }

    "send sms when have only phone" in {
      mockSmsSendSuccess()
      mockShortUrl()
      mockSetShortUrl()
      mockRender()
      receiptApi.send(ReceiptWoEmail).await
      verify(balanceApiMock, never()).render(?, ?)
      verify(balanceApiMock, never()).receipt(?)
      verify(balanceApiMock, never()).commit(?)
      verify(mailerMock, never()).send(?)
      verify(smsClientMock).sendText(?, ?, ?)
      verify(receiptSmsRenderService).shortUrl(?)
      verify(receiptSmsRenderService).render(?, ?)
      verify(receiptService).setShortUrl(?, ?)(?)
    }

    "fail send without email and phone" in {
      intercept[IllegalArgumentException] {
        receiptApi.send(ReceiptWoEmailAndPhone).await
      }
      verify(balanceApiMock, never()).render(?, ?)
      verify(balanceApiMock, never()).receipt(?)
      verify(balanceApiMock, never()).commit(?)
      verify(mailerMock, never()).send(?)
      verify(smsClientMock, never()).sendText(?, ?, ?)
      verify(receiptSmsRenderService, never()).shortUrl(?)
      verify(receiptSmsRenderService, never()).render(?, ?)
      verify(receiptService, never()).setShortUrl(?, ?)(?)
    }

    "fail send when email send fail" in {
      mockEmailSendFail(new IllegalArgumentException("test"))
      intercept[SendReceiptByEmailException] {
        receiptApi.send(ReceiptWoPhone).await
      }
      verify(balanceApiMock).render(?, ?)
      verify(balanceApiMock, never()).receipt(?)
      verify(balanceApiMock, never()).commit(?)
      verify(mailerMock).send(?)
      verify(smsClientMock, never()).sendText(?, ?, ?)
      verify(receiptSmsRenderService, never()).shortUrl(?)
      verify(receiptSmsRenderService, never()).render(?, ?)
      verify(receiptService, never()).setShortUrl(?, ?)(?)
    }

    "send sms with sms sent ok status" in {
      val resp = SmsClientResponse(SmsClientResponseStatuses.OK)
      mockSmsSend(resp)
      mockShortUrl()
      mockSetShortUrl()
      mockRender()
      val response = receiptApi.send(ReceiptWoEmail).futureValue
      response.status shouldBe ReceiptSentStatuses.OK
      response.failSentDescription shouldBe None
      verify(balanceApiMock, never()).render(?, ?)
      verify(balanceApiMock, never()).receipt(?)
      verify(balanceApiMock, never()).commit(?)
      verify(mailerMock, never()).send(?)
      verify(smsClientMock).sendText(?, ?, ?)
      verify(receiptSmsRenderService).shortUrl(?)
      verify(receiptSmsRenderService).render(?, ?)
      verify(receiptService).setShortUrl(?, ?)(?)
    }

    "work correctly with existing short url" in {
      val resp = SmsClientResponse(SmsClientResponseStatuses.OK)
      mockSmsSend(resp)
      mockShortUrl()
      mockSetShortUrl()
      mockRender()
      val ReceiptWoEmailWithShortUrl = ReceiptWoEmail.copy(shortUrl = Some("test"))
      val response = receiptApi.send(ReceiptWoEmailWithShortUrl).futureValue
      response.status shouldBe ReceiptSentStatuses.OK
      response.failSentDescription shouldBe None
      verify(balanceApiMock, never()).render(?, ?)
      verify(balanceApiMock, never()).receipt(?)
      verify(balanceApiMock, never()).commit(?)
      verify(mailerMock, never()).send(?)
      verify(smsClientMock).sendText(?, ?, ?)
      verify(receiptSmsRenderService, never()).shortUrl(?)
      verify(receiptSmsRenderService).render(?, ?)
      verify(receiptService, never()).setShortUrl(?, ?)(?)
    }

    "fail send when fetch of short url fail" in {
      mockShortUrl(new IllegalArgumentException("test"))
      intercept[IllegalArgumentException] {
        receiptApi.send(ReceiptWoEmail).await
      }
      verify(balanceApiMock, never()).render(?, ?)
      verify(balanceApiMock, never()).receipt(?)
      verify(balanceApiMock, never()).commit(?)
      verify(mailerMock, never()).send(?)
      verify(smsClientMock, never()).sendText(?, ?, ?)
      verify(receiptSmsRenderService).shortUrl(?)
      verify(receiptSmsRenderService, never()).render(?, ?)
      verify(receiptService, never()).setShortUrl(?, ?)(?)
    }

    "fail send when sms send failed with exception" in {
      mockSmsSendFail(new IllegalArgumentException("test"))
      mockShortUrl()
      mockSetShortUrl()
      mockRender()
      intercept[IllegalArgumentException] {
        receiptApi.send(ReceiptWoEmail).await
      }
      verify(balanceApiMock, never()).render(?, ?)
      verify(balanceApiMock, never()).receipt(?)
      verify(balanceApiMock, never()).commit(?)
      verify(mailerMock, never()).send(?)
      verify(smsClientMock).sendText(?, ?, ?)
      verify(receiptSmsRenderService).shortUrl(?)
      verify(receiptSmsRenderService).render(?, ?)
      verify(receiptService).setShortUrl(?, ?)(?)
    }

    "fail send when sms send failed with fail status" in {
      FailSmsClientResponses.zipWithIndex.foreach { case (resp, i) =>
        mockSmsSend(resp)
        mockShortUrl()
        mockSetShortUrl()
        mockRender()
        val response = receiptApi.send(ReceiptWoEmail).futureValue
        response.status shouldBe ReceiptSentStatuses.Fail
        response.failSentDescription.get.getError shouldBe resp.status.toString
        response.failSentDescription.get.getDescription shouldBe resp.description.get
        verify(balanceApiMock, never()).render(?, ?)
        verify(balanceApiMock, never()).receipt(?)
        verify(balanceApiMock, never()).commit(?)
        verify(mailerMock, never()).send(?)
        verify(smsClientMock, times(i + 1)).sendText(?, ?, ?)
        verify(receiptSmsRenderService, times(i + 1)).shortUrl(?)
        verify(receiptSmsRenderService, times(i + 1)).render(?, ?)
        verify(receiptService, times(i + 1)).setShortUrl(?, ?)(?)
      }
    }

    "fail send when sms send failed with unexpected status" in {
      UnexpectedSmsClientResponses.zipWithIndex.foreach { case (resp, i) =>
        mockSmsSend(resp)
        mockShortUrl()
        mockSetShortUrl()
        mockRender()
        intercept[SendReceiptBySmsException] {
          receiptApi.send(ReceiptWoEmail).await
        }
        verify(balanceApiMock, never()).render(?, ?)
        verify(balanceApiMock, never()).receipt(?)
        verify(balanceApiMock, never()).commit(?)
        verify(mailerMock, never()).send(?)
        verify(smsClientMock, times(i + 1)).sendText(?, ?, ?)
        verify(smsClientMock, times(i + 1)).sendText(?, ?, ?)
        verify(receiptSmsRenderService, times(i + 1)).shortUrl(?)
        verify(receiptSmsRenderService, times(i + 1)).render(?, ?)
        verify(receiptService, times(i + 1)).setShortUrl(?, ?)(?)
      }
    }

  }

}
