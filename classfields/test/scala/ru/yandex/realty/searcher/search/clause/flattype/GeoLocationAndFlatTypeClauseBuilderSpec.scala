package ru.yandex.realty.searcher.search.clause.flattype

import com.google.protobuf.Int64Value
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.{ApartmentInfo, FlatType, Offer}
import ru.yandex.realty.proto.RouteDistance
import ru.yandex.realty.proto.offer.vos.Offer.Placement
import ru.yandex.realty.proto.unified.offer.address.Station
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{FlatTypeClauseBuilder, GeoLocationClauseBuilder}
import ru.yandex.realty.searcher.search.clause.{NRTIndexFixture, NRTIndexOfferGenerator, SimpleQueryProvider}

import scala.collection.JavaConverters._

/**
  * вторичка + ж/д -> всё понятно
  * новостройки + шоссе -> всё понятно
  */
@RunWith(classOf[JUnitRunner])
class GeoLocationAndFlatTypeClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new FlatTypeClauseBuilder(), new GeoLocationClauseBuilder(regionGraph))
  private val offers: Seq[Offer] =
    Seq(
      (FlatType.NEW_FLAT, Some(1L), Some(11L)),
      (FlatType.NEW_SECONDARY, Some(2L), Some(14L)),
      (FlatType.SECONDARY, Some(2L), Some(13L)),
      (FlatType.NEW_SECONDARY, None, None),
      (FlatType.NEW_FLAT, None, Some(12L)),
      (FlatType.SECONDARY, Some(1L), Some(12L)),
      (FlatType.SECONDARY, None, Some(13L)),
      (FlatType.NEW_FLAT, Some(1L), Some(12L)),
      (FlatType.SECONDARY, Some(4L), Some(13L))
    ).zipWithIndex
      .map {
        case ((flatType, reailwayStationOpt, highwayOpt), index) =>
          buildOffer(index, flatType, reailwayStationOpt, highwayOpt)
      }

  insertOffers(offers)

  "GeoLocationAndFlatTypeClauseBuilder" should {
    "search secondary offers near railway station 1" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFlatType(FlatType.SECONDARY)
      searchQuery.setRailwayStation(1L)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
    }

    "search new flats near railway station 1" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFlatType(FlatType.NEW_FLAT)
      searchQuery.setRailwayStation(1L)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 2
    }

    "search new secondary offers near railway station 2" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFlatType(FlatType.NEW_SECONDARY)
      searchQuery.setRailwayStation(2L)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
    }

    "search secondary offers near highway 13" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFlatType(FlatType.SECONDARY)
      searchQuery.setDirection(13)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 3
    }

    "search new flats near highway 12" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFlatType(FlatType.NEW_FLAT)
      searchQuery.setDirection(12)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 2
    }

    "search new secondary offers near highway 14" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFlatType(FlatType.NEW_SECONDARY)
      searchQuery.setDirection(14)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
    }

  }

  private def buildOffer(
    offerId: Long,
    flatType: FlatType,
    reailwayStationOpt: Option[Long],
    highwayOpt: Option[Long]
  ): Offer = {
    val offer = offerGen(offerId = offerId).next

    val apartmentInfo = new ApartmentInfo()
    apartmentInfo.setFlatType(flatType)
    offer.setApartmentInfo(apartmentInfo)

    val placementInfo = Placement.newBuilder().setPayed(true).build()
    offer.setPlacementInfo(placementInfo)

    val location = new Location()
    reailwayStationOpt.foreach(railwayStation => {
      val station = Station.newBuilder().setEsr(railwayStation).build()
      location.setStation(Seq(station).asJava)
    })
    highwayOpt.foreach(highway => {
      val distance = RouteDistance.newBuilder().setHighwayId(Int64Value.of(highway)).build()
      location.setRouteDistances(Seq(distance).asJava)
    })
    offer.setLocation(location)

    offer
  }
}
