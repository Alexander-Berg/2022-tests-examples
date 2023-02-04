package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import auto.c2b.common.Prices.PriceRange
import auto.c2b.reception.Api.CanApplyForAuctionResponse
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.Mockito.{times, verify, verifyNoMoreInteractions}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.worker.YdbWorkerResult
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.dao.proxy.{AdditionalDataForReading, AdditionalDataLoader}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.autoru360.{StoryPhotosClient, StoryPhotosGenerator}
import ru.yandex.vos2.autoru.services.c2b_reception_api.C2bAuctionClient
import ru.yandex.vos2.autoru.services.mds.MdsUploader
import ru.yandex.vos2.autoru.utils.converters.offerform.OfferFormConverter
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder
import ru.yandex.vos2.services.mds.{MdsPhotoData, MdsPhotoUtils}

@RunWith(classOf[JUnitRunner])
class C2bAuctionWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers {

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val c2bReceptionApi: C2bAuctionClient = mock[C2bAuctionClient]
    val storyPhotosClient: StoryPhotosClient = mock[StoryPhotosClient]
    val offerFormConverter: OfferFormConverter = mock[OfferFormConverter]
    val additionalDataLoader: AdditionalDataLoader = mock[AdditionalDataLoader]
    val mockFeaturesManager: FeaturesManager = mock[FeaturesManager]

    val mockFeature: Feature[Boolean] = mock[Feature[Boolean]]

    val mdsPhotoUtils: MdsPhotoUtils = mock[MdsPhotoUtils]
    val mdsUploader: MdsUploader = mock[MdsUploader]

    val apiOffer = ApiOfferModel.Offer.newBuilder().build()
    val mdsPhotoData = MdsPhotoData("namespace", "xxx-yyy")

    when(additionalDataLoader.loadAdditionalDataForReading(?[Offer])(?)).thenReturn(AdditionalDataForReading())
    when(offerFormConverter.convert(?, ?, ?, ?)(?)).thenReturn(apiOffer)
    when(storyPhotosClient.getPhotoForStory(?)(?)).thenReturn(Array.emptyByteArray)
    when(mdsUploader.putFromInputStream(?, ?, ?)(?)).thenReturn(mdsPhotoData)
    when(mockFeature.value).thenReturn(false)
    when(mockFeaturesManager.UpdateExistingAuctionPhoto).thenReturn(mockFeature)

    val storyPhotosGenerator: StoryPhotosGenerator = new StoryPhotosGenerator(
      mdsPhotoUtils,
      storyPhotosClient,
      mdsUploader
    )

    val worker: C2bAuctionWorkerYdb =
      new C2bAuctionWorkerYdb(c2bReceptionApi, storyPhotosGenerator, offerFormConverter, additionalDataLoader)
        with YdbWorkerTestImpl {
        override def features = mockFeaturesManager
      }

    private val testOffer = TestUtils.createOffer()
    val offer: OfferModel.Offer = testOffer.build()
    val notValidOfferDealer: OfferModel.Offer = TestUtils.createOffer(dealer = true).build()
    val notValidOfferCategory: OfferModel.Offer = TestUtils.createOffer(category = Category.MOTO).build()
  }

  "should NOT process, invalid offer dealer" in new Fixture {
    assert(!worker.shouldProcess(notValidOfferDealer, None).shouldProcess)
  }

  "should NOT process, invalid offer category" in new Fixture {
    assert(!worker.shouldProcess(notValidOfferCategory, None).shouldProcess)
  }

  "should process None" in new Fixture {
    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  "should not process till 5 day from now" in new Fixture {
    assert(!worker.shouldProcess(offer, Some(DateTime.now.plusDays(5).getMillis.toString)).shouldProcess)
  }

  "should process on previous day" in new Fixture {
    assert(worker.shouldProcess(offer, Some(DateTime.now.minusDays(1).getMillis.toString)).shouldProcess)
  }

  "processing offer put tag" in new Fixture {
    val priceRange: PriceRange.Builder = PriceRange.newBuilder.setFrom(800000).setTo(1200000)

    val canApplyForAuctionResponse: CanApplyForAuctionResponse =
      CanApplyForAuctionResponse.newBuilder.setCanApply(true).setPricePrediction(priceRange).build()
    when(c2bReceptionApi.canApply(?)(?)).thenReturn(canApplyForAuctionResponse)

    val result: YdbWorkerResult = worker.process(offer, None)
    val newOffer: OfferModel.Offer = result.updateOfferFunc.get(offer)
    newOffer.getTagList.contains(C2bAuctionWorkerYdb.Tag) shouldBe true
    newOffer.getOfferAutoru.getC2BAuctionInfo.getPriceRange.getTo shouldBe 1200000
    newOffer.getOfferAutoru.getC2BAuctionInfo.getStoryPhotos.hasPhoto shouldBe true
    newOffer.getOfferAutoru.getC2BAuctionInfo.getStoryPhotos.getPhoto.getName shouldBe mdsPhotoData.name
  }

  "processing offer clear tag" in new Fixture {
    val priceRange: PriceRange.Builder = PriceRange.newBuilder.setFrom(0).setTo(0)

    val canApplyForAuctionResponse: CanApplyForAuctionResponse =
      CanApplyForAuctionResponse.newBuilder.setCanApply(false).setPricePrediction(priceRange).build()
    when(c2bReceptionApi.canApply(?)(?)).thenReturn(canApplyForAuctionResponse)

    val offerWithTag = offer.toBuilder.putTag(C2bAuctionWorkerYdb.Tag).build()
    val result: YdbWorkerResult = worker.process(offerWithTag, None)
    val newOffer: OfferModel.Offer = result.updateOfferFunc.get(offerWithTag)
    newOffer.getTagList.contains(C2bAuctionWorkerYdb.Tag) shouldBe false
    newOffer.getOfferAutoru.getC2BAuctionInfo.getPriceRange.getTo shouldBe 0
    newOffer.getOfferAutoru.getC2BAuctionInfo.getStoryPhotos.hasPhoto shouldBe false
  }

  "processing offer without changes" in new Fixture {
    val priceRange: PriceRange.Builder = PriceRange.newBuilder.setFrom(800000).setTo(1200000)

    val canApplyForAuctionResponse: CanApplyForAuctionResponse =
      CanApplyForAuctionResponse.newBuilder.setCanApply(true).setPricePrediction(priceRange).build()
    when(c2bReceptionApi.canApply(?)(?)).thenReturn(canApplyForAuctionResponse)

    val result: YdbWorkerResult = worker.process(offer, None)

    verify(storyPhotosClient, times(2)).getPhotoForStory(?)(?)

    val newOffer: OfferModel.Offer = result.updateOfferFunc.get(offer)

    val newResult: YdbWorkerResult = worker.process(newOffer, None)

    assert(newResult.updateOfferFunc.isEmpty)
    verifyNoMoreInteractions(storyPhotosClient)
  }

  "processing offer without changes and update photo" in new Fixture {
    when(mockFeature.value).thenReturn(true)

    val priceRange: PriceRange.Builder = PriceRange.newBuilder.setFrom(800000).setTo(1200000)

    val canApplyForAuctionResponse: CanApplyForAuctionResponse =
      CanApplyForAuctionResponse.newBuilder.setCanApply(true).setPricePrediction(priceRange).build()
    when(c2bReceptionApi.canApply(?)(?)).thenReturn(canApplyForAuctionResponse)

    val result: YdbWorkerResult = worker.process(offer, None)
    val newOffer: OfferModel.Offer = result.updateOfferFunc.get(offer)

    val newResult: YdbWorkerResult = worker.process(newOffer, None)

    assert(newResult.updateOfferFunc.nonEmpty)
    verify(storyPhotosClient, times(4)).getPhotoForStory(?)(?)
  }
}
