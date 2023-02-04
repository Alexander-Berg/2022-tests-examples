package ru.yandex.realty.searcher.search.clause.rooms

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{ApartmentInfo, Offer}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.RoomsRangeClauseBuilder
import ru.yandex.realty.searcher.search.clause.{NRTIndexFixture, NRTIndexOfferGenerator, SimpleQueryProvider}

import scala.collection.JavaConverters._

/**
  * однокомнатная + двухкомнатная -> в выдаче есть и одно- и двух-комнатные кв, отсекаются остальные комнатности
  */
@RunWith(classOf[JUnitRunner])
class RoomsRangeClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new RoomsRangeClauseBuilder())
  private val offers: Seq[Offer] =
    Seq(4, 1, 2, 5, 2, 3, 1, 3, 4, 1, 5).zipWithIndex
      .map {
        case (roomsNumber, index) => buildOffer(index, roomsNumber)
      }

  insertOffers(offers)

  "RoomsRangeClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRoomsMin(0)
      searchQuery.setRoomsMax(10)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 11
    }

    "search offers with 1 room" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRoomsMin(1)
      searchQuery.setRoomsMax(1)
      searchQuery.setShowSimilar(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 3
      result.getItems.asScala.map(offer => offer.getApartmentInfo.getRooms).toSet shouldBe Set(1L)
    }

    "search offers with 2 rooms" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRoomsMin(2)
      searchQuery.setRoomsMax(2)
      searchQuery.setShowSimilar(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 2
      result.getItems.asScala.map(offer => offer.getApartmentInfo.getRooms).toSet shouldBe Set(2L)
    }

    "search offers with 2 room with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRoomsMin(2)
      searchQuery.setRoomsMax(2)
      searchQuery.setShowSimilar(true)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 9
      result.getItems.asScala.map(offer => offer.getApartmentInfo.getRooms).toSet shouldBe Set(1L, 2L, 3L, 4L)
    }

    "search offers with 1 or 2 rooms" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRoomsMin(1)
      searchQuery.setRoomsMax(2)
      searchQuery.setShowSimilar(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 5
      result.getItems.asScala.map(offer => offer.getApartmentInfo.getRooms).toSet shouldBe Set(1L, 2L)
    }
  }

  private def buildOffer(offerId: Long, roomsNumber: Int): Offer = {
    val offer = offerGen(offerId = offerId).next
    val apartmentInfo = new ApartmentInfo()
    apartmentInfo.setRooms(roomsNumber)
    offer.setApartmentInfo(apartmentInfo)
    offer
  }
}
