package ru.yandex.vertis.shark.client.bank

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.auto.api.api_offer_model.{Documents, Offer, State}
import ru.auto.api.cars_model.CarInfo
import ru.auto.api.common_model.PriceInfo
import ru.yandex.vertis.shark.client.bank.converter.TinkoffBankConverter.Source
import ru.yandex.vertis.shark.client.bank.converter.impl._
import ru.yandex.vertis.shark.config.TinkoffBankClientConfig
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.model.{CreditApplication, SenderConverterContext, Tag}
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig}
import zio.{ZIO, ZLayer}
import zio.blocking.Blocking
import zio.test.Assertion._
import zio.test.TestAspect.ignore
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant
import scala.concurrent.duration._

class TinkoffBankClientSpec extends DefaultRunnableSpec with CreditApplicationGen with AutoruOfferGen {

  private lazy val proxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128)

  private lazy val config = TinkoffBankClientConfig(
    HttpClientConfig(
      url = "https://api.tinkoff.ru:443",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some
    )
  )

  private lazy val generalConverter = new TinkoffBankAutoCreditConverter
  private lazy val targetConverter = new TinkoffBankCarConverterImpl

  private lazy val bankHttpClientLayer =
    Blocking.live ++ ZLayer.succeed(config.http) >>> HttpClient.blockingLayer

  private lazy val tinkoffBankClientLayer =
    bankHttpClientLayer ++ ZLayer.succeed(config) >>> TinkoffBankClient.live

  private lazy val env = tinkoffBankClientLayer

  /**
    * This claim id was received from tinkoff bank for tests and may change in future.
    * If test fails ask them for new one.
    */
  private val autoruCreditApplication = sampleAutoruCreditApplication()
  private val claimId = "f961c29971ddcc5f4e2e998f07d67b76".taggedWith[Tag.CreditApplicationClaimId]
  private val bankClaimId = "f961c29971ddcc5f4e2e998f07d67b76".taggedWith[Tag.CreditApplicationBankClaimId]
  private val someTs = Instant.now
  private val clientContext = BankClientContext(autoruCreditApplication, claimId)

  def spec: ZSpec[TestEnvironment, Any] =
    suite("TinkoffBankClient")(
      testM("sendClaim") {
        val claim = CreditApplication.AutoruClaim.forTest(
          id = claimId,
          bankClaimId = None,
          created = someTs,
          updated = someTs,
          processAfter = None,
          creditProductId = "tinkoff-1".taggedWith[Tag.CreditProductId],
          state = proto.CreditApplication.Claim.ClaimState.DRAFT,
          bankState = None,
          approvedMaxAmount = None,
          approvedTermMonths = None,
          approvedInterestRate = None,
          approvedMinInitialFeeRate = None,
          offerEntities = Seq.empty
        )
        val application = autoruCreditApplication.copy(claims = Seq(claim))
        val timestamp = Instant.now()
        val converterContext =
          AutoConverterContext.forTest(timestamp = timestamp, creditApplication = application)
        val context = SenderConverterContext.forTest(converterContext)
        val source = Source(context, claimId)
        val res = for {
          client <- ZIO.service[TinkoffBankClient.Service]
          fields <- generalConverter.convert(source)
          r <- client.sendClaim(fields)(clientContext)
        } yield r
        assertM(res)(isNonEmptyString).provideLayer(env)
      },
      testM("sendClaimObject") {
        val doc = Documents.defaultInstance
          .withLicensePlate("А111МР")
          .withVin("JN1AZ34D26M354602")
          .withYear(2018)
        val car = CarInfo.defaultInstance
          .withTransmission("PP")
          .withMark("AMG")
          .withModel("GL500")
          .withEngineType("GASOLINE")
        val price = PriceInfo.defaultInstance.withRurPrice(10000000f)
        val state = State.defaultInstance.withMileage(100500)
        val offer = Offer.defaultInstance
          .withId("1100333558-d78d7fb9")
          .withColorHex("000000")
          .withDocuments(doc)
          .withCarInfo(car)
          .withPriceInfo(price)
          .withState(state)

        val res = for {
          client <- ZIO.service[TinkoffBankClient.Service]
          fields <- targetConverter.convert(offer)
          res <- client.sendClaimObject(bankClaimId, fields)(clientContext)
        } yield res

        assertM(res)(isUnit).provideLayer(env)
      }
    ) @@ ignore
}
