package ru.yandex.realty.searcher.search.clause.rooms

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{ApartmentInfo, BuildingInfo, Offer}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{RoomsRangeClauseBuilder, SiteIdClauseBuilder}
import ru.yandex.realty.searcher.search.clause.{NRTIndexFixture, NRTIndexOfferGenerator, SimpleQueryProvider}

/**
  * трехкомнатные + ЖК -> в выдаче только трешки в выбранном жк, отсекаются другие комнатности в выбранном жк и трешки в других жк
  */
@RunWith(classOf[JUnitRunner])
class SiteIdAndRoomsRangeClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new RoomsRangeClauseBuilder(), new SiteIdClauseBuilder())
  private val offers: Seq[Offer] =
    Seq(
      (3, 21L),
      (1, 23L),
      (2, 22L),
      (1, 21L),
      (2, 23L),
      (3, 22L),
      (1, 21L),
      (3, 23L),
      (4, 22L),
      (1, 21L),
      (3, 23L)
    ).zipWithIndex
      .map {
        case ((roomsNumber, siteId), index) => buildOffer(index, roomsNumber, siteId)
      }

  insertOffers(offers)

  "SiteIdAndRoomsRangeClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRoomsMin(-10)
      searchQuery.setRoomsMax(10)
      searchQuery.setShowSimilar(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 11
    }

    "search offers with 3 room and given site" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRoomsMin(3)
      searchQuery.setRoomsMax(3)
      searchQuery.setSiteId(23L)
      searchQuery.setShowSimilar(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 2
    }

  }

  private def buildOffer(offerId: Long, roomsNumber: Int, siteId: Long): Offer = {
    val offer = offerGen(offerId = offerId).next

    val apartmentInfo = new ApartmentInfo()
    apartmentInfo.setRooms(roomsNumber)
    if (roomsNumber == 1) {
      apartmentInfo.setStudio(true)
    }
    offer.setApartmentInfo(apartmentInfo)

    val buildingInfo = new BuildingInfo()
    buildingInfo.setSiteId(siteId)
    offer.setBuildingInfo(buildingInfo)

    offer
  }
}
