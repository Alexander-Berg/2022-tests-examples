package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.lucene.OfferDocumentFields
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.proto.unified.offer.address.Metro
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.basic.AbstractMultipleIntClauseBuilder

import scala.collection.JavaConverters._

/**
  * выбрано 2 станции метро -> в выдачу попадают квартиры, которые привязаны одновременно к обоим метро + те, что привязаны хотя бы к одному, отсекаются те, что не привязаны ни к одному
  */
@RunWith(classOf[JUnitRunner])
class MetroGeoIdClauseSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new MetroGeoIdClauseBuilder())
  private val offers: Seq[Offer] =
    Seq(
      Seq(3, 1),
      Seq(4, 3),
      Seq(2, 7),
      Seq(4, 1),
      Seq(5),
      Seq(3),
      Seq(4, 6),
      Seq(7, 5),
      Seq(4),
      Seq(3, 1),
      Seq(4, 5)
    ).zipWithIndex
      .map {
        case (metroGeoIds, index) => buildOffer(index, metroGeoIds)
      }

  insertOffers(offers)

  "MetroGeoIdClause" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setMetroGeoId(1)
      searchQuery.setMetroGeoId(2)
      searchQuery.setMetroGeoId(3)
      searchQuery.setMetroGeoId(4)
      searchQuery.setMetroGeoId(5)
      searchQuery.setMetroGeoId(6)
      searchQuery.setMetroGeoId(7)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 11
    }

    "search offers with metro geo id 5" in {
      val searchQuery = new SearchQuery()
      searchQuery.setMetroGeoId(5)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 3
    }

    "search offers with metro geo id 4" in {
      val searchQuery = new SearchQuery()
      searchQuery.setMetroGeoId(4)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 5
    }

    "search offers with metro geo id 4 and 5" in {
      val searchQuery = new SearchQuery()
      searchQuery.setMetroGeoId(4)
      searchQuery.setMetroGeoId(5)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 7
    }

  }

  private def buildOffer(offerId: Long, metroGeoIds: Seq[Int]): Offer = {
    val offer = offerGen(offerId = offerId).next

    val location = new Location()
    val metros = metroGeoIds.map(metro => {
      val builder = Metro.newBuilder()
      builder.setGeoId(metro)
      builder.setName(s"metro$metro")
      builder.build()
    })
    location.setMetro(metros.asJava)
    offer.setLocation(location)

    offer
  }
}

class MetroGeoIdClauseBuilder() extends AbstractMultipleIntClauseBuilder(OfferDocumentFields.METRO_GEO_IDS) {

  override def getValues(searchQuery: SearchQuery): java.util.Collection[java.lang.Integer] = {
    searchQuery.getMetroGeoId
  }
}
