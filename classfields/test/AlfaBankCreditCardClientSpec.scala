package ru.yandex.vertis.shark.client.bank

import baker.common.client.dadata.{DadataClient, DadataClientConfig}
import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.AlfaBankConverter
import ru.yandex.vertis.shark.client.bank.converter.AlfaBankConverter.Source
import ru.yandex.vertis.shark.client.bank.data.alfa.Entities.AlfaBankLeadId
import ru.yandex.vertis.shark.client.bank.data.alfa.Responses.SendClaimResponse
import ru.yandex.vertis.shark.client.bank.dictionary.alfa.{AlfaBankDictionary, StaticAlfaBankResource}
import ru.yandex.vertis.shark.config.AlfaBankClientConfig
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.generators.CreditApplicationGen
import ru.yandex.vertis.shark.model.{AutoruCreditApplication, CreditApplication, SenderConverterContext, Tag}
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.zio_baker.util.DateTimeUtil.RichInstant
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig, SslContextConfig}
import _root_.common.id.IdGenerator
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random
import zio.test.TestAspect.ignore
import zio.test.{assertTrue, DefaultRunnableSpec, ZSpec}
import zio.{Task, UIO, ZIO, ZLayer}

import java.time.Instant
import scala.concurrent.duration.DurationInt

class AlfaBankCreditCardClientSpec extends DefaultRunnableSpec with CreditApplicationGen with StaticSamples {

  private lazy val proxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128)

  private lazy val alfaBankClientConfig = AlfaBankClientConfig(
    HttpClientConfig(
      url = "https://apiws.alfabank.ru",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some,
      sslContextConfig = SslContextConfig(
        certificateBase64 = "cert_here",
        password = "password_here"
      ).some
    ),
    "IA",
    "44fec0ed-ca53-4ab5-9fbb-1535d7dce1b5"
  )

  private lazy val dadataClientConfig = DadataClientConfig(
    HttpClientConfig(
      url = "https://suggestions.dadata.ru:443",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some
    ),
    ""
  )

  private lazy val idGeneratorLayer = Clock.live >+> Random.live >+> IdGenerator.snowflake

  private lazy val bankHttpClientLayer =
    Blocking.live ++ ZLayer.succeed(alfaBankClientConfig.http) >>> HttpClient.blockingLayer

  private lazy val dadataHttpClientBackendLayer =
    Blocking.live ++ ZLayer.succeed(dadataClientConfig.http) >>> HttpClient.blockingLayer

  private lazy val dadataClientLayer =
    dadataHttpClientBackendLayer ++ ZLayer.succeed(dadataClientConfig) >>> DadataClient.live

  private lazy val alfaBankConverterLayer =
    idGeneratorLayer ++
      dadataClientLayer ++
      ZLayer.succeed[Resource[Any, AlfaBankDictionary.Service]](new StaticAlfaBankResource) >+>
      AlfaBankConverter.livePil

  private lazy val alfaBankClientLayer =
    bankHttpClientLayer ++ ZLayer.succeed(alfaBankClientConfig) >>> AlfaBankClient.live

  private lazy val env = alfaBankConverterLayer ++ alfaBankClientLayer

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("AlfaBankClient")(
      testM("send credit card claim") {
        val res = for {
          autoruCreditApplication <- Task.succeed(sampleCreditApplication)
          claimId = "claimId-test-2222".taggedWith[Tag.CreditApplicationClaimId]
          clientContext = BankClientContext(autoruCreditApplication, claimId)
          client <- ZIO.service[AlfaBankClient.Service]
          converter <- ZIO.service[AlfaBankConverter.Service]
          clock <- ZIO.service[Clock.Service]
          idGenerator <- ZIO.service[IdGenerator.Service]
          autoRuClaim = autoruCreditApplication.claims.find(_.creditProductId == "alfabank-1").get.asAuto
          leadId <- alfaBankLeadId(idGenerator, autoRuClaim)
          timestamp <- clock.instant
          converterContext = autoruConverterContext(timestamp, autoruCreditApplication)
          context = SenderConverterContext.forTest(converterContext)
          source = Source(context, leadId)
          claim <- converter.convert(source)
          res <- client.sendClaim(claim)(clientContext)
        } yield {
          println(res)
          assertTrue(res.isInstanceOf[SendClaimResponse])
        }
        res.provideLayer(env)
      }
    ) @@ ignore
  }

  private def autoruConverterContext(timestamp: Instant, autoruCreditApplication: AutoruCreditApplication) = {
    val regAddress = autoruCreditApplication.borrowerPersonProfile.flatMap(_.registrationAddress).map(_.addressEntity)
    val resAddress = autoruCreditApplication.borrowerPersonProfile.flatMap(_.residenceAddress).map(_.addressEntity)
    AutoConverterContext.forTest(
      timestamp,
      autoruCreditApplication,
      registrationAddress = regAddress,
      residenceAddress = resAddress
    )
  }

  private def alfaBankLeadId(
      idGenerator: IdGenerator.Service,
      claim: CreditApplication.AutoruClaim): UIO[AlfaBankLeadId] =
    idGenerator.nextId
      .map(_.toString.takeRight(18))
      .map(AlfaBankLeadId(_, claim.created.toLocalDateTime()))
}
