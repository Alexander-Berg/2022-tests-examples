package ru.auto.api.managers.enrich.enrichers

import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Section
import ru.auto.api.CommonModel.{DiscountOptions, GeoPoint}
import ru.auto.api.auth.Application
import ru.auto.api.managers.enrich.EnrichOptions
import ru.auto.api.managers.enrich.enrichers.DealerSearchTags.{ChatsEnabled, HasDiscountOptions, MinDistanceKm, NearToYou}
import ru.auto.api.managers.enrich.enrichers.OfferDistanceEnricher._
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.{RequestParams, UserLocation, UserRef, Version}
import ru.auto.api.testkit.TestData
import ru.auto.api.util.RequestImpl
import ru.auto.api.{ApiOfferModel, BaseSpec}
import ru.yandex.vertis.mockito.MockitoSupport

class SearchTagsForDealerEnricherSpec
  extends BaseSpec
  with MockitoSupport
  with ScalaCheckPropertyChecks
  with OptionValues {

  private val geoTree = TestData.tree
  private val enricher = new SearchTagsForDealerEnricher(geoTree)

  "SearchTagsForDealerEnricher" should {
    "add near_to_you tag if location less then 30 km" in {
      val offer = {
        val b = offerGen(RegisteredUserRefGen, Gen.const(List())).next.toBuilder
        b.setUserRef(DealerUserRefGen.next.toString)
        b.setSection(Section.NEW)
        val s = b.getSeller.toBuilder
        s.setChatsEnabled(false)
        b.setSeller(s)
        b.build()
      }
      val offerLocation = getOfferLocation(offer).get
      val userLocation = {
        UserLocation(offerLocation.getLatitude.toFloat, offerLocation.getLongitude.toFloat, 15)
      }
      val enrich =
        enricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              searchTagsForDealer = true
            )
          )(createRequest(offer.userRef, Some(userLocation)))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getTagsCount shouldBe 1
      enrichedOffer.getTags(0) shouldBe NearToYou
    }

    "skip processing near_to_you tag if location more then 30 km" in {
      val offer = {
        val b = offerGen(RegisteredUserRefGen, Gen.const(List())).next.toBuilder
        b.setUserRef(DealerUserRefGen.next.toString)
        b.setSection(Section.NEW)
        val s = b.getSeller.toBuilder
        s.setChatsEnabled(false)
        b.setSeller(s)
        b.build()
      }
      val offerLocation = getOfferLocation(offer).get
      val userLocation = {
        // 10' is ~18.53km
        val distanceShift = MinDistanceKm + 10
        UserLocation(
          (offerLocation.getLatitude + distanceShift / (18.53 * 6)).toFloat,
          offerLocation.getLongitude.toFloat,
          15
        )
      }
      val enrich =
        enricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              searchTagsForDealer = true
            )
          )(createRequest(offer.userRef, Some(userLocation)))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getTagsCount shouldBe 0
    }

    "add chats_enabled tag if offer.getSeller.getChatsEnabled is true" in {
      val offer = {
        val b = offerGen(RegisteredUserRefGen, Gen.const(List())).next.toBuilder
        b.setUserRef(DealerUserRefGen.next.toString)
        b.setSection(Section.NEW)
        val s = b.getSeller.toBuilder
        s.setChatsEnabled(true)
        b.setSeller(s)
        b.build()
      }
      val offerLocation = getOfferLocation(offer).get
      val userLocation = {
        // 10' is ~18.53km
        val distanceShift = MinDistanceKm + 10
        UserLocation(
          (offerLocation.getLatitude + distanceShift / (18.53 * 6)).toFloat,
          offerLocation.getLongitude.toFloat,
          15
        )
      }
      val enrich =
        enricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              searchTagsForDealer = true
            )
          )(createRequest(offer.userRef, Some(userLocation)))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getTagsCount shouldBe 1
      enrichedOffer.getTags(0) shouldBe ChatsEnabled
    }

    "not add has_discount_options tag if offer has discount options but they are empty and section has USED value" in {
      val offer = {
        val b = offerGen(RegisteredUserRefGen, Gen.const(List())).next.toBuilder
        b.setUserRef(DealerUserRefGen.next.toString)
        b.setSection(Section.USED)
        val s = b.getSeller.toBuilder
        s.setChatsEnabled(false)
        b.setSeller(s)
        val d = DiscountOptions.newBuilder()
        b.setDiscountOptions(d)
        b.build()
      }
      val offerLocation = getOfferLocation(offer).get
      val userLocation = {
        // 10' is ~18.53km
        val distanceShift = MinDistanceKm + 10
        UserLocation(
          (offerLocation.getLatitude + distanceShift / (18.53 * 6)).toFloat,
          offerLocation.getLongitude.toFloat,
          15
        )
      }
      val enrich =
        enricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              searchTagsForDealer = true
            )
          )(createRequest(offer.userRef, Some(userLocation)))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getTagsCount shouldBe 0
    }

    "add has_discount_options tag if offer has discount options and section has USED value" in {
      val offer = {
        val b = offerGen(RegisteredUserRefGen, Gen.const(List())).next.toBuilder
        b.setUserRef(DealerUserRefGen.next.toString)
        b.setSection(Section.USED)
        val s = b.getSeller.toBuilder
        s.setChatsEnabled(false)
        b.setSeller(s)
        val d = DiscountOptions
          .newBuilder()
          .setCredit(10)
          .setInsurance(5)
          .setMaxDiscount(1)
        b.setDiscountOptions(d)
        b.build()
      }
      val offerLocation = getOfferLocation(offer).get
      val userLocation = {
        // 10' is ~18.53km
        val distanceShift = MinDistanceKm + 10
        UserLocation(
          (offerLocation.getLatitude + distanceShift / (18.53 * 6)).toFloat,
          offerLocation.getLongitude.toFloat,
          15
        )
      }
      val enrich =
        enricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              searchTagsForDealer = true
            )
          )(createRequest(offer.userRef, Some(userLocation)))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getTagsCount shouldBe 1
      enrichedOffer.getTags(0) shouldBe HasDiscountOptions
    }

    "offer is not enriched with has_discount_options tag in case of it has no discount options but section has USED value" in {
      val offer = {
        val b = offerGen(RegisteredUserRefGen, Gen.const(List())).next.toBuilder
        b.setUserRef(DealerUserRefGen.next.toString)
        b.setSection(Section.USED)
        val s = b.getSeller.toBuilder
        s.setChatsEnabled(false)
        b.setSeller(s)
        b.build()
      }
      val offerLocation = getOfferLocation(offer).get
      val userLocation = {
        // 10' is ~18.53km
        val distanceShift = MinDistanceKm + 10
        UserLocation(
          (offerLocation.getLatitude + distanceShift / (18.53 * 6)).toFloat,
          offerLocation.getLongitude.toFloat,
          15
        )
      }
      val enrich =
        enricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              searchTagsForDealer = true
            )
          )(createRequest(offer.userRef, Some(userLocation)))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getTagsCount shouldBe 0
    }

    "offer is not enriched for non dealer user ref" in {
      val offer = {
        val b = offerGen(RegisteredUserRefGen, Gen.const(List())).next.toBuilder
        b.setUserRef(AnonymousUserRefGen.next.toString)
        b.setSection(Section.USED)
        val s = b.getSeller.toBuilder
        s.setChatsEnabled(true)
        b.setSeller(s)
        b.build()
      }
      val offerLocation = getOfferLocation(offer).get
      val userLocation = {
        UserLocation(offerLocation.getLatitude.toFloat, offerLocation.getLongitude.toFloat, 15)
      }
      val enrich =
        enricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              searchTagsForDealer = true
            )
          )(createRequest(offer.userRef, Some(userLocation)))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getTagsCount shouldBe 0
    }
  }

  private def createRequest(userRef: UserRef, userLocation: Option[UserLocation] = None) = {
    val r = new RequestImpl
    r.setApplication(Application.iosApp)
    r.setVersion(Version.V1_0)
    r.setRequestParams(RequestParams.construct("1.1.1.1", userLocation = userLocation))
    r.setUser(userRef)
    r.named("test")
    r
  }

  private def getOfferLocation(offer: ApiOfferModel.Offer): Option[GeoPoint] = {
    val geoId = offer.getSeller.getLocation.getGeobaseId
    val region = geoTree.region(geoId)
    region.map(r => getGeoPoint(r))
  }
}
