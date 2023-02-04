package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{BuildingInfo, Offer}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{FloorClauseBuilder, MatchAllDocsClauseBuilder}

import scala.collection.JavaConverters._

/**
  * купить квартиру + этаж с 2 по 4 - изначально есть квартиры с 1 по 5 этаж,
  * после поиска должны остаться только с 2 по 4
  */
@RunWith(classOf[JUnitRunner])
class FloorClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new MatchAllDocsClauseBuilder, new FloorClauseBuilder())
  private val offers: Seq[Offer] = Seq(
    buildOffer(1, Some(1)),
    buildOffer(2, Some(2)),
    buildOffer(3, Some(3)),
    buildOffer(4, Some(4)),
    buildOffer(5, Some(5)),
    buildOffer(6, None)
  )

  insertOffers(offers)

  "FloorClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      val result = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      result.getTotal shouldBe offers.size
    }

    "search offers on floor range without similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFloor(ru.yandex.realty.util.Range.create(2, 4))
      searchQuery.setShowSimilar(false)

      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(2L, 3L, 4L)
    }

    "search offers on floor range with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFloor(ru.yandex.realty.util.Range.create(2, 4))
      searchQuery.setShowSimilar(true)

      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(2L, 3L, 4L, 6L)
    }
  }

  private def buildOffer(offerId: Long, floor: Option[Int]): Offer = {
    val offer = offerGen(offerId = offerId).next
    offer.getApartmentInfo.setFloors(List.empty.asJava)
    if (offer.getBuildingInfo == null) {
      offer.setBuildingInfo(new BuildingInfo())
    }
    floor.foreach(f => offer.getApartmentInfo.setFloors(List(int2Integer(f)).asJava))
    offer
  }
}
