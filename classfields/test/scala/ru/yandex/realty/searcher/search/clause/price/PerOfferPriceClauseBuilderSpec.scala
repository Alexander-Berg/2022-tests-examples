package ru.yandex.realty.searcher.search.clause.price

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{Money, Offer, Transaction}
import ru.yandex.realty.model.request.PriceType
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.PriceClauseBuilder
import ru.yandex.realty.searcher.search.clause.{NRTIndexFixture, NRTIndexOfferGenerator, SimpleQueryProvider}
import ru.yandex.realty.storage.CurrencyStorage
import ru.yandex.realty.util.Range

import scala.collection.JavaConverters._

/**
  * купить квартиру от 5кк до 10кк -> в точный поиск попали кв за 5,1кк, 9,9кк, в неточный за 4,6кк, 4,9кк, 10,1кк, 10,9кк, не попали в выдачу за 4,4кк, 11,1кк
  * купить квартиру от 5кк до 10кк + без похожих -> в выдачу попали кв за 5,1кк, 9,9кк, не попали кв за 4,9кк, 10,1кк
  */
@RunWith(classOf[JUnitRunner])
class PerOfferPriceClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private lazy val clauseBuilders = Seq(new PriceClauseBuilder(regionGraph, currencyStorage))
  private val currencyStorage = new CurrencyStorage(Seq.empty.asJava, Seq.empty.asJava, regionGraphProvider)
  private val offers: Seq[Offer] =
    Seq(440000000L, 460000000L, 490000000L, 510000000L, 990000000L, 1010000000L, 1090000000L, 1110000000L).zipWithIndex
      .map {
        case (price, index) => buildOffer(index, price)
      }

  insertOffers(offers)

  "PriceClauseBuilder - price for whole offer" should {

    "search all offers with wide price range" in {
      val searchQuery = new SearchQuery()
      searchQuery.setPrice(Range.create(0f, 999999999f))
      searchQuery.setPriceType(PriceType.PER_OFFER)
      searchQuery.setCurrency(Currency.RUR)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 8
    }

    "search offers by price without similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setPrice(Range.create(5000000f, 10000000f))
      searchQuery.setPriceType(PriceType.PER_OFFER)
      searchQuery.setCurrency(Currency.RUR)
      searchQuery.setShowSimilar(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 2
      result.getItems.asScala.map(offer => offer.getTransaction.getWholeInRubles.getScaled).toSet shouldBe
        Set(510000000L, 990000000L)
    }

    "search offers by price with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setPrice(Range.create(5000000f, 10000000f))
      searchQuery.setPriceType(PriceType.PER_OFFER)
      searchQuery.setCurrency(Currency.RUR)
      searchQuery.setShowSimilar(true)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 6
      result.getItems.asScala.map(offer => offer.getTransaction.getWholeInRubles.getScaled).toSet shouldBe
        Set(460000000L, 490000000L, 510000000L, 990000000L, 1010000000L, 1090000000L)
    }
  }

  private def buildOffer(offerId: Long, price: Long): Offer = {
    val offer = offerGen(offerId = offerId).next
    val money = Money.scaledOf(Currency.RUR, price)
    val transaction = new Transaction()
    transaction.setWholeInRubles(money)
    offer.setTransaction(transaction)
    offer
  }
}
