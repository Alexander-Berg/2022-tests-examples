package ru.yandex.auto.extdata.jobs.feeds.feed.writers.cars

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.AutoSchemaVersions
import ru.yandex.auto.core.model.enums.State.Search
import ru.yandex.auto.extdata.jobs.feeds.feed.FeedProperties._
import ru.yandex.auto.extdata.jobs.feeds.feed.LandingUrlType
import ru.yandex.auto.extdata.jobs.feeds.feed.writers.cars.utils.FeedCarUrlUtils
import ru.yandex.auto.message.CarAdSchema.CarAdMessage

@RunWith(classOf[JUnitRunner])
class FeedCarUrlUtilSpec extends WordSpec with Matchers {

  private val mark = "audi"
  private val model = "a7"
  private val alternativeModel = "a8"
  private val generation = 2873623L
  private val alternativeGeneration = 21040120L
  private val state = Search.NEW.name()
  private val offerId = "12345"
  private val alternativeOfferId = "1103030990"
  private val offerHash = "isjdb3d"
  private val alternativeOfferHash = "c26d7555"
  private val moskva = "moskva"

  private val carAd = CarAdMessage
    .newBuilder()
    .setVersion(AutoSchemaVersions.CAR_AD_VERSION)
    .setMark(mark)
    .setRegionCode("213")
    .setModel(model)
    .addSuperGenerations(generation)
    .setId(s"autoru-$offerId")
    .setAutoruHashCode(offerHash)
    .setSearchState(state)
    .build()

  private val similarCarAd = CarAdMessage
    .newBuilder()
    .setVersion(AutoSchemaVersions.CAR_AD_VERSION)
    .setMark(mark)
    .setModel(alternativeModel)
    .addSuperGenerations(alternativeGeneration)
    .setId(s"autoru-$alternativeOfferId")
    .setAutoruHashCode(alternativeOfferHash)
    .setSearchState(state)
    .build()

  private val baseUrlPrefix = "https://auto.ru/cars"
  private val baseMoskvaUrlPrefix = "https://auto.ru/moskva/cars"

  private val OfferUrl = s"$baseUrlPrefix/$state/sale/$mark/$model/$offerId-$offerHash/".toLowerCase

  private def listingUrl(
      withoutGeneration: Boolean,
      withPinned: Boolean,
      forceListing: Boolean,
      isCommercialSeller: Boolean
  ): String = {
    val generationPart = Some(s"$generation/").filterNot(_ => withoutGeneration).getOrElse("")
    val pinned = Some(s"?pinned_offer_id=autoru-$offerId").filter(_ => withPinned).getOrElse("")
    val listing = if (forceListing) {
      (if (pinned.isEmpty) "?" else "&") + "output_type=list"
    } else ""

    val seller =
      if (isCommercialSeller)
        "seller_group=COMMERCIAL"
      else ""

    s"$baseUrlPrefix/$mark/$model/$generationPart$state/$pinned$listing$seller".toLowerCase
  }

  "FeedCarUrlUtil" should {
    "correctly generate offer url" in {
      FeedCarUrlUtils.generateFeedUrl(carAd, Seq.empty, OfferUrlDirectly) shouldBe OfferUrl
    }

    "correctly generate offer url with similars" in {
      FeedCarUrlUtils.generateFeedUrl(
        carAd,
        Seq(similarCarAd),
        ListingUrl(
          listingWithoutGeneration = false,
          pinOffer = false,
          forceListing = false,
          LandingUrlType.UsedMarkModelListingWithRelevantCars,
          isCommercialSellerGroup = false,
          shouldAddRegion = true
        )
      ) shouldBe s"$baseMoskvaUrlPrefix/$mark/$model/$state/?catalog_filter=mark=$mark,model=$alternativeModel".toLowerCase
    }

    "correctly listing with pinned url" in {
      Seq(true, false).foreach { withoutGeneration =>
        FeedCarUrlUtils.generateFeedUrl(
          carAd,
          Seq.empty,
          ListingUrl(
            withoutGeneration,
            pinOffer = true,
            forceListing = false,
            LandingUrlType.Non,
            isCommercialSellerGroup = false,
            shouldAddRegion = false
          )
        ) shouldBe listingUrl(
          withoutGeneration,
          withPinned = true,
          forceListing = false,
          isCommercialSeller = false
        )
      }
    }

    "correctly listing without pinned url" in {
      Seq(true, false).foreach { withoutGeneration =>
        FeedCarUrlUtils.generateFeedUrl(
          carAd,
          Seq.empty,
          ListingUrl(
            withoutGeneration,
            pinOffer = false,
            forceListing = false,
            LandingUrlType.Non,
            isCommercialSellerGroup = false,
            shouldAddRegion = false
          )
        ) shouldBe listingUrl(
          withoutGeneration,
          withPinned = false,
          forceListing = false,
          isCommercialSeller = false
        )
      }
    }

    "correctly listing with force listing" in {
      Seq(true, false).foreach { withoutGeneration =>
        FeedCarUrlUtils.generateFeedUrl(
          carAd,
          Seq.empty,
          ListingUrl(
            withoutGeneration,
            pinOffer = true,
            forceListing = true,
            LandingUrlType.Non,
            isCommercialSellerGroup = false,
            shouldAddRegion = false
          )
        ) shouldBe listingUrl(
          withoutGeneration,
          withPinned = true,
          forceListing = true,
          isCommercialSeller = false
        )
      }
    }
  }

}
