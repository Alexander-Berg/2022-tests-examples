package ru.yandex.auto.extdata.jobs.feeds.feed

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.extdata.jobs.feeds.feed.FeedProperties.{ListingUrl, OfferUrlDirectly}
import ru.yandex.auto.extdata.jobs.feeds.feed.writers.cars.utils.FeedCarUrlUtils
import ru.yandex.auto.message.CarAdSchema.CarAdMessage
import ru.yandex.auto.message.FeedConstructorSchema.FeedConstructorTermHolderMessage
import ru.yandex.vertis.mockito.MockitoSupport

@RunWith(classOf[JUnitRunner])
class FeedPropertiesSpec extends WordSpec with Matchers with MockitoSupport {

  private case class UrlSpec(
      withoutGeneration: Option[Boolean],
      offerCard: Option[Boolean],
      forceListing: Option[Boolean]
  ) {

    def applyTo(builder: FeedConstructorTermHolderMessage.Builder): FeedConstructorTermHolderMessage.Builder = {
      withoutGeneration.map(builder.setListingWithoutGeneration)
      offerCard.map(builder.setShouldLinkOfferCard)
      forceListing.map(builder.setNewGenerationToListing)

      builder
    }
  }

  private val carAdDummy = CarAdMessage
    .newBuilder()
    .setVersion(1)
    .setId("12345678")
    .setMark("kia")
    .setModel("rio")
    .setAutoruHashCode("ololo")
    .setSearchState("new")
    .build()

  "FeedProperties.build" should {
    "correctly decide url prop" in {
      val specs = Map(
        UrlSpec(withoutGeneration = None, offerCard = None, forceListing = None) -> ListingUrl(
          listingWithoutGeneration = false,
          pinOffer = true,
          forceListing = false,
          LandingUrlType.Non,
          isCommercialSellerGroup = false,
          shouldAddRegion = false
        ),
        UrlSpec(withoutGeneration = Some(false), offerCard = None, forceListing = None) -> ListingUrl(
          listingWithoutGeneration = false,
          pinOffer = true,
          forceListing = false,
          LandingUrlType.Non,
          isCommercialSellerGroup = false,
          shouldAddRegion = false
        ),
        UrlSpec(withoutGeneration = Some(true), offerCard = None, forceListing = None) -> ListingUrl(
          listingWithoutGeneration = true,
          pinOffer = true,
          forceListing = false,
          LandingUrlType.Non,
          isCommercialSellerGroup = false,
          shouldAddRegion = false
        ),
        UrlSpec(withoutGeneration = None, offerCard = Some(false), forceListing = None) -> ListingUrl(
          listingWithoutGeneration = false,
          pinOffer = true,
          forceListing = false,
          LandingUrlType.Non,
          isCommercialSellerGroup = false,
          shouldAddRegion = false
        ),
        UrlSpec(withoutGeneration = Some(false), offerCard = Some(false), forceListing = None) -> ListingUrl(
          listingWithoutGeneration = false,
          pinOffer = true,
          forceListing = false,
          LandingUrlType.Non,
          isCommercialSellerGroup = false,
          shouldAddRegion = false
        ),
        UrlSpec(withoutGeneration = Some(true), offerCard = Some(false), forceListing = None) -> ListingUrl(
          listingWithoutGeneration = true,
          pinOffer = true,
          forceListing = false,
          LandingUrlType.Non,
          isCommercialSellerGroup = false,
          shouldAddRegion = false
        ),
        UrlSpec(withoutGeneration = None, offerCard = Some(true), forceListing = None) -> OfferUrlDirectly,
        UrlSpec(withoutGeneration = Some(false), offerCard = Some(true), forceListing = None) -> OfferUrlDirectly,
        UrlSpec(withoutGeneration = Some(true), offerCard = Some(true), forceListing = None) -> OfferUrlDirectly,
        UrlSpec(withoutGeneration = None, offerCard = Some(false), forceListing = Some(true)) -> ListingUrl(
          listingWithoutGeneration = false,
          pinOffer = true,
          forceListing = true,
          LandingUrlType.Non,
          isCommercialSellerGroup = false,
          shouldAddRegion = false
        ),
        UrlSpec(withoutGeneration = Some(false), offerCard = Some(false), forceListing = Some(false)) -> ListingUrl(
          listingWithoutGeneration = false,
          pinOffer = true,
          forceListing = false,
          LandingUrlType.Non,
          isCommercialSellerGroup = false,
          shouldAddRegion = false
        ),
        UrlSpec(withoutGeneration = Some(true), offerCard = Some(false), forceListing = Some(false)) -> ListingUrl(
          listingWithoutGeneration = true,
          pinOffer = true,
          forceListing = false,
          LandingUrlType.Non,
          isCommercialSellerGroup = false,
          shouldAddRegion = false
        ),
        UrlSpec(withoutGeneration = Some(true), offerCard = Some(false), forceListing = Some(true)) -> ListingUrl(
          listingWithoutGeneration = true,
          pinOffer = true,
          forceListing = true,
          LandingUrlType.Non,
          isCommercialSellerGroup = false,
          shouldAddRegion = false
        )
      )

      println(specs.size)

      specs.foreach {
        case (spec, expected) =>
          val props =
            FeedProperties.build(spec.applyTo(FeedConstructorTermHolderMessage.newBuilder()).build(), Map.empty)

          val url = FeedCarUrlUtils.generateFeedUrl(carAdDummy, Seq.empty, props.feedUrlProp)

          println(s"Processing $spec")
          println(s"e: $expected, a: ${props.feedUrlProp}, eq: ${expected == props.feedUrlProp}")
          println(url)
          props.feedUrlProp shouldBe expected
          url.contains("output_type=list") shouldBe spec.forceListing.getOrElse(false)
      }
    }
  }

}
