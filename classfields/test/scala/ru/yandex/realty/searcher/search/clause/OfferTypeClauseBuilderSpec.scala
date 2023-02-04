package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{Offer, OfferType}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.OfferTypeClauseBuilder
import ru.yandex.realty.searcher.search.clause.OfferTypeClauseBuilderSpec._

@RunWith(classOf[JUnitRunner])
class OfferTypeClauseBuilderSpec extends SpecBase with NRTIndexFixture {

  private val clauseBuilders = Seq(new OfferTypeClauseBuilder())
  insertOffers(offers)

  "OfferTypeClauseBuilder" should {
    "search rent offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setType(OfferType.RENT)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual rentOffer.getId
      resultOffer.getOfferType shouldEqual rentOffer.getOfferType
    }

    "search sell offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setType(OfferType.SELL)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      val resultOffer = result.getItems.get(0)
      resultOffer.getId shouldEqual sellOffer.getId
      resultOffer.getOfferType shouldEqual sellOffer.getOfferType
    }
  }
}

object OfferTypeClauseBuilderSpec extends NRTIndexOfferGenerator {

  val rentOffer = buildOffer(1, OfferType.RENT)
  val sellOffer = buildOffer(2, OfferType.SELL)
  val offers = Seq(rentOffer, sellOffer)

  def buildOffer(offerId: Long, offerType: OfferType): Offer = {
    val offer = offerGen(offerId = offerId).next
    offer.setOfferType(offerType)
    offer
  }
}
