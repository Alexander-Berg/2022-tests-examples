package ru.yandex.vertis.safe_deal.client.bank

import cats.syntax.option._
import com.softwaremill.tagging._
import ru.yandex.vertis.safe_deal.client.bank.model.nominal_accounts.BeneficiaryFlResidentRequest.RussianCitizenship
import ru.yandex.vertis.safe_deal.client.bank.model.nominal_accounts.IncomingTransactionsIdentifyRequest.AmountDistributionItem
import ru.yandex.vertis.safe_deal.client.bank.model.nominal_accounts._
import ru.yandex.vertis.safe_deal.config.TinkoffClientConfig
import ru.yandex.vertis.safe_deal.model._
import ru.yandex.vertis.zio_baker.util.EmptyString
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig, SslContextConfig}
import zio.{Has, ZIO, ZLayer}
import zio.blocking.Blocking
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.test.environment._

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.duration.DurationInt

object TinkoffSecuredClientSpec extends DefaultRunnableSpec {

  private lazy val proxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128)

  private val httpClientConfig = HttpClientConfig(
    url = "https://secured-openapi.business.tinkoff.ru",
    connectionTimeout = 10.seconds,
    proxyConfig = proxyConfig.some,
    sslContextConfig = SslContextConfig(
      certificateBase64 =
        "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",
      password = "mfBcCh4nnj237gj"
    ).some
  )

  private val config = TinkoffClientConfig(
    http = httpClientConfig,
    secureHttp = httpClientConfig,
    token = "t.ANudXpV50cPPy82D5bI_VqjNNKI-r9QbiCKb1GRqQ_S29yDeU5EfRHMyGtKXulGBskwuRagm5N1F6r3DgnBUKg",
    accountNumber = EmptyString,
    dealAccountNumber = "40702810110001088085",
    bic = "044525974",
    purposeRegex = """[N№\#]\s*(БС|)\s*([\d]+)"""
  )

  private lazy val httpClientLayer =
    (Blocking.live ++ ZLayer.succeed(httpClientConfig) >>> HttpClient.blockingLayer).build.toLayerMany

  private lazy val tinkoffSecureClientLayer = ZLayer.succeed(config) ++ httpClientLayer >>> TinkoffSecureClient.live

  private lazy val env = tinkoffSecureClientLayer

  private val sellerBeneficiaryFlResidentRequest = BeneficiaryFlResidentRequest(
    firstName = "Пупа".taggedWith[Tag.FirstName],
    middleName = "Иванович".taggedWith[Tag.MiddleName].some,
    lastName = "Иванов".taggedWith[Tag.LastName],
    isSelfEmployed = false,
    birthDate = LocalDate.parse("1990-01-20"),
    birthPlace = "г.Нойруппин, Германия".taggedWith[Tag.BirthPlace],
    citizenship = RussianCitizenship,
    phoneNumber = "+79091234567".some,
    email = "ya@ya.ru".some,
    documents = Seq(
      BeneficiaryDocument(
        `type` = BeneficiaryDocument.DocumentType.Passport,
        serial = "6303".taggedWith[Tag.DocSeries].some,
        number = "524170".taggedWith[Tag.DocNumber],
        date = LocalDate.parse("2010-01-01").taggedWith[Tag.DocDate].some,
        organization = "ОВД".taggedWith[Tag.DepartName].some,
        division = "643-022".taggedWith[Tag.DepartCode].some,
        expireDate = LocalDate.parse("2030-01-01").taggedWith[Tag.DocDate].some
      )
    ),
    addresses = Seq(
      BeneficiaryAddress(
        `type` = BeneficiaryAddress.AddressType.RegistrationAddress,
        address = "127287, г. Москва, ул. 2-я Хуторская, д. 38А, стр. 26"
      )
    ),
    inn = "415047836220".taggedWith[Tag.Inn].some
  )

  private val buyerBeneficiaryFlResidentRequest = BeneficiaryFlResidentRequest(
    firstName = "Лупа".taggedWith[Tag.FirstName],
    middleName = "Иванович".taggedWith[Tag.MiddleName].some,
    lastName = "Иванов".taggedWith[Tag.LastName],
    isSelfEmployed = false,
    birthDate = LocalDate.parse("1990-01-20"),
    birthPlace = "г.Нойруппин, Германия".taggedWith[Tag.BirthPlace],
    citizenship = RussianCitizenship,
    phoneNumber = "+79091234567".some,
    email = "ya@ya.ru".some,
    documents = Seq(
      BeneficiaryDocument(
        `type` = BeneficiaryDocument.DocumentType.Passport,
        serial = "6303".taggedWith[Tag.DocSeries].some,
        number = "524170".taggedWith[Tag.DocNumber],
        date = LocalDate.parse("2010-01-01").taggedWith[Tag.DocDate].some,
        organization = "ОВД".taggedWith[Tag.DepartName].some,
        division = "643-022".taggedWith[Tag.DepartCode].some,
        expireDate = LocalDate.parse("2030-01-01").taggedWith[Tag.DocDate].some
      )
    ),
    addresses = Seq(
      BeneficiaryAddress(
        `type` = BeneficiaryAddress.AddressType.RegistrationAddress,
        address = "127287, г. Москва, ул. 2-я Хуторская, д. 38А, стр. 26"
      )
    ),
    inn = "725088144706".taggedWith[Tag.Inn].some
  )

  private val sellerRkcBankDetailsRequest = RkcBankDetailsRequest(
    isDefault = true.some,
    bik = "044525974".taggedWith[Tag.Bic],
    kpp = "773401001".taggedWith[Tag.Kpp].some,
    inn = "906858195320".taggedWith[Tag.Inn].some,
    name = "Иванов Иван Иванович".taggedWith[Tag.Name].some,
    bankName = "АО \"Тинькофф Банк\"".taggedWith[Tag.BankName],
    accountNumber = "11223344556677889900".taggedWith[Tag.AccountNumber],
    corrAccountNumber = "30101810145250000974".taggedWith[Tag.CorrAccountNumber]
  )

  private val amount = BigDecimal(10).taggedWith[Tag.Amount]

  override def spec: ZSpec[TestEnvironment, Any] = {
    (suite("TinkoffSecuredClient")(
      testM("Create beneficiaries & bank details & deal") {
        val actual = for {
          client <- ZIO.service[TinkoffSecureClient.Service]
          sellerBeneficiaryId <- client
            .nominalAccountsBeneficiaryCreate(sellerBeneficiaryFlResidentRequest, genIdempotencyKey())
            .map(_.beneficiaryId)
          _ = println(s"sellerBeneficiaryId: $sellerBeneficiaryId")
          sellerBankDetailsId <- client
            .nominalAccountsBeneficiariesBankDetailsCreate(
              sellerBeneficiaryId,
              sellerRkcBankDetailsRequest,
              genIdempotencyKey()
            )
            .map(_.bankDetailsId)
          _ = println(s"sellerBankDetailsId: $sellerBankDetailsId")
          buyerBeneficiaryId <- client
            .nominalAccountsBeneficiaryCreate(buyerBeneficiaryFlResidentRequest, genIdempotencyKey())
            .map(_.beneficiaryId)
          _ = println(s"buyerBeneficiaryId: $buyerBeneficiaryId")
          bankDealCreateResponse <- client.nominalAccountsDealCreate(
            BankDealCreateRequest(config.dealAccountNumber.taggedWith[Tag.AccountNumber]),
            genIdempotencyKey()
          )
          _ = println(s"bankDealCreateResponse: $bankDealCreateResponse")
          dealId = bankDealCreateResponse.dealId
          bankDealStepCreateResponse <- client
            .nominalAccountsDealStepCreate(dealId, BankDealStepCreateRequest("Some description"), genIdempotencyKey())
          _ = println(s"bankDealStepCreateResponse: $bankDealStepCreateResponse")
          stepId = bankDealStepCreateResponse.stepId
          _ <- client.nominalAccountsDealStepAddDeponents(
            dealId,
            stepId,
            buyerBeneficiaryId,
            BankDealStepAddDeponentsRequest(amount)
          )
          bankDealStepAddRecipientsResponse <- client.nominalAccountsDealStepAddRecipients(
            dealId,
            stepId,
            BankDealStepAddRecipientsRequest(
              beneficiaryId = sellerBeneficiaryId,
              amount = amount,
              purpose = "Назначение платежа".taggedWith[Tag.PaymentPurpose],
              bankDetailsId = sellerBankDetailsId,
              keepOnVirtualAccount = false
            ),
            genIdempotencyKey()
          )
          _ = println(s"bankDealStepAddRecipientsResponse: $bankDealStepAddRecipientsResponse")
        } yield ()
        assertM(actual)(isUnit)
      } @@ ignore,
      testM("Check scoring") {
        val sellerBeneficiaryId = ???
        val actual = for {
          client <- ZIO.service[TinkoffSecureClient.Service]
          beneficiariesScoringResponse <- client
            .nominalAccountsBeneficiariesScoring(sellerBeneficiaryId, passed = true.some, 0.some, 50.some)
          _ = println(s"beneficiariesScoringResponse: $beneficiariesScoringResponse")
        } yield ()
        assertM(actual)(isUnit)
      } @@ ignore,
      testM("Check payment") {
        val actual = for {
          client <- ZIO.service[TinkoffSecureClient.Service]
          incomingTransactionsResponse <- client.nominalAccountsIncomingTransactions(
            accountNumber = config.accountNumber.taggedWith[Tag.AccountNumber],
            offset = None,
            limit = None
          )
          _ = println(s"incomingTransactionsResponse: $incomingTransactionsResponse")
        } yield ()
        assertM(actual)(isUnit)
      } @@ ignore,
      testM("Check payment") {
        val operationId = ???
        val buyerBeneficiaryId = ???
        val sellerBeneficiaryId = ???
        val dealId = ???
        val stepId = ???
        val actual = for {
          client <- ZIO.service[TinkoffSecureClient.Service]
          _ <- client.nominalAccountsIncomingTransactionsIdentify(
            operationId,
            IncomingTransactionsIdentifyRequest(Seq(AmountDistributionItem(buyerBeneficiaryId, amount)))
          )
          _ <- client.nominalAccountsDealAccept(dealId)
          _ <- client.nominalAccountsDealStepComplete(dealId, stepId)
          paymentsResponse <- client.nominalAccountsPayments(
            sellerBeneficiaryId,
            dealId,
            config.accountNumber.taggedWith[Tag.AccountNumber],
            None,
            None
          )
          _ = println(s"paymentsResponse: $paymentsResponse")
        } yield ()
        assertM(actual)(isUnit)
      } @@ ignore,
      testM("Return money") {
        val buyerBeneficiaryId = ???
        val buyerRkcBankDetailsRequest = RkcBankDetailsRequest(
          isDefault = true.some,
          bik = "044525974".taggedWith[Tag.Bic],
          kpp = "773401001".taggedWith[Tag.Kpp].some,
          inn = "906858195320".taggedWith[Tag.Inn].some,
          name = "Иванов Иван Иванович".taggedWith[Tag.Name].some,
          bankName = "АО \"Тинькофф Банк\"".taggedWith[Tag.BankName],
          accountNumber = "11223344556677889900".taggedWith[Tag.AccountNumber],
          corrAccountNumber = "30101810145250000974".taggedWith[Tag.CorrAccountNumber]
        )
        val actual = for {
          client <- ZIO.service[TinkoffSecureClient.Service]
          buyerBankDetailsId <- client
            .nominalAccountsBeneficiariesBankDetailsCreate(
              buyerBeneficiaryId,
              buyerRkcBankDetailsRequest,
              genIdempotencyKey()
            )
            .map(_.bankDetailsId)
          _ = println(s"buyerBankDetailsId: $buyerBankDetailsId")
          regularPaymentRequest = RegularPaymentRequest(
            beneficiaryId = buyerBeneficiaryId,
            accountNumber = config.dealAccountNumber.taggedWith[Tag.AccountNumber],
            bankDetailsId = buyerBankDetailsId,
            amount = amount,
            purpose = "Возврат".taggedWith[Tag.PaymentPurpose]
          )
          _ <- client.nominalAccountsPaymentCreate(regularPaymentRequest, genIdempotencyKey())
        } yield ()
        assertM(actual)(isUnit)
      } @@ ignore
    ) @@ sequential)
      .provideCustomLayerShared(env.orDie)
  }

  private def genIdempotencyKey(): String = UUID.randomUUID.toString
}
