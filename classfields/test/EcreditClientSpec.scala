package ru.yandex.vertis.shark.client.bank

import baker.common.client.dadata.model.DadataOrganization
import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import io.circe.syntax.EncoderOps
import ru.auto.api.api_offer_model.Offer
import ru.yandex.vertis.shark.StaticSamples
import ru.yandex.vertis.shark.client.bank.converter.impl.ecredit.DefaultValues
import ru.yandex.vertis.shark.client.bank.converter.{EcreditEditAppConverter, EcreditNewAppConverter}
import ru.yandex.vertis.shark.client.bank.data.ecredit.Entities.AppStatusRequest
import ru.yandex.vertis.shark.client.bank.data.ecredit.Responses
import ru.yandex.vertis.shark.client.bank.dictionary.ecredit.{EcreditMarksDictionary, StaticEcreditMarksResource}
import ru.yandex.vertis.shark.config.EcreditClientConfig
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.model.{AutoruCreditApplication, SenderConverterContext}
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.proto.model.Block.GenderBlock.GenderType
import ru.yandex.vertis.shark.util.RichModel.RichCreditApplication
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig}
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.TestAspect.ignore
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.test.{assertTrue, ZSpec}
import zio.{ZIO, ZLayer}

import java.time.Instant
import scala.concurrent.duration.DurationInt

object EcreditClientSpec extends DefaultRunnableSpec with CreditApplicationGen with AutoruOfferGen with StaticSamples {

  private lazy val ecreditDealerId = "16453"
  private lazy val autoruCreditApplication = sampleCreditApplication
  private lazy val claim = autoruCreditApplication.getClaimByCreditProductId("dealer-1".taggedWith).get

  private lazy val proxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128)

  private lazy val apiConfig = EcreditClientConfig(
    HttpClientConfig(
      url = "https://test-api-online.ecredit.one",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some
    ),
    integratorUid = "<укажи меня>",
    token = "<укажи меня>"
  )

  private lazy val bankHttpClientLayer =
    Blocking.live ++ ZLayer.succeed(apiConfig.http) >>> HttpClient.blockingLayer

  private lazy val newAppConverterLayer = {
    val ecreditMarksDictionaryLayer =
      ZLayer.succeed[Resource[Any, EcreditMarksDictionary.Service]](new StaticEcreditMarksResource) >>>
        EcreditMarksDictionary.live
    ecreditMarksDictionaryLayer >>> EcreditNewAppConverter.live
  }

  private lazy val editAppConverterLayer = EcreditEditAppConverter.live

  private lazy val apiClientLayer =
    bankHttpClientLayer ++ ZLayer.succeed(apiConfig) >>> EcreditClient.live

  private lazy val env =
    ZLayer.requires[Blocking] ++ ZLayer.requires[Clock] ++
      newAppConverterLayer ++ editAppConverterLayer ++ apiClientLayer

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("EcreditClient")(
      testM("send new app claim") {
        val res = for {
          client <- ZIO.service[EcreditClient.Service]
          converter <- ZIO.service[EcreditNewAppConverter.Service]
          clock <- ZIO.service[Clock.Service]
          timestamp <- clock.instant
          claimId = claim.id
          converterContext = autoruConverterContext(timestamp, autoruCreditApplication)
          context = SenderConverterContext.forTest(converterContext)
          source = EcreditNewAppConverter.Source(context, claimId, ecreditDealerId)
          clientContext = BankClientContext(autoruCreditApplication, claimId)
          apiClaim <- converter.convert(source)
          res <- client.sendNewAppClaim(apiClaim)(clientContext)
        } yield {
          println(res)
          assertTrue(res.isInstanceOf[Responses.SendNewAppClaim])
        }
        res.provideLayer(env)
      } @@ ignore,
      testM("send edit app claim") {
        val res = for {
          client <- ZIO.service[EcreditClient.Service]
          converter <- ZIO.service[EcreditEditAppConverter.Service]
          clock <- ZIO.service[Clock.Service]
          timestamp <- clock.instant
          claimId = claim.id
          converterContext = autoruConverterContext(timestamp, autoruCreditApplication)
          context = SenderConverterContext.forTest(converterContext)
          source = EcreditEditAppConverter.Source(context, claimId, ecreditDealerId)
          clientContext = BankClientContext(autoruCreditApplication, claimId)
          apiClaim <- converter.convert(source)
          _ = println(apiClaim.asJson.spaces2)
          res <- client.sendEditAppClaim(apiClaim)(clientContext)
        } yield {
          println(res)
          assertTrue(res.isInstanceOf[Responses.SendEditAppClaim])
        }
        res.provideLayer(env)
      } @@ ignore,
      testM("get status") {
        val res = for {
          client <- ZIO.service[EcreditClient.Service]
          claimId = claim.id
          request = AppStatusRequest(claimId, DefaultValues.IdType)
          clientContext = BankClientContext(autoruCreditApplication, claimId)
          res <- client.getStatus(request)(clientContext)
        } yield {
          println(res)
          assertTrue(res.isInstanceOf[Responses.GetStatus])
        }
        res.provideLayer(env)
      } @@ ignore
    )

  private def autoruConverterContext(timestamp: Instant, autoruCreditApplication: AutoruCreditApplication) = {
    val vosOffer: Option[Offer] = sampleOffer().some
    val organization: Option[DadataOrganization] = sampleDadataOrganization.suggestions.headOption
    val gender: GenderType = GenderType.MALE
    val regAddress = autoruCreditApplication.borrowerPersonProfile.flatMap(_.registrationAddress).map(_.addressEntity)
    val resAddress = autoruCreditApplication.borrowerPersonProfile.flatMap(_.residenceAddress).map(_.addressEntity)
    val regDadataAddress = sampleDadataAddress1.suggestions.headOption
    val resDadataAddress = sampleDadataAddress2.suggestions.headOption
    AutoConverterContext.forTest(
      timestamp = timestamp,
      creditApplication = autoruCreditApplication,
      vosOffer = vosOffer,
      organization = organization,
      gender = gender,
      registrationAddress = regAddress,
      residenceAddress = resAddress,
      registrationDadataAddress = regDadataAddress,
      residenceDadataAddress = resDadataAddress
    )
  }
}
