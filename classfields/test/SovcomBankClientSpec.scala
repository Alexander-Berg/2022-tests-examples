package ru.yandex.vertis.shark.client.bank

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.SovcomBankConverter
import ru.yandex.vertis.shark.client.bank.converter.SovcomBankConverter.Source
import ru.yandex.vertis.shark.client.bank.data.sovcom.Responses.{SuccessResponse => SovcomSuccessResponse}
import ru.yandex.vertis.shark.config.SovcomBankClientConfig
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.generators.CreditApplicationGen
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig}
import zio.{ZIO, ZLayer}
import zio.blocking.Blocking
import zio.test.TestAspect.ignore
import zio.test.environment.TestEnvironment
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}

import java.time.Instant
import scala.concurrent.duration._

object SovcomBankClientSpec extends DefaultRunnableSpec with CreditApplicationGen with StaticSamples {

  private lazy val proxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128)

  private lazy val sovcomBankClientConfig = SovcomBankClientConfig(
    http = HttpClientConfig(
      url = "https://api-app2.sovcombank.ru",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some
    ),
    token = "введи токен"
  )

  private lazy val bankHttpClientLayer =
    Blocking.live ++ ZLayer.succeed(sovcomBankClientConfig.http) >>> HttpClient.blockingLayer

  private lazy val sovcomBankClientLayer =
    bankHttpClientLayer ++ ZLayer.succeed(sovcomBankClientConfig) >>> SovcomBankClient.live

  private val converterLayer = MockDadataClient.live >>> SovcomBankConverter.live

  private val env = converterLayer ++ sovcomBankClientLayer

  private val autoruCreditApplication = sampleAutoruCreditApplication()
  private val claimId = "f961c29971ddcc5f4e2e998f07d67b76".taggedWith[Tag.CreditApplicationClaimId]
  private val clientContext = BankClientContext(autoruCreditApplication, claimId)

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("SovcomBankConverter")(
      testM("client") {
        val timestamp = Instant.now()

        val res = for {
          converter <- ZIO.service[SovcomBankConverter.Service]
          client <- ZIO.service[SovcomBankClient.Service]
          converterContext = AutoConverterContext.forTest(timestamp, autoruCreditApplication)
          context = SenderConverterContext.forTest(converterContext)
          source = Source(context)
          converted <- converter.convert(source)
          createResponse <- client.sendClaim(converted)(clientContext)
          statusResponse <- client.getStatus(createResponse.requestId.taggedWith)(clientContext)
        } yield {
          assertTrue(
            createResponse.isInstanceOf[SovcomSuccessResponse],
            statusResponse.isInstanceOf[SovcomSuccessResponse]
          )
        }
        res.provideLayer(env)
      } @@ ignore
    )
}
