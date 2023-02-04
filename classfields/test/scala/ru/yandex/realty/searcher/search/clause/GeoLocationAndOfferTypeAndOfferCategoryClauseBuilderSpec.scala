package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.{CategoryType, Offer, OfferType}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{
  GeoLocationClauseBuilder,
  OfferCategoryClauseBuilder,
  OfferTypeClauseBuilder
}

/**
  * купить квартиру + район -> отсекаются офферы в этом районе об аренде квартир и о продаже неКвартир, а также о продаже квартир в другом районе, остается только нужное
  */
@RunWith(classOf[JUnitRunner])
class GeoLocationAndOfferTypeAndOfferCategoryClauseBuilderSpec
  extends SpecBase
  with NRTIndexFixture
  with NRTIndexOfferGenerator {

  private val clauseBuilders =
    Seq(new GeoLocationClauseBuilder(regionGraph), new OfferTypeClauseBuilder(), new OfferCategoryClauseBuilder())
  private val offers: Seq[Offer] =
    Seq(
      (OfferType.SELL, CategoryType.APARTMENT, 432345237L),
      (OfferType.SELL, CategoryType.GARAGE, 432345237L),
      (OfferType.SELL, CategoryType.APARTMENT, 432345236L),
      (OfferType.RENT, CategoryType.APARTMENT, 432345237L),
      (OfferType.RENT, CategoryType.APARTMENT, 432345237L),
      (OfferType.RENT, CategoryType.APARTMENT, 432345236L),
      (OfferType.RENT, CategoryType.COMMERCIAL, 432345236L),
      (OfferType.SELL, CategoryType.APARTMENT, 432345237L),
      (OfferType.RENT, CategoryType.APARTMENT, 432345236L),
      (OfferType.SELL, CategoryType.APARTMENT, 432345237L),
      (OfferType.SELL, CategoryType.COMMERCIAL, 432345237L),
      (OfferType.RENT, CategoryType.APARTMENT, 432345236L)
    ).zipWithIndex
      .map {
        case ((offerType, category, rgid), index) => buildOffer(index, offerType, category, rgid)
      }

  insertOffers(offers)

  "GeoLocationAndOfferTypeAndOfferCategory" should {

    "search sell offers in kirovskiy district of kazan" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRgid(432345237)
      searchQuery.setType(OfferType.SELL)
      searchQuery.setCategory(CategoryType.APARTMENT)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 3
    }

    "search rent offers in sovetskiy district of kazan" in {
      val searchQuery = new SearchQuery()
      searchQuery.setRgid(432345236)
      searchQuery.setType(OfferType.RENT)
      searchQuery.setCategory(CategoryType.APARTMENT)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 3
    }
  }

  def buildOffer(offerId: Long, offerType: OfferType, category: CategoryType, rgid: Long): Offer = {
    val offer = offerGen(offerId = offerId).next

    offer.setOfferType(offerType)

    offer.setCategoryType(category)

    val location = new Location()
    location.setRegionGraphId(rgid)
    offer.setLocation(location)

    offer
  }

}
