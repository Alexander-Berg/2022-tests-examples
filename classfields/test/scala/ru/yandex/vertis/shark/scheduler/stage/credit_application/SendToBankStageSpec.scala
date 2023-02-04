package ru.yandex.vertis.shark.scheduler.stage.credit_application

import baker.common.client.dadata.model.Responses.SuccessResponse
import baker.common.client.dadata.model.{Address, Organization, Suggestion}
import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import org.scalacheck.magnolia._
import ru.auto.api.api_offer_model.{Category, Section}
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
import ru.yandex.vertis.shark.controller._
import ru.yandex.vertis.shark.dictionary.{CreditProductDictionary, DealerConfigurationDictionary, RegionsDictionary}
import ru.yandex.vertis.shark.enricher.CreditProductEnricher
import ru.yandex.vertis.shark.model.CreditApplication.AutoruClaim
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.model.generators.{AutoruOfferGen, CreditApplicationGen}
import ru.yandex.vertis.shark.proto.{model => proto}
import ru.yandex.vertis.shark.scheduler.stage.Stage
import ru.yandex.vertis.shark.sender.impl._
import ru.yandex.vertis.shark.sender.{ConverterContextDecider, CreditApplicationBankSenderProvider}
import ru.yandex.vertis.zio_baker.geobase.{Region, RegionTypes}
import ru.yandex.vertis.zio_baker.util.EmptyString
import ru.yandex.vertis.zio_baker.zio.features.DurationRange
import ru.yandex.vertis.zio_baker.zio.grpc.client.GrpcClient
import ru.yandex.vertis.zio_baker.zio.grpc.client.config.GrpcClientConfig
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import ru.yandex.vertis.zio_baker.zio.palma.PalmaClient
import _root_.common.id.IdGenerator
import ru.yandex.vertis.shark.client.bank.converter.TinkoffBankConverter.{TinkoffBankCarConverter, TinkoffBankConverter}
import ru.yandex.vertis.shark.controller.CreditProductCalculator.CreditProductCalculator
import ru.yandex.vertis.shark.controller.credit_product_calculator.testkit.CreditProductCalculatorMock
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion.{anything, equalTo, isNone, isSome}
import zio.test.environment.TestEnvironment
import zio.test.mock.{mockable, Expectation}
import zio.test.{assert, assertM, DefaultRunnableSpec, ZSpec}
import zio.{Has, Runtime, UIO, ULayer, ZIO, ZLayer}
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.scheduler.stage.credit_application.DecisionFromBankStageSpec.regionsDictionaryMock
import baker.common.client.dadata.DadataClient
import ru.yandex.vertis.zio_baker.zio.client.geocoder.GeocoderClient
import ru.yandex.vertis.zio_baker.zio.client.vos.{VosAutoruClient, VosAutoruClientConfig}
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.zio.resource.Resource
import zio.blocking.Blocking

import java.net.URL
import java.time.Instant
import scala.concurrent.duration._

object SendToBankStageSpec extends DefaultRunnableSpec with CreditApplicationGen with AutoruOfferGen {

  @mockable[VosAutoruClient.Service]
  object VosAutoruClientMock

  @mockable[RegionsDictionary.Service]
  object RegionsDictionaryMock

  @mockable[DealerConfigurationDictionary.Service]
  object DealerConfigurationDictionaryMock

  @mockable[DadataClient.Service]
  object DadataClientMock

  @mockable[PersonProfileController.Service]
  object PersonProfileControllerMock

  @mockable[GeocoderClient.Service]
  object GeocoderClientMock

  private val creditProductId = Tinkoff1CreditApplicationBankSender.CreditProductId

  private def creditProductCalculatorMock(passed: Boolean = true) =
    CreditProductCalculatorMock
      .Suitable(
        anything,
        Expectation.value(
          Suitable(
            creditProductId,
            passed,
            CheckRequirements.Passed,
            CheckBorrower.Passed,
            MissingBlocks(Seq.empty, Seq.empty),
            MissingBlocks(Seq.empty, Seq.empty),
            CheckRateLimit.Passed,
            Seq.empty
          )
        )
      )
      .optional

  private val vosAutoruClientMock: Expectation[Has[VosAutoruClient.Service]] = VosAutoruClientMock
    .Offer(
      anything,
      Expectation.value(
        sampleOffer()
      )
    )
    .atLeast(0)

  private val emptyOrganization =
    Organization(
      inn = None,
      kpp = None,
      ogrn = None,
      ogrnDate = None,
      hid = None,
      `type` = None,
      name = None,
      okato = None,
      oktmo = None,
      okpo = None,
      okogu = None,
      okfs = None,
      okved = None,
      okvedType = None,
      okveds = None,
      opf = None,
      management = None,
      branchCount = None,
      branchType = None,
      address = None,
      state = None,
      employeeCount = None,
      authorities = None,
      citizenship = None,
      source = None,
      qc = None,
      finance = None,
      founders = None,
      managers = None,
      predecessors = None,
      successors = None,
      licenses = None,
      capital = None,
      documents = None,
      phones = None
    )

  private val dadataExpectation = Expectation.value(
    Suggestion("org1", "org1", emptyOrganization.some).some
  )

  private val dadataAddressExpectation = Expectation.value(
    SuccessResponse[Address](Seq.empty)
  )

  private val dadataFioExpectation = Expectation.value(None)

  private val dadataClientMock: Expectation[Has[DadataClient.Service]] =
    (DadataClientMock.OrganizationByInn(anything, dadataExpectation).optional &&
      DadataClientMock.AddressSuggestionsByString(anything, dadataAddressExpectation).atMost(2) &&
      DadataClientMock.Fio(anything, dadataFioExpectation).atMost(1)).optional

  private val regionsMock = Seq(
    Region(213L.taggedWith, 1L.taggedWith, 0L.taggedWith, RegionTypes.City, "Москва", 55.753215d, 37.622504d, 10800)
  )

  private val personProfileMock = PersonProfileStub.forTest(blockTypes = Seq.empty)

  private val regionsDictionaryMock = {
    val mockIsInside = RegionsDictionaryMock
      .IsInside(
        anything,
        Expectation.value(true)
      )
      .atLeast(0)

    val mockGetParentRegions = RegionsDictionaryMock
      .GetParentRegions(
        anything,
        Expectation.value(regionsMock)
      )
      .atLeast(0)

    mockIsInside && mockGetParentRegions
  }

  private val dealerConfigsMockResult = Seq.empty

  private val dealerConfigurationDictionaryMock =
    DealerConfigurationDictionaryMock
      .List(
        anything,
        Expectation.value(dealerConfigsMockResult)
      )
      .optional

  private val personProfileControllerMock = PersonProfileControllerMock
    .GetById(anything, Expectation.value(personProfileMock))
    .optional

  private val creditProductDictionaryLayer: ULayer[CreditProductDictionary] = ZLayer.succeed {
    new CreditProductDictionary.Service {

      protected val all: Seq[CreditProduct] = Seq(
        gen[AutoCreditProduct].arbitrary.sample.get
          .copy(id = Tinkoff1CreditApplicationBankSender.CreditProductId, isActive = true, rateLimit = None),
        gen[AutoCreditProduct].arbitrary.sample.get
          .copy(id = Raiffeisen1CreditApplicationBankSender.CreditProductId, isActive = false, rateLimit = None),
        gen[AutoCreditProduct].arbitrary.sample.get
          .copy(id = Gazprom1CreditApplicationBankSender.CreditProductId, isActive = false, rateLimit = None)
      )

      override def list(filter: CreditProductDictionary.Filter): UIO[Seq[CreditProduct]] =
        UIO.effectTotal(all)

      override def get(id: CreditProductId): UIO[Option[CreditProduct]] =
        UIO.effectTotal(all.find(_.id == id))
    }
  }

  private val tinkoffBankClientConfig = TinkoffBankClientConfig(
    HttpClientConfig(url = "https://api.tinkoff.ru:443")
  )

  private val tinkoffBankCardCreditReportsClientConfig = TinkoffBankCardCreditReportsClientConfig(
    HttpClientConfig(url = "https://offer.datamind.ru"),
    EmptyString
  )

  private val gazpromBankClientConfig = GazpromBankClientConfig(
    HttpClientConfig(url = "https://testapi.gazprombank.ru/gpbapi/test"),
    "",
    ""
  )

  private val raiffeisenBankClientConfig = RaiffeisenBankClientConfig(
    HttpClientConfig(url = "https://api.raiffeisen.ru")
  )

  private lazy val rosgosstrahBankClientConfig = RosgosstrahBankClientConfig(
    HttpClientConfig(url = "https://testapi.gazprombank.ru/gpbapi/test"),
    AuthConfig(EmptyString, EmptyString)
  )

  private lazy val sovcomBankClientConfig = SovcomBankClientConfig(
    HttpClientConfig(url = "https://api-app2.sovcombank.ru"),
    EmptyString
  )

  private lazy val alfaBankClientConfig = AlfaBankClientConfig(
    HttpClientConfig(url = "https://apiws.alfabank.ru"),
    "IA",
    EmptyString
  )

  private val vtbConfig = VtbClientConfig(
    HttpClientConfig(url = "https://extcrmti.vtb24.ru:7443"),
    productName = "А_Авто.ру"
  )

  private lazy val sravniRuClientConfig = SravniRuClientConfig(
    HttpClientConfig(url = "http://public.partner.qa.sravni-team.ru"),
    EmptyString
  )

  private val psbClientConfig = PsbClientConfig(
    HttpClientConfig(url = "https://retail-tst.payment.ru"),
    EmptyString,
    EmptyString
  )

  private val ecreditClientConfig = EcreditClientConfig(
    HttpClientConfig(url = "https://test-api-online.ecredit.one"),
    EmptyString,
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

  private val vtbClientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(vtbConfig.http) >>>
    HttpClient.blockingLayer ++ ZLayer.succeed(vtbConfig) >>> VtbBankClient.live

  private lazy val idGeneratorLayer =
    Random.any >>> IdGenerator.random

  private lazy val tinkoffBankClientLayer =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(tinkoffBankClientConfig.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(tinkoffBankClientConfig) >>> TinkoffBankClient.live

  private lazy val tinkoffBankCardCreditReportsClientLayer =
    ZLayer.succeed(tinkoffBankCardCreditReportsClientConfig.http) >>>
      HttpClient.asyncLayer ++ ZLayer.succeed(tinkoffBankCardCreditReportsClientConfig) >>>
      TinkoffBankCardCreditReportsClient.live

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

  private lazy val rosgosstrahBankConverterLayer = {
    idGeneratorLayer ++ dadataClientMock >>> RosgosstrahBankConverter.live
  }

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

  private val tinkoffBankCardCreditConverter: ULayer[TinkoffBankConverter] =
    ZLayer.succeed(new TinkoffBankCardCreditConverter)

  private val tinkoffBankAutoCreditConverter: ULayer[TinkoffBankConverter] =
    ZLayer.succeed(new TinkoffBankAutoCreditConverter)

  private val tinkoffBankCarConverter: ULayer[TinkoffBankCarConverter] =
    ZLayer.succeed(new TinkoffBankCarConverterImpl)

  private lazy val tinkoff1BankSenderLayer =
    Clock.any ++ vosAutoruClientLayer ++ tinkoffBankClientLayer ++
      tinkoffBankAutoCreditConverter ++ tinkoffBankCarConverter >>>
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
    Clock.any ++ vosAutoruClientLayer ++ rosgosstrahBankClientLayer ++ rosgosstrahBankConverterLayer >>>
      Rosgosstrah1CreditApplicationBankSender.live

  private lazy val sovcomBankSenderLayer =
    Clock.any ++ sovcomBankClientLayer ++ sovcomBankConverterLayer >>>
      Sovcom1CreditApplicationBankSender.live

  private lazy val alfa1BankSenderLayer = {
    val bankClientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(alfaBankClientConfig.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(alfaBankClientConfig) >>> AlfaBankClient.live
    val bankConverterLayer = ZLayer.succeed[Resource[Any, AlfaBankDictionary.Service]](new StaticAlfaBankResource) >>>
      AlfaBankConverter.livePil
    val idGeneratorLayer = IdGenerator.snowflake
    bankClientLayer ++ bankConverterLayer ++ idGeneratorLayer >>>
      Alfa1CreditApplicationBankSender.live
  }

  private lazy val alfa2BankSenderLayer = {
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
      ZLayer.succeed(vtbConfig) >>>
      VtbMiniAppConverter.live

  private lazy val vtbFullAppConverterLayer =
    staticVtbResourceLayer ++
      regionsDictionaryMock ++
      ZLayer.succeed(vtbConfig) >>>
      VtbFullAppConverter.live

  private lazy val vtb1BankAutoSenderLayer = {
    vtbClientLayer ++
      vtbMiniAppConverterLayer ++
      vtbFullAppConverterLayer ++
      ZLayer.succeed(vtbConfig) >>>
      Vtb1CreditApplicationBankSender.live
  }

  private lazy val sravniRu1SenderLayer = {
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

  private lazy val palmaConfigLayer = ZLayer.succeed(palmaConfig)

  private lazy val palmaGrpcClientLayer =
    palmaConfigLayer >>> GrpcClient.live[ProtoDictionaryService](new ProtoDictionaryServiceStub(_))

  private lazy val palmaClientLayer = palmaGrpcClientLayer >>> PalmaClient.live

  private lazy val dealerApplicationConverterLayer =
    dealerConfigurationDictionaryMock >>> DealerApplicationConverter.live

  private lazy val dealerApplicationClientLayer = palmaClientLayer >>> DealerApplicationClient.live

  private lazy val ecreditClientLayer = ZLayer.requires[Blocking] ++ ZLayer.succeed(ecreditClientConfig.http) >>>
    HttpClient.blockingLayer ++ ZLayer.succeed(ecreditClientConfig) >>> EcreditClient.live

  private lazy val ecreditNewAppConverterLayer =
    ZLayer.succeed[Resource[Any, EcreditMarksDictionary.Service]](new StaticEcreditMarksResource) >>>
      EcreditMarksDictionary.live >>>
      EcreditNewAppConverter.live

  private lazy val ecreditEditAppConverterLayer = EcreditEditAppConverter.live

  private lazy val dealer1BankSenderLayer =
    dealerApplicationClientLayer ++ dealerApplicationConverterLayer ++ dealerConfigurationDictionaryMock ++
      ecreditClientLayer ++ ecreditNewAppConverterLayer ++ ecreditEditAppConverterLayer >>> Dealer1BankSender.live

  private lazy val bankSenderProviderLayer =
    tinkoff1BankSenderLayer ++ tinkoff2BankSenderLayer ++
      raiffeisen1BankSenderLayer ++ raiffeisen2BankSenderLayer ++
      alfa1BankSenderLayer ++ alfa2BankSenderLayer ++
      rosgosstrahBankSenderLayer ++ sovcomBankSenderLayer ++
      vtb1BankAutoSenderLayer ++ gazpromBankSenderLayer ++
      sravniRu1SenderLayer ++ psb1BankSenderLayer ++
      sber1BankSenderLayer ++ dealer1BankSenderLayer >>>
      CreditApplicationBankSenderProvider.liveAuto

  private lazy val autoConverterContextDeciderLayer =
    creditProductControllerLayer ++
      (dadataClientMock && vosAutoruClientMock && regionsDictionaryMock && personProfileControllerMock) >>>
      ConverterContextDecider.liveAuto

  private lazy val creditProductEnricherLayer = regionsDictionaryMock.toLayer ++
    dealerConfigurationDictionaryMock.toLayer ++ vosAutoruClientLayer >>> CreditProductEnricher.live

  private lazy val creditProductControllerLayer = creditProductDictionaryLayer ++ creditProductEnricherLayer >>>
    CreditProductController.live

  private def sendToBankStageLayer(mockLayer: Expectation[CreditProductCalculator] = creditProductCalculatorMock()) =
    Clock.any ++ bankSenderProviderLayer ++ creditProductControllerLayer ++
      autoConverterContextDeciderLayer ++ mockLayer >>> SendToBankStage.live(Domain.DOMAIN_AUTO)

  private val ts = Instant.now()

  private val testClaim = AutoruClaim.forTest(
    id = "some-claim-id".taggedWith[Tag.CreditApplicationClaimId],
    bankClaimId = None,
    created = ts,
    updated = ts,
    processAfter = None,
    creditProductId = creditProductId,
    state = proto.CreditApplication.Claim.ClaimState.DRAFT,
    bankState = None,
    approvedMaxAmount = None,
    approvedTermMonths = None,
    approvedInterestRate = None,
    approvedMinInitialFeeRate = None,
    offerEntities = Seq(
      AutoruClaim.OfferEntity(
        offer = AutoruCreditApplication.Offer(
          category = Category.CARS,
          section = Section.NEW,
          id = "123-hash".taggedWith[zio_baker.Tag.OfferId],
          sellerType = None,
          userRef = None
        ),
        created = ts,
        updated = ts,
        state = proto.CreditApplication.Claim.ClaimPayload.ObjectState.DRAFT
      )
    )
  )

  def spec: ZSpec[TestEnvironment, Any] =
    suite("SendToBankStage")(
      testM("Process with tinkoff") {
        val creditApplication = sampleAutoruCreditApplication().copy(
          claims = Seq(testClaim),
          state = proto.CreditApplication.State.ACTIVE
        )
        val actual = ZIO
          .accessM[Stage[CreditApplication, CreditApplication.UpdateRequest]](
            _.get.process(creditApplication, ScheduleDurationRange)
          )
          .map(_.result.flatMap(_.claims.headOption.flatMap(_._2.bankClaimId)))
        assertM(actual)(isSome).provideLayer(sendToBankStageLayer())
      },
      testM("Process with inactive credit product") {
        val claim = testClaim.copy(creditProductId = Raiffeisen1CreditApplicationBankSender.CreditProductId)
        val creditApplication = sampleAutoruCreditApplication().copy(
          claims = Seq(claim),
          state = proto.CreditApplication.State.ACTIVE
        )
        val actual = ZIO
          .accessM[Stage[CreditApplication, CreditApplication.UpdateRequest]](
            _.get.process(creditApplication, ScheduleDurationRange)
          )
          .map(_.result.flatMap(_.claims.headOption.flatMap(_._2.bankClaimId)))
        assertM(actual)(isNone).provideLayer(sendToBankStageLayer())
      },
      testM("Process with tinkoff when passed is false") {
        val creditApplication = sampleAutoruCreditApplication().copy(
          claims = Seq(testClaim),
          state = proto.CreditApplication.State.ACTIVE
        )
        val actual = ZIO
          .accessM[Stage[CreditApplication, CreditApplication.UpdateRequest]](
            _.get.process(creditApplication, ScheduleDurationRange)
          )
          .map(_.result.flatMap(_.claims.headOption.flatMap(_._2.bankClaimId)))
        assertM(actual)(isNone).provideLayer(sendToBankStageLayer(creditProductCalculatorMock(passed = false)))
      },
      testM("Add SentSnapshot") {
        val creditApplication = sampleAutoruCreditApplication().copy(
          claims = Seq(testClaim),
          state = proto.CreditApplication.State.ACTIVE
        )
        val getStageResult = ZIO
          .accessM[Stage[CreditApplication, CreditApplication.UpdateRequest]](
            _.get.process(creditApplication, ScheduleDurationRange)
          )

        getStageResult
          .map { stageResult =>
            val claim = stageResult.result.flatMap(_.claims.values.headOption)
            val sentSnapshot = claim.flatMap(_.sentSnapshot)
            assert(sentSnapshot)(isSome) &&
            assert(sentSnapshot.flatMap(_.borrowerPersonProfileId))(
              equalTo(creditApplication.borrowerPersonProfile.flatMap(_.id))
            )
          }
          .provideLayer(sendToBankStageLayer())
      }
    )

  val ScheduleDurationRange: DurationRange = DurationRange(1.minute, 2.minutes)
}
