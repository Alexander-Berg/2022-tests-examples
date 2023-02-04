package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{BathroomUnitType, Offer}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{
  ApartmentsClauseBuilder,
  BathroomUnitClauseBuilder,
  MatchAllDocsClauseBuilder
}

import scala.collection.JavaConverters._

/**
  * купить квартиру + апартаменты - в выдаче апарты, жилые кв отсекаются
  * купить квартиру + без апартаментов - в выдаче жилые кв, апарты отсекаются
  */
@RunWith(classOf[JUnitRunner])
class ApartmentsClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new MatchAllDocsClauseBuilder, new ApartmentsClauseBuilder)
  private val offers: Seq[Offer] = Seq(
    buildOffer(1, true),
    buildOffer(2, false)
  )

  insertOffers(offers)

  "ApartmentsClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe offers.size
    }

    "search offers with apartments" in {
      val searchQuery = new SearchQuery()
      searchQuery.setApartments(true)
      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(1L)
    }

    "search offers without apartments" in {
      val searchQuery = new SearchQuery()
      searchQuery.setApartments(false)
      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(2L)
    }

  }

  private def buildOffer(offerId: Long, apartments: Boolean): Offer = {
    val offer = offerGen(offerId = offerId).next
    offer.getApartmentInfo.setApartments(apartments)
    offer
  }
}
