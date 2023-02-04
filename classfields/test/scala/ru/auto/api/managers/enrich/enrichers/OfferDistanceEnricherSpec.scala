package ru.auto.api.managers.enrich.enrichers

import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.{ApiOfferModel, BaseSpec, CommonModel}
import ru.auto.api.auth.Application
import ru.auto.api.geo.GeoUtils.distance
import ru.auto.api.managers.enrich.EnrichOptions
import ru.auto.api.managers.enrich.enrichers.OfferDistanceEnricher._
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.searcher.OfferCardAdditionalParams
import ru.auto.api.model.{RequestParams, UserLocation, UserRef, Version}
import ru.auto.api.testkit.TestData
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

class OfferDistanceEnricherSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with OptionValues {

  private val geoTree = TestData.tree
  private val offerDistanceEnricher = new OfferDistanceEnricher(geoTree)

  "OfferDistanceEnricher" should {
    "not replace" in {
      val offer = OfferGen.next
      val offer2 = {
        val b = offer.toBuilder
        b.setDescription("text")
        b.build()
      }
      val userLocation = {
        val l = LocationGen.next
        val r = geoTree.region(l.getGeobaseId).value
        UserLocation(r.latitude.toFloat, r.longitude.toFloat, 65)
      }
      val enrich =
        offerDistanceEnricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              offerDistance = true
            )
          )(createRequest(offer.userRef, Some(userLocation)))
          .futureValue
      val enrichedOffer = enrich(offer2)
      enrichedOffer.getDescription shouldBe "text"
    }

    "not enrich with distance from offer seller location to near rid" in {
      val offer = OfferGen.next
      val offerCoord = getGeoPoint(geoTree.region(offer.getSeller.getLocation.getGeobaseId).value)
      val location = LocationGen.filter(isNear(offerCoord, _)).next
      val rid = location.getGeobaseId.toInt
      val enrich =
        offerDistanceEnricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              offerDistance = true,
              offerCardAdditionalParams = OfferCardAdditionalParams.empty.copy(geoIds = List(rid))
            )
          )(createRequest(offer.userRef))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getSeller.getLocation.getDistanceToSelectedGeoCount shouldBe 0
    }

    "enrich with distance from offer seller location to chief region" in {
      val federalSubject = Gen
        .oneOf(TestData.tree.regions.filter(_.isFederalSubject).filter(r => TestData.tree.isInside(r.id, Set(225))))
        .next
      val chiefRegion = TestData.tree.region(federalSubject.chiefRegion.value).value
      val chiefRegionCoord = getGeoPoint(chiefRegion)
      val offer = {
        val b = OfferGen.next.toBuilder
        b.getSellerBuilder.setLocation(LocationGen.filter(isFar(chiefRegionCoord, _)).next) // Тверь
        b.build()
      }
      val offerCoord = getGeoPoint(geoTree.region(offer.getSeller.getLocation.getGeobaseId).value)
      val rids = List(federalSubject.id.toInt) // Москва и Московская область
      val enrich =
        offerDistanceEnricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              offerDistance = true,
              offerCardAdditionalParams = OfferCardAdditionalParams.empty.copy(geoIds = rids)
            )
          )(createRequest(offer.userRef))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getSeller.getLocation.getDistanceToSelectedGeoCount shouldBe 1
      val distances = enrichedOffer.getSeller.getLocation.getDistanceToSelectedGeoList.asScala
      distances.map(_.getGeobaseId) should contain theSameElementsAs rids
      val regions = List(chiefRegion)
      distances.map(_.getCoord) should contain theSameElementsAs regions.map(getGeoPoint)
      distances.map(_.getDistance) should contain theSameElementsAs regions.map(r => {
        distance(getGeoPoint(r), offerCoord).toInt
      })
      enrichedOffer.getSeller.getLocation.hasDistanceToUser shouldBe false
    }

    "enrich with distance from offer seller location to presented rids" in {
      val n = Gen.choose(1, 5).next
      val offer = OfferGen.next
      val offerCoord = getGeoPoint(geoTree.region(offer.getSeller.getLocation.getGeobaseId).value)
      val locations = Gen.listOfN(n, LocationGen.filter(isFar(offerCoord, _))).next
      val rids = locations.map(_.getGeobaseId.toInt)
      val enrich =
        offerDistanceEnricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              offerDistance = true,
              offerCardAdditionalParams = OfferCardAdditionalParams.empty.copy(geoIds = rids)
            )
          )(createRequest(offer.userRef))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getSeller.getLocation.getDistanceToSelectedGeoCount shouldBe n
      val distances = enrichedOffer.getSeller.getLocation.getDistanceToSelectedGeoList.asScala
      distances.map(_.getGeobaseId) should contain theSameElementsAs rids
      val regions = rids.flatMap(r => geoTree.region(r))
      distances.map(_.getCoord) should contain theSameElementsAs regions.map(getGeoPoint)
      distances.map(_.getDistance) should contain theSameElementsAs regions.map(r => {
        distance(getGeoPoint(r), offerCoord).toInt
      })
      enrichedOffer.getSeller.getLocation.hasDistanceToUser shouldBe false
    }

    "not enrich with distance from offer seller location to non-settlement rid" in {
      val offer = OfferGen.next
      val locations = List(NonSettlementNonFederalSubjectLocationGen.next)
      val rids = locations.map(_.getGeobaseId.toInt)
      val enrich =
        offerDistanceEnricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              offerDistance = true,
              offerCardAdditionalParams = OfferCardAdditionalParams.empty.copy(geoIds = rids)
            )
          )(createRequest(offer.userRef))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getSeller.getLocation.getDistanceToSelectedGeoCount shouldBe 0
    }

    "not enrich with distance from offer seller location to near x-user-location" in {
      val offer = OfferGen.next
      val offerCoord = getGeoPoint(geoTree.region(offer.getSeller.getLocation.getGeobaseId).value)
      val userLocation = {
        val l = LocationGen.filter(isNear(offerCoord, _)).next
        val r = geoTree.region(l.getGeobaseId).value
        UserLocation(r.latitude.toFloat, r.longitude.toFloat, 65)
      }
      val enrich =
        offerDistanceEnricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              offerDistance = true
            )
          )(createRequest(offer.userRef, Some(userLocation)))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getSeller.getLocation.hasDistanceToUser shouldBe false
    }

    "enrich with distance from offer seller location to x-user-location" in {
      val offer = OfferGen.next
      val offerCoord = getGeoPoint(geoTree.region(offer.getSeller.getLocation.getGeobaseId).value)
      val userLocation = {
        val l = LocationGen.filter(isFar(offerCoord, _)).next
        val r = geoTree.region(l.getGeobaseId).value
        UserLocation(r.latitude.toFloat, r.longitude.toFloat, 65)
      }
      val userCoord = getGeoPoint(userLocation)
      val enrich =
        offerDistanceEnricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              offerDistance = true
            )
          )(createRequest(offer.userRef, Some(userLocation)))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getSeller.getLocation.getDistanceToSelectedGeoCount shouldBe 0
      enrichedOffer.getSeller.getLocation.hasDistanceToUser shouldBe true
      enrichedOffer.getSeller.getLocation.getDistanceToUser.getCoord shouldBe userCoord
      enrichedOffer.getSeller.getLocation.getDistanceToUser.getGeobaseId shouldBe 0
      enrichedOffer.getSeller.getLocation.getDistanceToUser.getDistance shouldBe distance(userCoord, offerCoord).toInt
    }

    "enrich with both distances" in {
      val n = Gen.choose(1, 5).next
      val offer = OfferGen.next
      val offerCoord = getGeoPoint(geoTree.region(offer.getSeller.getLocation.getGeobaseId).value)
      val userLocation = {
        val l = LocationGen.filter(isFar(offerCoord, _)).next
        val r = geoTree.region(l.getGeobaseId).value
        UserLocation(r.latitude.toFloat, r.longitude.toFloat, 65)
      }
      val userCoord = getGeoPoint(userLocation)
      val locations = Gen.listOfN(n, LocationGen.filter(isFar(offerCoord, _))).next
      val rids = locations.map(_.getGeobaseId.toInt)
      val enrich =
        offerDistanceEnricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              offerDistance = true,
              offerCardAdditionalParams = OfferCardAdditionalParams.empty.copy(geoIds = rids)
            )
          )(createRequest(offer.userRef, Some(userLocation)))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getSeller.getLocation.getDistanceToSelectedGeoCount shouldBe n
      val distances = enrichedOffer.getSeller.getLocation.getDistanceToSelectedGeoList.asScala
      distances.map(_.getGeobaseId) should contain theSameElementsAs rids
      val regions = rids.flatMap(r => geoTree.region(r))
      distances.map(_.getCoord) should contain theSameElementsAs regions.map(getGeoPoint)
      distances.map(_.getDistance) should contain theSameElementsAs regions.map(r => {
        distance(getGeoPoint(r), offerCoord).toInt
      })
      enrichedOffer.getSeller.getLocation.hasDistanceToUser shouldBe true
      enrichedOffer.getSeller.getLocation.getDistanceToUser.getCoord shouldBe userCoord
      enrichedOffer.getSeller.getLocation.getDistanceToUser.getGeobaseId shouldBe 0
      enrichedOffer.getSeller.getLocation.getDistanceToUser.getDistance shouldBe distance(userCoord, offerCoord).toInt
    }

    "not enrich with any distance" in {
      val n = Gen.choose(1, 5).next
      val offer = OfferGen.next
      val userLocation = {
        val l = LocationGen.next
        val r = geoTree.region(l.getGeobaseId).value
        UserLocation(r.latitude.toFloat, r.longitude.toFloat, 65)
      }
      val locations = Gen.listOfN(n, LocationGen).next
      val rids = locations.map(_.getGeobaseId.toInt)
      val enrich =
        offerDistanceEnricher
          .getFunction(
            Seq(offer),
            EnrichOptions(
              offerDistance = false,
              offerCardAdditionalParams = OfferCardAdditionalParams.empty.copy(geoIds = rids)
            )
          )(createRequest(offer.userRef, Some(userLocation)))
          .futureValue
      val enrichedOffer = enrich(offer)
      enrichedOffer.getSeller.getLocation.getDistanceToSelectedGeoCount shouldBe 0
      enrichedOffer.getSeller.getLocation.hasDistanceToUser shouldBe false
    }
  }

  private def isNear(offerCoord: CommonModel.GeoPoint, l: ApiOfferModel.Location) = {
    distance(getByLocation(l), offerCoord) < MinDistance
  }

  private def isFar(offerCoord: CommonModel.GeoPoint, l: ApiOfferModel.Location) = {
    distance(getByLocation(l), offerCoord) > MinDistance
  }

  private def getByLocation(l: ApiOfferModel.Location) = {
    getGeoPoint(geoTree.region(l.getGeobaseId).get)
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
}
