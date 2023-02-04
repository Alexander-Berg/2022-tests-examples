package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{BuildingInfo, Offer}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{
  CeilingHeightClauseBuilder,
  FloorClauseBuilder,
  MatchAllDocsClauseBuilder
}

import scala.collection.JavaConverters._

/**
  * купить квартиру + высота потолков > 3м - в точное совпадение кв с высотой потолков > 3м,
  * в неточное - с потолками > 2.7м и те,
  * где высота потолков не указана, отсекаются кв с высотой потолков < 2.7м
  */
@RunWith(classOf[JUnitRunner])
class CeilingHeightClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new MatchAllDocsClauseBuilder, new CeilingHeightClauseBuilder())
  private val offers: Seq[Offer] = Seq(
    buildOffer(1, Some(2.5f)),
    buildOffer(2, Some(2.7f)),
    buildOffer(3, Some(2.8f)),
    buildOffer(4, Some(3f)),
    buildOffer(5, Some(3.5f)),
    buildOffer(6, None)
  )

  insertOffers(offers)

  "CeilingHeightClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      val result = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      result.getTotal shouldBe offers.size
    }

    "search offers on ceiling height range without similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setCeilingHeight(ru.yandex.realty.util.Range.create(3f, null))
      searchQuery.setShowSimilar(false)

      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(4L, 5L)
    }

    "search offers on floor range with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setCeilingHeight(ru.yandex.realty.util.Range.create(3f, null))
      searchQuery.setShowSimilar(true)

      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(2L, 3L, 4L, 5L, 6L)
    }

    "search offers on floor range with similar sort" in {
      val searchQuery = new SearchQuery()
      searchQuery.setCeilingHeight(ru.yandex.realty.util.Range.create(3f, null))
      searchQuery.setShowSimilar(true)

      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).takeRight(3).toSet shouldBe Set(2L, 3L, 6L)
    }

  }

  private def buildOffer(offerId: Long, ceilingHeight: Option[Float]): Offer = {
    val offer = offerGen(offerId = offerId).next
    offer.getApartmentInfo.setCeilingHeight(null, null)
    ceilingHeight.foreach(h => offer.getApartmentInfo.setCeilingHeight(h, h))
    offer
  }
}
