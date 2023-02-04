package ru.yandex.vertis.shark.scheduler.sender

import baker.common.client.dadata.DadataClient
import baker.common.client.dadata.DadataClient.DadataClient
import com.softwaremill.tagging.Tagger
import common.ops.prometheus.DefaultMetricsSupport
import common.zio.clients.s3.{S3Client, S3ClientLive}
import common.zio.features.testkit.FeaturesTest
import common.zio.ops.prometheus.Prometheus
import common.zio.ops.prometheus.Prometheus.Prometheus
import ru.yandex.vertis.ops.prometheus.{PrometheusRegistry, SimpleCompositeCollector}
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.palma.services.proto_dictionary_service.ProtoDictionaryServiceGrpc.{
  ProtoDictionaryService,
  ProtoDictionaryServiceStub
}
import ru.yandex.vertis.shark.{Mock, TestSharedConfig}
import ru.yandex.vertis.shark.config.S3EdrConfig
import ru.yandex.vertis.shark.controller.requirements.RequirementsCheck
import ru.yandex.vertis.shark.controller.{
  CreditProductCalculator,
  CreditProductController,
  CreditProductRateCounter,
  PersonProfileController
}
import ru.yandex.vertis.shark.dictionary.{CreditProductDictionary, DealerConfigurationDictionary, RegionsDictionary}
import ru.yandex.vertis.shark.enricher.CreditProductEnricher
import ru.yandex.vertis.shark.model.Tag
import ru.yandex.vertis.shark.resource.CreditProductResource
import ru.yandex.vertis.shark.scheduler.sender.TestLayers.TestLayer
import ru.yandex.vertis.shark.sender._
import ru.yandex.vertis.zio_baker.geobase.{Region, RegionTypes}
import ru.yandex.vertis.zio_baker.util.DateTimeUtil.RichDuration
import ru.yandex.vertis.zio_baker.zio.client.vos.VosAutoruClient
import ru.yandex.vertis.zio_baker.zio.grpc.client.GrpcClient
import ru.yandex.vertis.zio_baker.zio.grpc.client.config.GrpcClientConfig
import ru.yandex.vertis.zio_baker.zio.grpc.client.impl.TvmGrpcClient
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.palma.PalmaClient
import ru.yandex.vertis.zio_baker.zio.resource.ResourceLoader
import ru.yandex.vertis.zio_baker.zio.resource.impl.{DealerCreditConfigurationResource, RegionsResource}
import ru.yandex.vertis.shark.controller.CreditProductCalculator.CreditProductCalculator
import ru.yandex.vertis.shark.controller.PersonProfileController.PersonProfileController
import ru.yandex.vertis.zio_baker.zio.s3edr.{S3EdrReader, S3EdrReaderImpl}
import ru.yandex.vertis.zio_baker.zio.tvm.Tvm
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.Assertion.anything
import zio.test.environment.{TestClock, TestEnvironment}
import zio.test.mock.{mockable, Expectation}
import zio.{Has, RLayer, UIO, ULayer, ZIO, ZLayer}

import java.time.OffsetDateTime
import scala.concurrent.duration.DurationInt

trait TestLayers {

  def config: TestSharedConfig
  def bankSenderLayer: RLayer[TestEnvironment, CreditApplicationBankSender]

  @mockable[CreditProductRateCounter.Service]
  object CreditProductRateCounterMock

  @mockable[RegionsDictionary.Service]
  object RegionsDictionaryMock

  @mockable[DealerConfigurationDictionary.Service]
  object DealerConfigurationDictionaryMock

  private val creditProductRateCounterMock = CreditProductRateCounterMock
    .ActualCount(
      anything,
      Expectation.value(0.taggedWith[Tag.RequestRate])
    )
    .optional

  private val regionsMock = Seq(
    Region(213L.taggedWith, 1L.taggedWith, 0L.taggedWith, RegionTypes.City, "Москва", 55.753215d, 37.622504d, 10800)
  )

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

  private lazy val tvmLayer: ZLayer[Any, Nothing, Has[Tvm.Service]] =
    ZLayer.succeed(config.tvm) ++ FeaturesTest.test >>> Tvm.live

  protected lazy val s3ClientLayer: ZLayer[Any, Nothing, S3Client.S3Client] =
    ZLayer.succeed(config.s3Client) >>> S3ClientLive.live

  protected lazy val regionsDictionaryLayer: ZLayer[Blocking with Clock, Nothing, RegionsDictionary] = {
    val regionsResourceLoaderLayer = ZLayer.requires[Clock] >>> ResourceLoader.live
    val regionsResourceLayer = ZLayer.succeed(config.regionsResource) >>> RegionsResource.live
    ZLayer.requires[Blocking] ++ s3ClientLayer ++ regionsResourceLoaderLayer ++ regionsResourceLayer >>>
      RegionsDictionary.live
  }

  protected lazy val s3EdrConfig: ULayer[Has[S3EdrConfig]] = ZLayer.succeed(config.s3Edr)

  val dataTypes = Seq(
    DealerCreditConfigurationResource.DefaultDataType
  )

  protected lazy val s3EdrReaderLayer: RLayer[Blocking, Has[S3EdrReader.Service]] = Blocking.any ++
    prometheusRegistryLayer ++ s3EdrConfig >>> S3EdrReaderImpl
      .managed(dataTypes)
      .toLayer

  protected lazy val prometheusRegistryLayer: ULayer[Prometheus] =
    ZLayer.succeed(new Prometheus.Service {

      override def registry: UIO[PrometheusRegistry] = UIO.succeed(new SimpleCompositeCollector())

      override def metricsSupport: UIO[MetricsSupport] = registry.map(DefaultMetricsSupport.apply)
    })

  protected lazy val dealerConfDictionaryLayer: ZLayer[TestEnvironment, Throwable, DealerConfigurationDictionary] = {
    val dealerResourceLoaderLayer = ZLayer.requires[Clock] >>> ResourceLoader.live
    val dealerCredConfResLayer = DealerCreditConfigurationResource.live(1.minute.asJava)
    Blocking.any ++ Clock.any ++ s3EdrReaderLayer ++ dealerResourceLoaderLayer ++ dealerCredConfResLayer >>>
      DealerConfigurationDictionary.live
  }

  protected lazy val vosAutoruClientLayer: ZLayer[Blocking, Throwable, VosAutoruClient] =
    ZLayer.requires[Blocking] ++
      ZLayer.succeed(config.vosAutoruClient.http) >>>
      HttpClient.blockingLayer ++
      ZLayer.succeed(config.vosAutoruClient) >>>
      VosAutoruClient.live

  protected lazy val dadataLayer: ZLayer[Blocking, Throwable, DadataClient] =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(config.dadataClient.http) >>>
      HttpClient.blockingLayer ++ ZLayer.succeed(config.dadataClient) >>> DadataClient.live

  private lazy val requirementsCheckLayer = regionsDictionaryLayer >>> RequirementsCheck.live

  protected lazy val calculatorLayer: RLayer[Blocking with Clock, CreditProductCalculator] = regionsDictionaryLayer ++
    creditProductRateCounterMock ++ requirementsCheckLayer ++ ZLayer.requires[Clock] >>> CreditProductCalculator.live

  protected lazy val palmaConfigLayer: ULayer[Has[GrpcClientConfig]] = ZLayer.succeed(config.palmaGrpcClient)

  protected lazy val palmaGrpcClientLayer: ULayer[Has[GrpcClientConfig] with GrpcClient[ProtoDictionaryService]] =
    palmaConfigLayer >+> GrpcClient.live[ProtoDictionaryService](new ProtoDictionaryServiceStub(_))

  protected lazy val tvmPalmaGrpcClientLayer: ULayer[GrpcClient[ProtoDictionaryService]] =
    tvmLayer ++ palmaGrpcClientLayer >>> TvmGrpcClient.wrap

  protected lazy val creditProductDictionaryLayer: RLayer[Clock with Blocking, CreditProductDictionary] = {
    val creditProductResourceLoaderLayer = ZLayer.requires[Clock] >>> ResourceLoader.live
    val creditProductResourceLayer = ZLayer.succeed(config.creditProductResource) >>> CreditProductResource.live
    Blocking.any ++ creditProductResourceLayer ++ creditProductResourceLoaderLayer ++ s3ClientLayer >>>
      CreditProductDictionary.live
  }

  protected lazy val creditProductEnricherLayer = regionsDictionaryMock.toLayer ++
    dealerConfigurationDictionaryMock.toLayer ++ vosAutoruClientLayer >>> CreditProductEnricher.live

  protected lazy val creditProductControllerLayer = creditProductDictionaryLayer ++ creditProductEnricherLayer >>>
    CreditProductController.live

  protected lazy val personProfileControllerLayer: ULayer[PersonProfileController] = {
    val palmaClientLayer = tvmPalmaGrpcClientLayer >>> PalmaClient.live
    palmaClientLayer >>> PersonProfileController.live
  }

  protected lazy val converterContextDeciderLayer: RLayer[Blocking with Clock, ConverterContextDecider] =
    vosAutoruClientLayer ++ creditProductControllerLayer ++
      regionsDictionaryLayer ++ dadataLayer ++ personProfileControllerLayer >>>
      ConverterContextDecider.liveAuto

  protected lazy val testLayer: RLayer[TestEnvironment, TestLayer] = {
    val initLayer = (for {
      _ <- TestClock.setDateTime(OffsetDateTime.now())
      clock <- ZIO.service[Clock.Service]
    } yield clock).toLayer

    initLayer ++
      creditProductDictionaryLayer ++
      calculatorLayer ++
      converterContextDeciderLayer ++
      bankSenderLayer
  }
}

object TestLayers {

  type TestLayer = Clock
    with CreditProductDictionary
    with CreditProductCalculator
    with ConverterContextDecider
    with CreditApplicationBankSender
}
