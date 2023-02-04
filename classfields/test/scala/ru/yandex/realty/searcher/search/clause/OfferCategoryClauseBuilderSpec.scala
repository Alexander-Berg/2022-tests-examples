package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{CategoryType, Offer}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.OfferCategoryClauseBuilder
import ru.yandex.realty.searcher.search.clause.OfferCategoryTypeClauseBuilderSpec._

@RunWith(classOf[JUnitRunner])
class OfferCategoryClauseBuilderSpec extends SpecBase with NRTIndexFixture {

  private val clauseBuilders = Seq(new OfferCategoryClauseBuilder())
  insertOffers(offers)

  "OfferCategoryClauseBuilder" should {
    "search apartment offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setCategory(CategoryType.APARTMENT)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual apartmentOffer.getId
      resultOffer.getCategoryType shouldEqual apartmentOffer.getCategoryType
    }

    "search rooms offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setCategory(CategoryType.ROOMS)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual roomsOffer.getId
      resultOffer.getCategoryType shouldEqual roomsOffer.getCategoryType
    }

    "search house offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setCategory(CategoryType.HOUSE)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual houseOffer.getId
      resultOffer.getCategoryType shouldEqual houseOffer.getCategoryType
    }

    "search lot offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setCategory(CategoryType.LOT)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual lotOffer.getId
      resultOffer.getCategoryType shouldEqual lotOffer.getCategoryType
    }

    "search commercial offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setCategory(CategoryType.COMMERCIAL)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual commercialOffer.getId
      resultOffer.getCategoryType shouldEqual commercialOffer.getCategoryType
    }

    "search garage offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setCategory(CategoryType.GARAGE)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual garageOffer.getId
      resultOffer.getCategoryType shouldEqual garageOffer.getCategoryType
    }
  }
}

object OfferCategoryTypeClauseBuilderSpec extends NRTIndexOfferGenerator {

  val roomsOffer = buildOffer(1, CategoryType.ROOMS)
  val apartmentOffer = buildOffer(2, CategoryType.APARTMENT)
  val houseOffer = buildOffer(3, CategoryType.HOUSE)
  val lotOffer = buildOffer(4, CategoryType.LOT)
  val commercialOffer = buildOffer(5, CategoryType.COMMERCIAL)
  val garageOffer = buildOffer(6, CategoryType.GARAGE)
  val offers = Seq(roomsOffer, apartmentOffer, houseOffer, lotOffer, commercialOffer, garageOffer)

  def buildOffer(offerId: Long, categoryType: CategoryType): Offer = {
    val offer = offerGen(offerId = offerId).next
    offer.setCategoryType(categoryType)
    offer
  }
}
