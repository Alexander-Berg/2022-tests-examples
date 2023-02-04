package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.pricequality

import java.util.concurrent.atomic.AtomicReference
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.vin.VinResolutionEnums
import ru.auto.api.vin.VinResolutionEnums.ResolutionPart
import ru.auto.api.vin.VinResolutionModel.{ResolutionEntry, VinIndexResolution}
import ru.yandex.vertis.baker.components.workdistribution.WorkDistributionData
import ru.yandex.vertis.baker.components.workersfactory.workers.{WorkResult, WorkersFactory}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.{EndpointConfig, LocalTracingSupport, Traced, TracingSupport}
import ru.yandex.vertis.ydb.skypper.YdbWrapper
import ru.yandex.vos2.AutoruModel.AutoruOffer._
import ru.yandex.vos2.autoru.catalog.SuperGenTop
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.utils.currency.CurrencyRates
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder
import ru.yandex.vos2.{BasicsModel, OfferModel}

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class PriceQualityWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val mockedFeatureManager = mock[FeaturesManager]

    val suitableOffer: OfferModel.Offer = {
      val predictRange =
        PriceRange.newBuilder().setCurrency(BasicsModel.Currency.RUB).setFrom(200000).setTo(400000).build()

      val q10q90 =
        PriceRange.newBuilder().setCurrency(BasicsModel.Currency.RUB).setFrom(340000).setTo(380000).build()

      val q25q75 =
        PriceRange.newBuilder().setCurrency(BasicsModel.Currency.RUB).setFrom(350000).setTo(370000).build()

      val market =
        Market.newBuilder().setCurrency(BasicsModel.Currency.RUB).setPrice(300000).build()

      val predictPrice = PredictPrice
        .newBuilder()
        .setAutoru(predictRange)
        .setQ10Q90(q10q90)
        .setQ25Q75(q25q75)
        .setVersion(1)
        .setMarket(market)
        .build()

      val resolutionEntry =
        ResolutionEntry.newBuilder().setPart(ResolutionPart.SUMMARY).setStatus(VinResolutionEnums.Status.OK).build()

      val vinIndexResolution =
        VinIndexResolution.newBuilder().addAllEntries(Seq(resolutionEntry).asJava).build()

      val vinResolution =
        VinResolution.newBuilder().setResolution(vinIndexResolution).setVersion(1).build()

      val price = Price
        .newBuilder()
        .setCurrency(BasicsModel.Currency.RUB)
        .setPrice(1)
        .setCreated(System.currentTimeMillis())
        .build()

      val builder = TestUtils.createOffer()
      builder.getOfferAutoruBuilder
        .setPredictPrice(predictPrice)

      builder.getOfferAutoruBuilder.getStateBuilder.setCondition(Condition.EXCELLENT)
      builder.getOfferAutoruBuilder.getEssentialsBuilder.setYear(2006)

      builder.getOfferAutoruBuilder.getStateBuilder.setMileage(2000)

      builder.getOfferAutoruBuilder
        .setVinResolution(vinResolution)

      builder.getOfferAutoruBuilder.getCarInfoBuilder.setSuperGenId(3913672)

      builder.getOfferAutoruBuilder
        .setPrice(price)

      builder.clearTag()
      builder.build()
    }

    val offerWithUnknownVinResolution = {
      val builder = suitableOffer.toBuilder
      val unknownResolutionEntry =
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.SUMMARY)
          .setStatus(VinResolutionEnums.Status.UNKNOWN)
          .build()
      val vinIndexResolution =
        VinIndexResolution.newBuilder().addAllEntries(Seq(unknownResolutionEntry).asJava).build()
      val vinResolution =
        VinResolution.newBuilder().setResolution(vinIndexResolution).setVersion(1).build()
      builder.getOfferAutoruBuilder
        .setVinResolution(vinResolution)
      builder.build()
    }

    val offerWithNoVinResolution = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.clearVinResolution()
      builder.build()
    }

    private val mockSuperGenTop: SuperGenTop = mock[SuperGenTop]
    private val mockCurrencyRates: CurrencyRates = mock[CurrencyRates]

    when(mockSuperGenTop.contains(?)).thenReturn(true)
    stub(mockCurrencyRates.convert _) {
      case (price, fromCurrency, _) =>
        if (fromCurrency != CurrencyRates.RurCurrency) Some(price * 70)
        else Some(price)
    }

    val worker: PriceQualityWorkerYdb = new PriceQualityWorkerYdb(
      mockSuperGenTop,
      mockCurrencyRates
    ) {

      import ru.yandex.vertis.vos2.autoru.workers.ydb.util.Threads

      implicit override def ec: ExecutionContext = Threads.sameThreadEc

      override def shouldWork: Boolean = true

      override def start(): Unit = ???

      override def stop(): Unit = ???

      override def features: FeaturesManager = mockedFeatureManager

      override def workersFactory: WorkersFactory = ???

      override val ignoreWorkDuration: FiniteDuration = 10.seconds
      override val workerName = ""

      override def doWork(): WorkResult = ???
    }
  }

  ("commercial good price") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(300000)
      builder.getOfferAutoruBuilder.setSellerType(SellerType.COMMERCIAL)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)

    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala
    assert(tags.contains(PriceQualityWorkerYdb.GoodPriceTag))
  }

  ("private good price") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(360000)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)

    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala

    assert(tags.contains(PriceQualityWorkerYdb.GoodPriceTag))
  }

  ("private broken: no tag") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(360000)
      builder.getOfferAutoruBuilder.getStateBuilder.setCondition(Condition.NEED_REPAIR)
      builder.addTag(PriceQualityWorkerYdb.GoodPriceTag)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)

    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala

    assert(!tags.contains(PriceQualityWorkerYdb.GoodPriceTag))
    assert(!tags.contains(PriceQualityWorkerYdb.ExcellentPriceTag))
  }

  ("set tag for unknown vin resolution") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = offerWithUnknownVinResolution.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(360000)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)
    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala

    assert(tags.contains(PriceQualityWorkerYdb.GoodPriceTag))
  }

  ("set tag for no vin resolution") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = offerWithNoVinResolution.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(360000)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)
    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala

    assert(tags.contains(PriceQualityWorkerYdb.GoodPriceTag))
  }

  ("commercial excellent price") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(260000)
      builder.getOfferAutoruBuilder.setSellerType(SellerType.COMMERCIAL)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)

    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala

    assert(tags.contains(PriceQualityWorkerYdb.ExcellentPriceTag))
  }

  ("private excellent price") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(345000)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)

    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala

    assert(tags.contains(PriceQualityWorkerYdb.ExcellentPriceTag))
  }

  ("commercial below the market") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(235000)
      builder.putTag(PriceQualityWorkerYdb.GoodPriceTag)
      builder.getOfferAutoruBuilder.setSellerType(SellerType.COMMERCIAL)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)
    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala

    assert(!tags.exists(PriceQualityWorkerYdb.NeededTags.contains))
  }

  ("private below the market") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(288000)
      builder.putTag(PriceQualityWorkerYdb.GoodPriceTag)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)

    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala

    assert(!tags.exists(PriceQualityWorkerYdb.NeededTags.contains))
  }

  ("commercial above the market") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(315000)
      builder.putTag(PriceQualityWorkerYdb.ExcellentPriceTag)
      builder.getOfferAutoruBuilder.setSellerType(SellerType.COMMERCIAL)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)

    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala

    assert(!tags.exists(PriceQualityWorkerYdb.NeededTags.contains))
  }

  ("private above the market") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(395000)
      builder.putTag(PriceQualityWorkerYdb.ExcellentPriceTag)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)

    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala

    assert(!tags.exists(PriceQualityWorkerYdb.NeededTags.contains))
  }

  ("change excellent to good") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(300000)
      builder.putTag(PriceQualityWorkerYdb.ExcellentPriceTag)
      builder.getOfferAutoruBuilder.setSellerType(SellerType.COMMERCIAL)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)

    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala

    assert(!tags.contains(PriceQualityWorkerYdb.ExcellentPriceTag))
    assert(tags.contains(PriceQualityWorkerYdb.GoodPriceTag))
  }

  ("already has good, no changes") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(300000)
      builder.putTag(PriceQualityWorkerYdb.GoodPriceTag)
      builder.getOfferAutoruBuilder.setSellerType(SellerType.COMMERCIAL)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)
    assert(worker.process(offer, None).updateOfferFunc.isEmpty)
  }

  ("change good to excellent") in new Fixture {

    val offer: OfferModel.Offer = {
      val builder = suitableOffer.toBuilder
      builder.getOfferAutoruBuilder.getPriceBuilder.setPrice(240000)
      builder.putTag(PriceQualityWorkerYdb.GoodPriceTag)
      builder.getOfferAutoruBuilder.setSellerType(SellerType.COMMERCIAL)
      builder.build()
    }

    assert(worker.shouldProcess(offer, None).shouldProcess)

    val tags = worker.process(offer, None).updateOfferFunc.get(offer).getTagList.asScala

    assert(!tags.contains(PriceQualityWorkerYdb.GoodPriceTag))
    assert(tags.contains(PriceQualityWorkerYdb.ExcellentPriceTag))
  }

}
