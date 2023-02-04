package ru.yandex.vertis.shark.client.bank

import baker.common.client.dadata.model._
import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import common.zio.logging.Logging
import io.circe.syntax.EncoderOps
import ru.auto.api.api_offer_model.Offer
import ru.yandex.vertis.shark.client.bank.converter.PsbConverter
import ru.yandex.vertis.shark.config.PsbClientConfig
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.model.{Tag, _}
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.proto.model.Block.GenderBlock.GenderType
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig, SslContextConfig}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.ignore
import zio.test._
import zio.test.environment.TestEnvironment
import zio._

import java.time.Instant
import scala.concurrent.duration.DurationInt

object PsbClientSpec
  extends DefaultRunnableSpec
  with CreditApplicationGen
  with AutoruOfferGen
  with StaticSamples
  with Logging {

  implicit val runtime: Runtime[zio.ZEnv] = Runtime.default

  private val proxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128)

  private val sslContextConfig = SslContextConfig("/etc/yandex/vertis-datasources-secrets/psb-cert.p12", "напиши меня")

  private val httpClientConfig =
    HttpClientConfig(
      url = "https://retail-tst.payment.ru:443",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some,
      sslContextConfig = sslContextConfig.some
    )

  private val psbClientConfig =
    PsbClientConfig(httpClientConfig, "напиши меня", "напиши меня")

  private lazy val httpClientBackendLayer =
    Clock.live >+> Blocking.live >+> ZLayer.succeed(psbClientConfig.http) >+> HttpClient.blockingLayer

  private lazy val converterLayer =
    ZLayer.requires[Blocking] ++ ZLayer.requires[Clock] ++ ZLayer.succeed(psbClientConfig) >>> PsbConverter.live

  private lazy val bankClientLayer =
    converterLayer ++ (httpClientBackendLayer ++ ZLayer.succeed(psbClientConfig) >>> PsbClient.live)

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("PsbClientSpec")(
      testM("sendClaim") {
        val claims = ZIO.collectAll {
          Seq.fill(100) {
            val timestamp = Instant.now()
            val creditApplication: CreditApplication = sampleCreditApplication
            val vosOffer: Option[Offer] = sampleOffer().some
            val organization: Option[DadataOrganization] = sampleDadataOrganization.suggestions.headOption
            val gender: GenderType = GenderType.MALE
            val converterContext = SenderConverterContext.forTest(
              AutoConverterContext.forTest(
                timestamp = timestamp,
                creditApplication = creditApplication,
                vosOffer = vosOffer,
                organization = organization,
                gender = gender
              )
            )
            val claimId = "psb-claim-test".taggedWith[Tag.CreditApplicationClaimId]
            val clientContext = BankClientContext(creditApplication, claimId)
            val source = PsbConverter.Source(converterContext)
            for {
              converter <- ZIO.service[PsbConverter.Service]
              client <- ZIO.service[PsbClient.Service]
              bankClaim <- converter.convert(source)
              token <- client.signIn()(clientContext).map(_.accessToken)
              response <- client.sendClaim(bankClaim)(token, clientContext)
              _ <- client.state(response.applicationId)(token, clientContext)
            } yield response.applicationId -> bankClaim.asJson.noSpaces
          }
        }
        val res = for {
          claimSeq <- claims
          _ <- log.info {
            claimSeq
              .map { case (applicationId, bankClaim) =>
                String.join("\n", applicationId, bankClaim)
              }
              .mkString("\n")
          }
        } yield ()
        assertM(res)(isUnit).provideLayer(bankClientLayer)
      } @@ ignore
    )
  }
}
