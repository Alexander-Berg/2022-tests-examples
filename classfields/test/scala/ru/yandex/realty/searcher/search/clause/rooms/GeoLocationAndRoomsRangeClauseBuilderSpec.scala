package ru.yandex.realty.searcher.search.clause.rooms

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.location.{Location, LocationAccuracy}
import ru.yandex.realty.model.offer.{ApartmentInfo, Offer}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{GeoLocationClauseBuilder, RoomsRangeClauseBuilder}
import ru.yandex.realty.searcher.search.clause.{NRTIndexFixture, NRTIndexOfferGenerator, SimpleQueryProvider}

/**
  * студия + улица -> в выдаче только студии по указанной улице, отсекаются другие комнатности на выбранной улице и студии на других улицах
  */
@RunWith(classOf[JUnitRunner])
class GeoLocationAndRoomsRangeClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new RoomsRangeClauseBuilder(), new GeoLocationClauseBuilder(regionGraph))
  private val offers: Seq[Offer] =
    Seq(
      (4, "Проспект"),
      (1, "Переулок"),
      (2, "Проспект"),
      (1, "Проспект"),
      (2, "Переулок"),
      (3, "Проспект"),
      (1, "Улица"),
      (3, "Улица"),
      (4, "Улица"),
      (1, "Переулок"),
      (1, "Улица")
    ).zipWithIndex
      .map {
        case ((roomsNumber, street), index) => buildOffer(index, roomsNumber, street)
      }

  insertOffers(offers)

  "GeoLocationAndRoomsRangeClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRoomsMin(-10)
      searchQuery.setRoomsMax(10)
      searchQuery.setShowSimilar(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 11
    }

    "search offers with 1 room and given street" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRoomsMin(-2)
      searchQuery.setRoomsMax(-2)
      searchQuery.setShowSimilar(false)
      searchQuery.setAddress("Россия, город, Улица")
      searchQuery.setUnifiedStreet("Улица")

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 2
    }

  }

  private def buildOffer(offerId: Long, roomsNumber: Int, street: String): Offer = {
    val offer = offerGen(offerId = offerId).next

    val apartmentInfo = new ApartmentInfo()
    apartmentInfo.setRooms(roomsNumber)
    if (roomsNumber == 1) {
      apartmentInfo.setStudio(true)
    }
    offer.setApartmentInfo(apartmentInfo)

    val location = new Location()
    location.setStreet(street)
    location.setAccuracy(LocationAccuracy.EXACT)
    offer.setLocation(location)

    offer
  }
}
