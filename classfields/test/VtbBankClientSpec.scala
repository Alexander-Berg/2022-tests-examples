package ru.yandex.vertis.shark.client.bank

import cats.implicits._
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.client.bank.converter.VtbConverter.Source
import ru.yandex.vertis.shark.client.bank.converter.{VtbFullAppConverter, VtbMiniAppConverter}
import ru.yandex.vertis.shark.client.bank.dictionary.vtb.{StaticVtbResource, VtbDictionary}
import ru.yandex.vertis.shark.config.VtbClientConfig
import ru.yandex.vertis.shark.dictionary.RegionsDictionary
import ru.yandex.vertis.shark.model.ConverterContext.AutoConverterContext
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.CreditApplicationGen
import ru.yandex.vertis.shark.model.{SenderConverterContext, Tag}
import ru.yandex.vertis.shark.util.GeobaseUtils
import ru.yandex.vertis.shark.{RegionsDictionaryLayers, StaticSamples}
import ru.yandex.vertis.zio_baker.zio.client.vos.{VosAutoruClient, VosAutoruClientConfig}
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig, SslContextConfig}
import _root_.common.id.IdGenerator
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion._
import zio.test.TestAspect.ignore
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Runtime, Task, ZIO, ZLayer}

import scala.concurrent.duration.DurationInt

object VtbBankClientSpec
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

  private lazy val idGeneratorLayer = Clock.live >+> Random.live >+> IdGenerator.snowflake

  private lazy val config = VtbClientConfig(
    HttpClientConfig(
      url = "https://extcrmti.vtb24.ru:7443",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some,
      sslContextConfig = SslContextConfig("cert", "password").some
    ),
    productName = "А_Авто.ру"
  )

  private lazy val bankHttpClientLayer =
    Blocking.live ++ ZLayer.succeed(config.http) >>> HttpClient.blockingLayer

  private lazy val miniAppConverterLayer =
    regionsDictionaryLayer ++
      idGeneratorLayer ++
      ZLayer.succeed[Resource[Any, VtbDictionary.Service]](new StaticVtbResource) ++
      ZLayer.succeed(config) >>>
      VtbMiniAppConverter.live

  private lazy val fullAppConverterLayer =
    regionsDictionaryLayer ++
      ZLayer.succeed[Resource[Any, VtbDictionary.Service]](new StaticVtbResource) ++
      ZLayer.succeed(config) >>>
      VtbFullAppConverter.live

  private lazy val vtbBankClientLayer =
    bankHttpClientLayer ++ ZLayer.succeed(config) >>> VtbBankClient.live

  private lazy val bankClientLayer = {
    ZLayer.requires[Clock] ++
      vosAutoruClientLayer ++ miniAppConverterLayer ++ fullAppConverterLayer ++
      regionsDictionaryLayer ++ vtbBankClientLayer
  }

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("VtbBankClient")(
      testM("mini app") {
        val res = for {
          converter <- ZIO.service[VtbMiniAppConverter.Service]
          regionsDictionary <- ZIO.service[RegionsDictionary.Service]
          clock <- ZIO.service[Clock.Service]
          vosClient <- ZIO.service[VosAutoruClient.Service]
          client <- ZIO.service[VtbBankClient.Service]
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
          source = Source(
            context = context,
            claimId = creditApplication.claims.head.id,
            None,
            None,
            None,
            None
          )
          miniApp <- converter.convert(source)
          bankClientContext = BankClientContext(creditApplication, source.claimId)
          _ <- client.sendMiniApp(miniApp)(bankClientContext)
        } yield ()
        assertM(res)(isUnit).provideLayer(bankClientLayer)
      } @@ ignore,
      testM("full app") {
        val res = for {
          converter <- ZIO.service[VtbFullAppConverter.Service]
          regionsDictionary <- ZIO.service[RegionsDictionary.Service]
          clock <- ZIO.service[Clock.Service]
          vosClient <- ZIO.service[VosAutoruClient.Service]
          client <- ZIO.service[VtbBankClient.Service]
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
          source = Source(
            context = context,
            claimId = creditApplication.claims.head.id,
            miniAppNumber = "123".some,
            vtbClientId = "123".some,
            bankClaimId = "123".taggedWith[Tag.CreditApplicationBankClaimId].some,
            vtbApplicationId = None
          )
          fullApp <- converter.convert(source)
          bankClientContext = BankClientContext(creditApplication, source.claimId)
          _ <- client.sendFullApp(fullApp)(bankClientContext)
        } yield ()
        assertM(res)(isUnit).provideLayer(bankClientLayer)
      } @@ ignore
    )
  }
}
