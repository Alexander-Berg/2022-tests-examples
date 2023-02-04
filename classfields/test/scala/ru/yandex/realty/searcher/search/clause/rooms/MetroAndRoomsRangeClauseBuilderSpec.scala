package ru.yandex.realty.searcher.search.clause.rooms

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.lucene.OfferDocumentFields
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.{ApartmentInfo, Offer}
import ru.yandex.realty.proto.unified.offer.address.Metro
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.RoomsRangeClauseBuilder
import ru.yandex.realty.searcher.query.clausebuilder.basic.AbstractMultipleIntClauseBuilder
import ru.yandex.realty.searcher.search.clause.{NRTIndexFixture, NRTIndexOfferGenerator, SimpleQueryProvider}

import scala.collection.JavaConverters._

/**
  * четырехкомнатные + метро -> всё понятно
  */
@RunWith(classOf[JUnitRunner])
class MetroAndRoomsRangeClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  val clauseBuilders = Seq(new RoomsRangeClauseBuilder(), new MetroGeoIdClauseBuilder())
  private val offers: Seq[Offer] =
    Seq(
      (3, 21),
      (4, 23),
      (2, 22),
      (4, 21),
      (2, 23),
      (3, 22),
      (4, 21),
      (3, 23),
      (4, 22),
      (4, 21),
      (3, 23)
    ).zipWithIndex
      .map {
        case ((roomsNumber, metro), index) => buildOffer(index, roomsNumber, metro)
      }

  insertOffers(offers)

  "MetroAndRoomsRangeClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRoomsMin(-10)
      searchQuery.setRoomsMax(10)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 11
    }

    "search offers with 3 room and given site" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRoomsMin(4)
      searchQuery.setRoomsMax(4)
      searchQuery.setMetroGeoId(21)
      searchQuery.setShowSimilar(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 3
    }

  }

  private def buildOffer(offerId: Long, roomsNumber: Int, metro: Int): Offer = {
    val offer = offerGen(offerId = offerId).next

    val apartmentInfo = new ApartmentInfo()
    apartmentInfo.setRooms(roomsNumber)
    if (roomsNumber == 1) {
      apartmentInfo.setStudio(true)
    }
    offer.setApartmentInfo(apartmentInfo)

    val location = new Location()
    val builder = Metro.newBuilder()
    builder.setGeoId(metro)
    builder.setName(s"metro$metro")
    location.setMetro(Seq(builder.build()).asJava)
    offer.setLocation(location)

    offer
  }
}

class MetroGeoIdClauseBuilder() extends AbstractMultipleIntClauseBuilder(OfferDocumentFields.METRO_GEO_IDS) {

  override def getValues(searchQuery: SearchQuery): java.util.Collection[java.lang.Integer] = {
    searchQuery.getMetroGeoId
  }
}
