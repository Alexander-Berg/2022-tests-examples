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
  * купить квартиру от 100к до 200к за м² -> в точный поиск попали кв 101к и 199к за м², в неточном - 91к, 99к, 201к, 219к, не попали - за 89к и 221к за м²
  */
@RunWith(classOf[JUnitRunner])
class PerMeterPriceClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private lazy val clauseBuilders = Seq(new PriceClauseBuilder(regionGraph, currencyStorage))
  private val currencyStorage = new CurrencyStorage(Seq.empty.asJava, Seq.empty.asJava, regionGraphProvider)
  private val offers: Seq[Offer] =
    Seq(8900000L, 9100000L, 9900000L, 10100000L, 19900000L, 20100000L, 21900000L, 22100000L).zipWithIndex
      .map {
        case (price, index) => buildOffer(index, price)
      }

  insertOffers(offers)

  "PriceClauseBuilder - price per meter" should {

    "search all offers with wide price range" in {
      val searchQuery = new SearchQuery()
      searchQuery.setPrice(Range.create(0f, 999999999f))
      searchQuery.setPriceType(PriceType.PER_METER)
      searchQuery.setCurrency(Currency.RUR)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 8
    }

    "search offers by price without similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setPrice(Range.create(100000f, 200000f))
      searchQuery.setPriceType(PriceType.PER_METER)
      searchQuery.setCurrency(Currency.RUR)
      searchQuery.setShowSimilar(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 2
      result.getItems.asScala.map(offer => offer.getTransaction.getSqmInRubles.getScaled).toSet shouldBe
        Set(10100000L, 19900000L)
    }

    "search offers by price with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setPrice(Range.create(100000f, 200000f))
      searchQuery.setPriceType(PriceType.PER_METER)
      searchQuery.setCurrency(Currency.RUR)
      searchQuery.setShowSimilar(true)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 6
      result.getItems.asScala.map(offer => offer.getTransaction.getSqmInRubles.getScaled).toSet shouldBe
        Set(9100000L, 9900000L, 10100000L, 19900000L, 20100000L, 21900000L)
    }
  }

  private def buildOffer(offerId: Long, price: Long): Offer = {
    val offer = offerGen(offerId = offerId).next
    val square = 39.1f
    val moneyPerMeter = Money.scaledOf(Currency.RUR, price)
    val moneyPerOffer = Money.scaledOf(Currency.RUR, Math.round(square * price))
    val transaction = new Transaction()
    transaction.setSqmInRubles(moneyPerMeter, square)
    // если не ставить wholeInRubles, то оффер всегда попадает в similar
    transaction.setWholeInRubles(moneyPerOffer)
    offer.setTransaction(transaction)
    offer
  }
}
