package ru.yandex.vertis.story.api.managers.xml

import org.mockito.Mockito.{never, times, verify}
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.baker.util.TracedUtils
import ru.yandex.vertis.baker.util.extdata.geo.RegionTree
import ru.yandex.vertis.baker.util.test.http.BaseSpec
import ru.yandex.vertis.feature.model.{Feature, FeatureRegistry}
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.story.api.components.features.{FeaturesAware, FeaturesManager}
import ru.yandex.vertis.story.api.managers.offer.{OffersByCategory, OffersManager}
import ru.yandex.vertis.story.api.storage.StoriesStorage
import ru.yandex.vertis.story.api.util.{BasicGenerators, TestOffer}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.util.concurrent.Threads
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}
import ru.yandex.vertis.story.api.managers.story.GeneratorStory.{serviceTypeAuction, serviceTypeOffer}
import ru.yandex.vertis.story.api.managers.story.{GeneratorStory, StoriesManager}
import ru.yandex.vertis.story.api.managers.story.StoriesManager.{idImage, idVideo, UnreportingAndroidVersion, UnreportingIosVersion}
import ru.yandex.vertis.story.api.model.Story.Image
import ru.yandex.vertis.story.api.model.{StoriesContainer, Story, StoryContent, StoryType}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.asJavaIterableConverter

class StoriesManagerSpec extends BaseSpec with MockitoSupport with ProducerProvider {

  abstract class Fixture(showUsersOfferInStory: Boolean = true, showAuctionOfferInStory: Boolean = true) {
    implicit val trace: Traced = TracedUtils.empty
    implicit val ec: ExecutionContext = Threads.SameThreadEc

    val allStoriesStorage: StoriesStorage = mock[StoriesStorage]
    val orderedStoriesStorage: StoriesStorage = mock[StoriesStorage]
    val offersManager: OffersManager = mock[OffersManager]
    val regionTree: RegionTree = mock[RegionTree]
    val featuresAware: FeaturesAware = mock[FeaturesAware]
    val featuresManager: FeaturesManager = mock[FeaturesManager]
    val featuresRegistry: FeatureRegistry = mock[FeatureRegistry]
    val showUsersOfferInStoryFeature: Feature[Boolean] = mock[Feature[Boolean]]
    val showAuctionOfferInStoryFeature: Feature[Boolean] = mock[Feature[Boolean]]
    val userIdWithNonEmptyResultOfferUserAndEmptyResultAuction: String = BasicGenerators.readableString.next
    val userIdWithEmptyResultOfferUserAndNonEmptyResultAuction: String = BasicGenerators.readableString.next
    val userIdWithNonEmptyResultAuction: String = BasicGenerators.readableString.next
    val userIdWithEmptyResultAuction: String = BasicGenerators.readableString.next

    val generatorStory: GeneratorStory = new GeneratorStory(offersManager, "autoru.api.host")

    val offer: ApiOfferModel.Offer =
      TestOffer.createTestOffer("user:" ++ userIdWithNonEmptyResultOfferUserAndEmptyResultAuction, Category.CARS, "")

    val auctionTags = Seq("available_for_c2b_auction")

    val auctionOffer: ApiOfferModel.Offer =
      TestOffer
        .createTestOffer("user:" ++ userIdWithEmptyResultOfferUserAndNonEmptyResultAuction, Category.CARS, "")
        .toBuilder
        .addAllTags(auctionTags.asJava)
        .build()

    val offers: OffersByCategory = OffersByCategory(Some(offer), None, None)
    val auctionOffers: OffersByCategory = OffersByCategory(Some(auctionOffer), None, None)
    val emptyOffers: OffersByCategory = OffersByCategory(None, None, None)

    when(offersManager.getRandomOffersByCategory(?, ?, ?)(?)).thenReturn(Future.successful(offers))
    when(offersManager.getOfferId(?, ?)(?)).thenReturn(Future.successful(Some(offer)))
    val offerStoryTitle = "Смотрите, какой автомобиль!"
    when(offersManager.getPreviewBytes(?, eeq(StoryType.Offer))).thenReturn(offerStoryTitle)
    val auctionStoryTitle = "Оставить заявку"
    when(offersManager.getPreviewBytes(?, eeq(StoryType.Auction))).thenReturn(auctionStoryTitle)
    when(offersManager.getTitleAndText(?, ?))
      .thenReturn(
        StoryContent(
          "title",
          Some("price"),
          Some("textMileage"),
          "textInfoObject",
          Some("textTransmission"),
          "buttonText"
        )
      )
    when(featuresManager.ShowUsersOfferInStory).thenReturn(showUsersOfferInStoryFeature)
    when(showUsersOfferInStoryFeature.value).thenReturn(showUsersOfferInStory)
    when(featuresManager.ShowAuctionOfferInStory).thenReturn(showAuctionOfferInStoryFeature)
    when(showAuctionOfferInStoryFeature.value).thenReturn(showAuctionOfferInStory)
    when(
      offersManager.getOffersByUserId(
        ?,
        eeq(userIdWithNonEmptyResultOfferUserAndEmptyResultAuction),
        eeq(Some(serviceTypeOffer)),
        ?
      )(?)
    ).thenReturn(Future.successful(offers))
    when(
      offersManager.getOffersByUserId(
        ?,
        eeq(userIdWithEmptyResultOfferUserAndNonEmptyResultAuction),
        eeq(Some(serviceTypeOffer)),
        ?
      )(?)
    ).thenReturn(Future.successful(emptyOffers))
    when(
      offersManager.getOffersByUserId(
        ?,
        eeq(userIdWithEmptyResultOfferUserAndNonEmptyResultAuction),
        eeq(None),
        eeq(Some(List.empty))
      )(?)
    ).thenReturn(Future.successful(emptyOffers))
    when(
      offersManager.getOffersByUserId(
        ?,
        eeq(userIdWithNonEmptyResultOfferUserAndEmptyResultAuction),
        eeq(None),
        eeq(Some(auctionTags))
      )(?)
    ).thenReturn(Future.successful(emptyOffers))
    when(
      offersManager.getOffersByUserId(
        ?,
        eeq(userIdWithNonEmptyResultOfferUserAndEmptyResultAuction),
        eeq(None),
        eeq(Some(List.empty))
      )(?)
    ).thenReturn(Future.successful(emptyOffers))
    when(
      offersManager.getOffersByUserId(
        ?,
        eeq(userIdWithEmptyResultOfferUserAndNonEmptyResultAuction),
        eeq(None),
        eeq(Some(auctionTags))
      )(?)
    ).thenReturn(Future.successful(auctionOffers))
    when(offersManager.getOffersByUserId(?, eeq(userIdWithEmptyResultAuction), eeq(Some(serviceTypeAuction)), ?)(?))
      .thenReturn(Future.successful(emptyOffers))
    when(offersManager.getOffersByUserId(?, eeq(userIdWithNonEmptyResultAuction), eeq(Some(serviceTypeAuction)), ?)(?))
      .thenReturn(Future.successful(offers))
    when(offersManager.getOffersByUserId(?, eeq(userIdWithNonEmptyResultAuction), eeq(Some(serviceTypeOffer)), ?)(?))
      .thenReturn(Future.successful(offers))
    when(offersManager.getOffersByUserId(?, eeq(userIdWithNonEmptyResultAuction), eeq(None), ?)(?))
      .thenReturn(Future.successful(offers))
    when(offersManager.getOffersByUserId(?, eeq(userIdWithEmptyResultAuction), eeq(Some(serviceTypeOffer)), ?)(?))
      .thenReturn(Future.successful(offers))

    val firstStory: Story = Story(
      id = "1",
      version = 1,
      pages = 1,
      image = "https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyimgpreview@x3.jpg",
      imageFull = "https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyimgpreview@x3.jpg",
      imagePreview = Some(
        "aHR0cHM6Ly95YXN0YXRpYy5uZXQvczMvdmVydGlzLWZyb250ZW5kL2F1dG9ydS1mcm9udGVuZC90ZW1wL3N0b3J5aW1ncHJldmlld0B4My5qcGc="
      ),
      image_sizes = Some(
        Image(
          Some(
            "aHR0cHM6Ly95YXN0YXRpYy5uZXQvczMvdmVydGlzLWZyb250ZW5kL2F1dG9ydS1mcm9udGVuZC90ZW1wL3N0b3J5aW1ncHJldmlld0B4My5qcGc="
          ),
          Some("https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyimgpreview@x1.jpg"),
          Some("https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyimgpreview@x2.jpg"),
          Some("https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyimgpreview@x3.jpg")
        )
      ),
      image_full_sizes = Some(
        Image(
          Some(
            "aHR0cHM6Ly95YXN0YXRpYy5uZXQvczMvdmVydGlzLWZyb250ZW5kL2F1dG9ydS1mcm9udGVuZC90ZW1wL3N0b3J5aW1ncHJldmlld0B4My5qcGc="
          ),
          Some("https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyvideotest@1x.jpg"),
          Some("https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyvideotest@2x.jpg"),
          Some("https://yastatic.net/s3/vertis-frontend/autoru-frontend/temp/storyvideotest@3x.jpg")
        )
      ),
      nativeStory = "autoru.api.host/1.0/story/test_video/xml?token=story-xml",
      jsonStory = None,
      raw = None,
      background = "#ffffff",
      text = "#ffffff",
      title = "Российские новинки недели",
      geo = None,
      tags = None,
      card_id = None,
      card_category = None,
      `x-android-app-version` = None,
      `x-ios-app-version` = None,
      `x-ios-app-version-to` = None,
      `x-android-app-version-to` = None
    )

    private def storySequence: Seq[Story] = {
      val secondStory = firstStory.copy(id = "2")
      val thirdStory = firstStory.copy(id = "3")
      Seq(firstStory, secondStory, thirdStory)
    }

    val storiesManager = {
      new StoriesManager(
        regionTree,
        allStoriesStorage,
        orderedStoriesStorage,
        offersManager,
        generatorStory,
        "autoru.api.host",
        "autoru.host",
        "autoru.buyout-path"
      ) {
        override def features: FeaturesManager = featuresManager
        override def featureRegistry: FeatureRegistry = featuresRegistry
      }
    }
    when(allStoriesStorage.get(?, ?)).thenReturn(Seq.empty)
    when(allStoriesStorage.search(?, ?, ?, ?)).thenReturn(storySequence)
    when(allStoriesStorage.getBasePath).thenReturn("/tmp/")
    when(allStoriesStorage.get("1", true)).thenReturn(Seq(storySequence.head))
    when(allStoriesStorage.get("2", true)).thenReturn(Seq(storySequence(1)))
    when(allStoriesStorage.get("3", true)).thenReturn(Seq(storySequence(2)))
    when(allStoriesStorage.get("1", false)).thenReturn(Seq(storySequence.head))
    when(allStoriesStorage.get("2", false)).thenReturn(Seq(storySequence(1)))
    when(allStoriesStorage.get("3", false)).thenReturn(Seq(storySequence(2)))

    when(orderedStoriesStorage.search(?, ?, ?, ?)).thenReturn(storySequence)
    when(orderedStoriesStorage.getBasePath).thenReturn("/tmp/")
    when(orderedStoriesStorage.get("1", withRaw = true)).thenReturn(Seq(storySequence.head))
    when(orderedStoriesStorage.get("2", withRaw = true)).thenReturn(Seq(storySequence(1)))
    when(orderedStoriesStorage.get("3", withRaw = true)).thenReturn(Seq(storySequence(2)))
    when(orderedStoriesStorage.get("1", withRaw = false)).thenReturn(Seq(storySequence.head))
    when(orderedStoriesStorage.get("2", withRaw = false)).thenReturn(Seq(storySequence(1)))
    when(orderedStoriesStorage.get("3", withRaw = false)).thenReturn(Seq(storySequence(2)))
  }

  "StoriesManager.search" should {

    "return stories with offer for user" when {
      "userId Empty" in new Fixture() {
        val storiesContainer: StoriesContainer = storiesManager
          .search(
            regionId = None,
            tags = Seq.empty,
            androidVersion = Some(UnreportingAndroidVersion),
            iosVersion = Some(UnreportingIosVersion),
            excludeOfferId = Set.empty,
            category = Set(Category.CARS, Category.TRUCKS, Category.MOTO),
            userId = None,
            showExperiment = Some("IMAGE")
          )
          .futureValue
        verify(offersManager, never()).getOffersByUserId(?, ?, ?, ?)(?)
        verify(offersManager, times(1)).getRandomOffersByCategory(?, ?, ?)(?)
        storiesContainer.stories.head.id shouldBe idImage
      }

      "userId not empty and offer exists" in new Fixture() {
        val storiesContainer: StoriesContainer = storiesManager
          .search(
            regionId = None,
            tags = Seq.empty,
            androidVersion = None,
            iosVersion = None,
            excludeOfferId = Set.empty,
            category = Set(Category.CARS, Category.TRUCKS, Category.MOTO),
            userId = Some(userIdWithNonEmptyResultOfferUserAndEmptyResultAuction),
            showExperiment = Some("VIDEO")
          )
          .futureValue

        verify(offersManager, times(2)).getOffersByUserId(?, ?, ?, ?)(?)
        verify(offersManager, never()).getRandomOffersByCategory(?, ?, ?)(?)
        storiesContainer.stories.head.id shouldBe idVideo
        val offerStory = storiesContainer.stories(2)
        offerStory.id shouldBe "offer_" + offer.getId
          .split("-")
          .head + "_" + offer.getCategory + "_" + 1
        offerStory.title shouldBe offerStoryTitle
      }

      "userId not empty and offer exists for auction" in new Fixture() {
        val storiesContainer: StoriesContainer = storiesManager
          .search(
            regionId = None,
            tags = Seq.empty,
            androidVersion = None,
            iosVersion = None,
            excludeOfferId = Set.empty,
            category = Set(Category.CARS, Category.TRUCKS, Category.MOTO),
            userId = Some(userIdWithEmptyResultOfferUserAndNonEmptyResultAuction),
            showExperiment = None
          )
          .futureValue

        verify(offersManager, times(2)).getOffersByUserId(?, ?, ?, ?)(?)
        verify(offersManager, times(1)).getRandomOffersByCategory(?, ?, ?)(?)
        val auctionStory = storiesContainer.stories(1)
        auctionStory.id shouldBe "auction_" + auctionOffer.getId
          .split("-")
          .head + "_" + auctionOffer.getCategory + "_" + 1
        auctionStory.tags.map(_.map(_.name)) shouldBe Some(auctionTags)
        auctionStory.title shouldBe auctionStoryTitle
      }

      "userId not empty and offer not exists" in new Fixture() {
        val storiesContainer: StoriesContainer = storiesManager
          .search(
            regionId = None,
            tags = Seq.empty,
            androidVersion = None,
            iosVersion = None,
            excludeOfferId = Set.empty,
            category = Set(Category.CARS, Category.TRUCKS, Category.MOTO),
            userId = Some(userIdWithEmptyResultOfferUserAndNonEmptyResultAuction),
            showExperiment = None
          )
          .futureValue

        verify(offersManager, times(2)).getOffersByUserId(?, ?, ?, ?)(?)
        verify(offersManager, times(1)).getRandomOffersByCategory(?, ?, ?)(?)
        storiesContainer.stories(1).id shouldBe "auction_" + auctionOffer.getId
          .split("-")
          .head + "_" + auctionOffer.getCategory + "_" + 1
      }

      "userId not empty and offer auction not exists" in new Fixture() {
        val storiesContainer: StoriesContainer = storiesManager
          .search(
            regionId = None,
            tags = Seq.empty,
            androidVersion = None,
            iosVersion = None,
            excludeOfferId = Set.empty,
            category = Set(Category.CARS, Category.TRUCKS, Category.MOTO),
            userId = Some(userIdWithNonEmptyResultAuction),
            showExperiment = None
          )
          .futureValue

        verify(offersManager, times(2)).getOffersByUserId(?, ?, ?, ?)(?)
        verify(offersManager, times(1)).getRandomOffersByCategory(?, ?, ?)(?)
        storiesContainer.stories(1).id shouldBe "offer_" + offer.getId
          .split("-")
          .head + "_" + offer.getCategory + "_" + 2
      }

      "userId not empty and stories in right order" in new Fixture() {
        val storiesContainer: StoriesContainer = storiesManager
          .search(
            regionId = None,
            tags = Seq.empty,
            androidVersion = None,
            iosVersion = None,
            excludeOfferId = Set.empty,
            category = Set(Category.CARS, Category.TRUCKS, Category.MOTO),
            userId = Some(userIdWithNonEmptyResultOfferUserAndEmptyResultAuction),
            showExperiment = Some("VIDEO")
          )
          .futureValue

        verify(orderedStoriesStorage).search(?, ?, ?, ?)
        verify(allStoriesStorage, never()).search(?, ?, ?, ?)

        val expectedOfferStoryId: String = "offer_" + offer.getId
          .split("-")
          .head + "_" + offer.getCategory + "_" + 1

        val expectedOrderOfIds: Seq[String] = Seq(
          idVideo,
          "1",
          expectedOfferStoryId,
          "2",
          "3"
        )

        storiesContainer.stories.map(_.id) shouldBe expectedOrderOfIds
      }
    }
  }

  "StoriesManager.get" should {
    "get story when raw = false and story_id = test_video" in new Fixture() {
      val story: Story = storiesManager
        .get(id = "test_video", withRaw = false)
        .futureValue
        .stories
        .head

      story.id shouldBe "test_video"
      story.raw shouldBe None
    }

    "get story when raw = false and story_id = test_image" in new Fixture() {
      val story: Story = storiesManager
        .get(id = "test_image", withRaw = false)
        .futureValue
        .stories
        .head

      story.id shouldBe "test_image"
      story.raw shouldBe None
    }

    "get story when raw = false and story_id = offer_id" in new Fixture() {
      val story: Story = storiesManager
        .get(id = "offer_" + offer.getId.split("-").head + "_" + offer.getCategory + "_" + 1, withRaw = false)
        .futureValue
        .stories
        .head

      story.id shouldBe "offer_" + offer.getId.split("-").head + "_" + offer.getCategory + "_" + 1
      story.raw shouldBe None
    }

    "get story when raw = false and story_id = offer_id_auction" in new Fixture() {
      val story = storiesManager
        .get(id = "auction_" + offer.getId.split("-").head + "_" + offer.getCategory + "_" + 1, withRaw = false)
        .futureValue
        .stories
        .head

      story.id shouldBe "auction_" + offer.getId.split("-").head + "_" + offer.getCategory + "_" + 1
      story.raw shouldBe None
    }

    "random_story from allStoriesStorage" in new Fixture() {
      val story: StoriesContainer = storiesManager
        .get(id = "random_story", withRaw = false)
        .futureValue

      story.stories shouldBe Seq.empty
    }

    "get story when raw = true and story_id = test_video" in new Fixture() {
      val story: Story = storiesManager
        .get(id = "test_video", withRaw = true)
        .futureValue
        .stories
        .head

      story.id shouldBe "test_video"
      story.raw.isEmpty shouldBe false
    }

    "get story when raw = true and story_id = test_image" in new Fixture() {
      val story: Story = storiesManager
        .get(id = "test_image", withRaw = true)
        .futureValue
        .stories
        .head

      story.id shouldBe "test_image"
      story.raw.isEmpty shouldBe false
    }

    "get story when raw = true and story_id = offer_id" in new Fixture() {
      val story: Story = storiesManager
        .get(id = "offer_" + offer.getId.split("-").head + "_" + offer.getCategory + "_" + 1, withRaw = true)
        .futureValue
        .stories
        .head

      story.id shouldBe
        "offer_" + offer.getId.split("-").head + "_" + offer.getCategory + "_" + 1
      story.raw.isEmpty shouldBe false
    }

    "get story when raw = true and story_id = random_story from allStoriesStorage" in new Fixture() {
      val story: Seq[Story] = storiesManager
        .get(id = "random_story", withRaw = true)
        .futureValue
        .stories

      story shouldBe Seq.empty
    }

    "get story with story_id = 1 from allStoriesStorage and withRaw = true" in new Fixture() {
      val story: Seq[Story] = storiesManager
        .get(id = "1", withRaw = true)
        .futureValue
        .stories

      verify(orderedStoriesStorage, never()).get(?, ?)
      verify(allStoriesStorage).get("1", true)
      story.head.id shouldBe "1"
    }

    "get story with existing story_id = 2 from allStoriesStorage and withRaw = false" in new Fixture() {
      val story: Seq[Story] = storiesManager
        .get(id = "2", withRaw = false)
        .futureValue
        .stories

      verify(orderedStoriesStorage, never()).get(?, ?)
      verify(allStoriesStorage).get("2", false)
      story.head.id shouldBe "2"
    }
  }
}
