package ru.yandex.vertis.shark.client.bank

import cats.implicits._
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.client.bank.converter.SravniRuConverter
import ru.yandex.vertis.shark.client.bank.converter.SravniRuConverter.Source
import ru.yandex.vertis.shark.config.SravniRuClientConfig
import ru.yandex.vertis.shark.dictionary.RegionsDictionary
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.CreditApplicationGen
import ru.yandex.vertis.shark.model.{SenderConverterContext, Tag}
import ru.yandex.vertis.shark.util.GeobaseUtils
import ru.yandex.vertis.shark.{RegionsDictionaryLayers, StaticSamples}
import ru.yandex.vertis.zio_baker.zio.client.vos.{VosAutoruClient, VosAutoruClientConfig}
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.ignore
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Runtime, Task, ZIO, ZLayer}

import scala.concurrent.duration.DurationInt

class SravniRuClientSpec
  extends DefaultRunnableSpec
  with CreditApplicationGen
  with StaticSamples
  with RegionsDictionaryLayers {

  implicit val runtime: Runtime[zio.ZEnv] = Runtime.default

  private val vosAutoruClientConfig = VosAutoruClientConfig(
    HttpClientConfig(url = "http://vos2-autoru-api.vrts-slb.test.vertis.yandex.net:80")
  )

  private val vosAutoruClientLayer =
    ZLayer.requires[Blocking] ++
      ZLayer.succeed(vosAutoruClientConfig.http) >>>
      HttpClient.blockingLayer ++
      ZLayer.succeed(vosAutoruClientConfig) >>>
      VosAutoruClient.live

  private lazy val proxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128)

  private lazy val config = SravniRuClientConfig(
    HttpClientConfig(
      url = "http://public.partner.qa.sravni-team.ru",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some
    ),
    partnerId = "введи меня"
  )

  private lazy val bankHttpClientLayer =
    Blocking.live ++ ZLayer.succeed(config.http) >>> HttpClient.blockingLayer

  private lazy val sravniRuConverterLayer = SravniRuConverter.live

  private lazy val sravniRuClientLayer =
    bankHttpClientLayer ++ ZLayer.succeed(config) >>> SravniRuClient.live

  private lazy val bankClientLayer = {
    ZLayer.requires[Clock] ++
      vosAutoruClientLayer ++ sravniRuConverterLayer ++ regionsDictionaryLayer ++ sravniRuClientLayer
  }

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("SravniRuClient")(
      testM("send") {
        val res = for {
          converter <- ZIO.service[SravniRuConverter.Service]
          regionsDictionary <- ZIO.service[RegionsDictionary.Service]
          clock <- ZIO.service[Clock.Service]
          vosClient <- ZIO.service[VosAutoruClient.Service]
          client <- ZIO.service[SravniRuClient.Service]
          currentDateTime <- clock.instant
          creditApplication <- Task.succeed(sampleCreditApplication)
          geobaseIds = GeobaseUtils.identityOrDefault {
            creditApplication.requirements.map(_.geobaseIds).orEmpty
          }
          parentRegions <- regionsDictionary.getParentRegions(geobaseIds)
          offer = creditApplication.offers.head
          vosOffer <- vosClient.offer(offer.category, offer.id, includeRemoved = false)
          converterContext = AutoConverterContext
            .forTest(
              timestamp = currentDateTime,
              creditApplication = creditApplication,
              vosOffer = vosOffer.some,
              parentRegions = parentRegions
            )
          context = SenderConverterContext.forTest(converterContext)
          source = Source(context = context)
          claimId = "ssravniru-claim-test-001".taggedWith[Tag.CreditApplicationClaimId]
          bankClaim <- converter.convert(source)
          bankClientContext = BankClientContext(creditApplication, claimId)
          response <- client.sendClaim(bankClaim)(bankClientContext)
        } yield {
          println(response)
        }
        assertM(res)(isUnit).provideLayer(bankClientLayer)
      } @@ ignore
    )
  }
}
