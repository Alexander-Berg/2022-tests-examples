package ru.yandex.vertis.shark.scheduler.stage.credit_application

import baker.common.client.dadata.{DadataClient, DadataClientConfig}
import com.softwaremill.tagging.Tagger
import org.scalacheck.magnolia.gen
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.palma.services.proto_dictionary_service.ProtoDictionaryServiceGrpc.{
  ProtoDictionaryService,
  ProtoDictionaryServiceStub
}
import ru.yandex.vertis.shark.client.bank._
import ru.yandex.vertis.shark.client.bank.converter._
import ru.yandex.vertis.shark.client.bank.converter.impl._
import ru.yandex.vertis.shark.client.bank.dictionary.alfa.{AlfaBankDictionary, StaticAlfaBankResource}
import ru.yandex.vertis.shark.client.bank.dictionary.ecredit.{EcreditMarksDictionary, StaticEcreditMarksResource}
import ru.yandex.vertis.shark.client.bank.dictionary.gazprom.{GazpromBankDictionary, StaticGazpromBankResource}
import ru.yandex.vertis.shark.client.bank.dictionary.raiffeizen.{RaiffeisenBankDictionary, StaticRaiffeisenBankResource}
import ru.yandex.vertis.shark.client.bank.dictionary.sber.{SberBankDictionary, StaticSberBankResource}
import ru.yandex.vertis.shark.client.bank.dictionary.vtb.{StaticVtbResource, VtbDictionary}
import ru.yandex.vertis.shark.client.dealerapplication.DealerApplicationClient
import ru.yandex.vertis.shark.config._
import ru.yandex.vertis.shark.dictionary.{CreditProductDictionary, DealerConfigurationDictionary, RegionsDictionary}
import ru.yandex.vertis.shark.model.CreditApplication.AutoruClaim
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.shark.scheduler.stage.Stage
import ru.yandex.vertis.shark.sender.CreditApplicationBankSenderProvider
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.zio_baker.util.EmptyString
import ru.yandex.vertis.zio_baker.zio.client.geocoder.GeocoderClient
import ru.yandex.vertis.zio_baker.zio.client.vos.{VosAutoruClient, VosAutoruClientConfig}
import ru.yandex.vertis.zio_baker.zio.features.DurationRange
import ru.yandex.vertis.zio_baker.zio.grpc.client.GrpcClient
import ru.yandex.vertis.zio_baker.zio.grpc.client.config.GrpcClientConfig
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import _root_.common.id.IdGenerator
import ru.yandex.vertis.shark.client.bank.converter.TinkoffBankConverter.{TinkoffBankCarConverter, TinkoffBankConverter}
import ru.yandex.vertis.zio_baker.zio.palma.PalmaClient
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion.{anything, isSome}
import zio.test.TestAspect.ignore
import zio.test.environment.{TestClock, TestEnvironment}
import zio.test.mock.{mockable, Expectation}
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.{Runtime, UIO, ULayer, ZIO, ZLayer}
import zio.blocking.Blocking

import java.net.URL
import java.time.OffsetDateTime
import scala.concurrent.duration._

object DecisionFromBankStageSpec extends DefaultRunnableSpec with CreditApplicationGen with AutoruOfferGen {

  @mockable[RegionsDictionary.Service]
  object RegionsDictionaryMock

  @mockable[GeocoderClient.Service]
  object GeocoderClientMock

  @mockable[DealerConfigurationDictionary.Service]
  object DealerConfigurationDictionaryMock

  private val regionsDictionaryMock = RegionsDictionaryMock.IsInside(
    anything,
    Expectation.value(true)
  )

  private val tinkoffBankClientConfig = TinkoffBankClientConfig(
    HttpClientConfig(url = "https://api.tinkoff.ru:443")
  )

  private val tinkoffBankCardCreditReportsClientConfig = TinkoffBankCardCreditReportsClientConfig(
    HttpClientConfig(url = "https://offer.datamind.ru"),
    EmptyString
  )

  private val raiffeisenBankClientConfig = RaiffeisenBankClientConfig(
    HttpClientConfig(url = "https://api.raiffeisen.ru")
  )

  private val gazpromBankClientConfig = GazpromBankClientConfig(
    HttpClientConfig(url = "https://testapi.gazprombank.ru/gpbapi/test"),
    EmptyString,
    EmptyString
  )

  private val rosgosstrahBankClientConfig = RosgosstrahBankClientConfig(
    HttpClientConfig(url = "https://testapi.gazprombank.ru/gpbapi/test"),
    AuthConfig(EmptyString, EmptyString)
  )

  private val sovcomBankClientConfig = SovcomBankClientConfig(
    HttpClientConfig(url = "https://api-app2.sovcombank.ru"),
    EmptyString
  )

  private val alfaBankClientConfig = AlfaBankClientConfig(
    HttpClientConfig(url = "https://apiws.alfabank.ru"),
    "IA",
    EmptyString
  )

  private val vtbClientConfig = VtbClientConfig(
    HttpClientConfig(url = "https://extcrmti.vtb24.ru:7443"),
    productName = "А_Авто.ру"
  )

  private val sravniRuClientConfig = SravniRuClientConfig(
    HttpClientConfig(url = "http://public.partner.qa.sravni-team.ru"),
    EmptyString
  )

  private val psbClientConfig = PsbClientConfig(
    HttpClientConfig(url = "https://retail-tst.payment.ru"),
    "13",
    "108544174"
  )

  private val ecreditClientConfig = EcreditClientConfig(
    HttpClientConfig(url = "https://test-api-online.ecredit.one"),
    EmptyString,
    EmptyString
  )

  private val dadataClientConfig = DadataClientConfig(
    HttpClientConfig(url = "https://suggestions.dadata.ru:443"),
    EmptyString
  )

  private val vosAutoruClientConfig = VosAutoruClientConfig(
    HttpClientConfig(url = "http://vos2-autoru-api.vrts-slb.test.vertis.yandex.net:80")
  )

  private val palmaConfig = GrpcClientConfig(
    endpoint = "palma-api-grpc-api.vrts-slb.test.vertis.yandex.net:80"
  )

  private lazy val vosAutoruClientLayer =
    ZLayer.requires[Blocking] ++
      ZLayer.succeed(vosAutoruClientConfig.http) >>>
      HttpClient.blockingLayer ++
      ZLayer.succeed(vosAutoruClientConfig) >>>
      VosAutoruClient.live

  private val tinkoffBankCardCreditConverter: ULayer[TinkoffBankConverter] =
    ZLayer.succeed(new TinkoffBankCardCreditConverter)

  private val tinkoffBankAutoCreditConverter: ULayer[TinkoffBankConverter] =
    ZLayer.succeed(new TinkoffBankAutoCreditConverter)

  private val tinkoffBankCarConverter: ULayer[TinkoffBankCarConverter] =
    ZLayer.succeed(new TinkoffBankCarConverterImpl)

  private val tinkoffBankClientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(tinkoffBankClientConfig.http) >>>
    HttpClient.blockingLayer ++ ZLayer.succeed(tinkoffBankClientConfig) >>> TinkoffBankClient.live

  private val tinkoffBankCardCreditReportsClientLayer =
    ZLayer.succeed(tinkoffBankCardCreditReportsClientConfig.http) >>>
      HttpClient.asyncLayer ++ ZLayer.succeed(tinkoffBankCardCreditReportsClientConfig) >>>
      TinkoffBankCardCreditReportsClient.live

  private val vtbClientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(vtbClientConfig.http) >>>
    HttpClient.blockingLayer ++ ZLayer.succeed(vtbClientConfig) >>> VtbBankClient.live

  private lazy val idGeneratorLayer =
    Random.any >>> IdGenerator.random

  private lazy val dadataClientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(dadataClientConfig.http) >>>
    HttpClient.blockingLayer ++ ZLayer.succeed(dadataClientConfig) >>> DadataClient.live

  private lazy val staticRaiffeisenBankResourceLayer =
    ZLayer.succeed[Resource[Any, RaiffeisenBankDictionary.Service]](new StaticRaiffeisenBankResource)

  private lazy val raiffeisen1BankConverterLayer =
    staticRaiffeisenBankResourceLayer >>> RaiffeisenBankConverter.liveAny

  private lazy val raiffeisen2BankConverterLayer =
    staticRaiffeisenBankResourceLayer >>> RaiffeisenBankConverter.liveRefinancing

  private lazy val raiffeisenBankClientLayer =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(raiffeisenBankClientConfig.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(raiffeisenBankClientConfig) >>> RaiffeisenBankClient.live

  private lazy val gazpromBankConverterLayer = GeocoderClientMock.empty ++ Clock.any ++ regionsDictionaryMock ++
    ZLayer.succeed[Resource[Any, GazpromBankDictionary.Service]](new StaticGazpromBankResource) >>>
    GazpromBankConverter.live

  private lazy val rosgosstrahBankConverterLayer =
    idGeneratorLayer ++ dadataClientLayer >>> RosgosstrahBankConverter.live

  private lazy val sovcomBankConverterLayer = SovcomBankConverter.live

  private lazy val gazpromBankClientLayer =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(gazpromBankClientConfig.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(gazpromBankClientConfig) >>> GazpromBankClient.live

  private lazy val rosgosstrahBankClientLayer =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(rosgosstrahBankClientConfig.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(rosgosstrahBankClientConfig) >>> RosgosstrahBankClient.live

  private lazy val sovcomBankClientLayer =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(sovcomBankClientConfig.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(sovcomBankClientConfig) >>> SovcomBankClient.live

  private val dealerConfigsMockResult = Seq.empty

  private val dealerConfigurationMockLayer =
    DealerConfigurationDictionaryMock
      .List(
        anything,
        Expectation.value(dealerConfigsMockResult)
      )
      .optional

  private lazy val tinkoff1BankSenderLayer =
    Clock.any ++ vosAutoruClientLayer ++
      tinkoffBankClientLayer ++ tinkoffBankAutoCreditConverter ++ tinkoffBankCarConverter >>>
      Tinkoff1CreditApplicationBankSender.live

  private lazy val tinkoff2BankSenderLayer =
    Clock.any ++
      tinkoffBankClientLayer ++ tinkoffBankCardCreditReportsClientLayer ++ tinkoffBankCardCreditConverter >>>
      Tinkoff2CreditApplicationBankSender.live

  private lazy val raiffeisen1BankSenderLayer =
    Clock.any ++ raiffeisenBankClientLayer ++ raiffeisen1BankConverterLayer >>>
      Raiffeisen1CreditApplicationBankSender.live

  private lazy val raiffeisen2BankSenderLayer =
    Clock.any ++ raiffeisenBankClientLayer ++ raiffeisen2BankConverterLayer >>>
      Raiffeisen2CreditApplicationBankSender.live

  private lazy val gazpromBankSenderLayer =
    Clock.any ++ vosAutoruClientLayer ++ gazpromBankClientLayer ++ gazpromBankConverterLayer >>>
      Gazprom1CreditApplicationBankSender.live

  private lazy val rosgosstrahBankSenderLayer =
    rosgosstrahBankClientLayer ++ rosgosstrahBankConverterLayer >>> Rosgosstrah1CreditApplicationBankSender.live

  private lazy val sovcomBankSenderLayer =
    sovcomBankClientLayer ++ sovcomBankConverterLayer >>> Sovcom1CreditApplicationBankSender.live

  private lazy val alfa1BankAutoSenderLayer = {
    val bankClientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(alfaBankClientConfig.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(alfaBankClientConfig) >>> AlfaBankClient.live
    val bankConverterLayer = ZLayer.succeed[Resource[Any, AlfaBankDictionary.Service]](new StaticAlfaBankResource) >>>
      AlfaBankConverter.livePil
    val idGeneratorLayer = IdGenerator.snowflake
    bankClientLayer ++ bankConverterLayer ++ idGeneratorLayer >>>
      Alfa1CreditApplicationBankSender.live
  }

  private lazy val alfa2BankAutoSenderLayer = {
    val bankClientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(alfaBankClientConfig.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(alfaBankClientConfig) >>> AlfaBankClient.live
    val bankConverterLayer = ZLayer.succeed[Resource[Any, AlfaBankDictionary.Service]](new StaticAlfaBankResource) ++
      regionsDictionaryMock >>>
      AlfaBankConverter.liveCreditCard
    val idGeneratorLayer = IdGenerator.snowflake
    bankClientLayer ++ bankConverterLayer ++ idGeneratorLayer >>>
      Alfa2CreditApplicationBankSender.live
  }

  private lazy val staticVtbResourceLayer = ZLayer.succeed[Resource[Any, VtbDictionary.Service]](new StaticVtbResource)

  private lazy val vtbMiniAppConverterLayer =
    IdGenerator.snowflake ++
      regionsDictionaryMock ++
      staticVtbResourceLayer ++
      ZLayer.succeed(vtbClientConfig) >>>
      VtbMiniAppConverter.live

  private lazy val vtbFullAppConverterLayer =
    staticVtbResourceLayer ++
      regionsDictionaryMock ++
      ZLayer.succeed(vtbClientConfig) >>>
      VtbFullAppConverter.live

  private lazy val vtb1BankAutoSenderLayer = {
    vtbClientLayer ++
      vtbMiniAppConverterLayer ++
      vtbFullAppConverterLayer ++
      ZLayer.succeed(vtbClientConfig) >>>
      Vtb1CreditApplicationBankSender.live
  }

  private lazy val sravniRu1Sender = {
    val bankClientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(sravniRuClientConfig.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(sravniRuClientConfig) >>> SravniRuClient.live
    val bankConverterLayer = SravniRuConverter.live
    bankClientLayer ++ bankConverterLayer >>> SravniRu1Sender.live
  }

  private lazy val psbBankConverterLayer = ZLayer.succeed(psbClientConfig) >>> PsbConverter.live

  private lazy val psbBankClientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(psbClientConfig.http) >>>
    HttpClient.blockingLayer ++ ZLayer.succeed(psbClientConfig) >>> PsbClient.live

  private lazy val psb1BankSenderLayer =
    psbBankClientLayer ++ psbBankConverterLayer >>> Psb1CreditApplicationBankSender.live

  private lazy val sberBankConfig = SberBankConfig(new URL("https://sberbank.ru"), "autoru")

  private lazy val sberBankUrlConverterLayer =
    ZLayer.succeed[Resource[Any, SberBankDictionary.Service]](new StaticSberBankResource) ++
      ZLayer.succeed(sberBankConfig) >>> SberBankUrlConverter.live

  private lazy val sber1BankSenderLayer = sberBankUrlConverterLayer >>> Sber1CreditApplicationBankSender.live

  private lazy val dealerAppConverterLayer = dealerConfigurationMockLayer >>> DealerApplicationConverter.live

  private lazy val palmaConfigLayer = ZLayer.succeed(palmaConfig)

  private lazy val palmaGrpcClientLayer =
    palmaConfigLayer >>> GrpcClient.live[ProtoDictionaryService](new ProtoDictionaryServiceStub(_))

  private lazy val palmaClientLayer = palmaGrpcClientLayer >>> PalmaClient.live

  private lazy val dealerApplicationClientLayer = palmaClientLayer >>> DealerApplicationClient.live

  private lazy val ecreditClientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(ecreditClientConfig.http) >>>
    HttpClient.blockingLayer ++ ZLayer.succeed(ecreditClientConfig) >>> EcreditClient.live

  private lazy val ecreditNewAppConverterLayer =
    ZLayer.succeed[Resource[Any, EcreditMarksDictionary.Service]](new StaticEcreditMarksResource) >>>
      EcreditMarksDictionary.live >>>
      EcreditNewAppConverter.live

  private lazy val ecreditEditAppConverterLayer = EcreditEditAppConverter.live

  private lazy val dealer1BankSenderLayer =
    dealerApplicationClientLayer ++ dealerAppConverterLayer ++ dealerConfigurationMockLayer ++
      ecreditClientLayer ++ ecreditNewAppConverterLayer ++ ecreditEditAppConverterLayer >>> Dealer1BankSender.live

  private lazy val bankSenderProviderLayer =
    tinkoff1BankSenderLayer ++ tinkoff2BankSenderLayer ++
      raiffeisen1BankSenderLayer ++ raiffeisen2BankSenderLayer ++
      rosgosstrahBankSenderLayer ++ sovcomBankSenderLayer ++
      alfa1BankAutoSenderLayer ++ alfa2BankAutoSenderLayer ++
      vtb1BankAutoSenderLayer ++ gazpromBankSenderLayer ++
      sravniRu1Sender ++ psb1BankSenderLayer ++
      sber1BankSenderLayer ++ dealer1BankSenderLayer >>>
      CreditApplicationBankSenderProvider.liveAuto

  private val creditProductDictionaryLayer: ULayer[CreditProductDictionary] = ZLayer.succeed {
    new CreditProductDictionary.Service {
      protected val all: Seq[CreditProduct] = Seq(
        gen[AutoCreditProduct].arbitrary.sample.get
          .copy(id = Tinkoff1CreditApplicationBankSender.CreditProductId, isActive = true),
        gen[AutoCreditProduct].arbitrary.sample.get
          .copy(id = Raiffeisen1CreditApplicationBankSender.CreditProductId, isActive = false),
        gen[AutoCreditProduct].arbitrary.sample.get
          .copy(id = Gazprom1CreditApplicationBankSender.CreditProductId, isActive = false)
      )
      override def list(filter: CreditProductDictionary.Filter): UIO[Seq[CreditProduct]] =
        UIO.effectTotal(all)
      override def get(id: CreditProductId): UIO[Option[CreditProduct]] =
        UIO.effectTotal(all.find(_.id == id))
    }
  }

  private lazy val decisionFromBankStageLayer = Clock.any ++ bankSenderProviderLayer ++ creditProductDictionaryLayer >>>
    DecisionFromBankStage.live(Domain.DOMAIN_AUTO)

  private lazy val layer = TestClock.any ++ decisionFromBankStageLayer

  def spec: ZSpec[TestEnvironment, Any] =
    suite("DecisionFromBankStage")(
      testM("Process with raiffeisen") {
        val actual = for {
          clock <- ZIO.service[Clock.Service]
          _ <- TestClock.setDateTime(OffsetDateTime.now())
          ts <- clock.instant
          claim = AutoruClaim.forTest(
            id = "some-claim-id".taggedWith[Tag.CreditApplicationClaimId],
            bankClaimId = Option("OAPI20201214PLL002111200636525".taggedWith[Tag.CreditApplicationBankClaimId]),
            created = ts,
            updated = ts,
            processAfter = None,
            creditProductId = "raiffeisen-1".taggedWith[Tag.CreditProductId],
            state = proto.CreditApplication.Claim.ClaimState.ISSUE
          )
          creditApplication = sampleAutoruCreditApplication().copy(
            claims = Seq(claim),
            state = proto.CreditApplication.State.ACTIVE
          )
          s <- ZIO.service[Stage.Service[CreditApplication, CreditApplication.UpdateRequest]]
          res <- s.process(creditApplication, ScheduleDurationRange)
        } yield {
          println(res.result)
          println(res.reschedule)
          res.result.flatMap(_.claims.headOption.flatMap(_._2.bankClaimId))
        }
        assertM(actual)(isSome).provideLayer(layer)
      }
    ) @@ ignore

  private val ScheduleDurationRange: DurationRange = DurationRange(1.minute, 2.minutes)
}
