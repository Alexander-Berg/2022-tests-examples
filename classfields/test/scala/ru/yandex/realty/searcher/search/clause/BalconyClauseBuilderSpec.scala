package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.proto.offer.BalconyType
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{BalconyClauseBuilder, MatchAllDocsClauseBuilder}

import scala.collection.JavaConverters._

/**
  * купить квартира + есть балкон - в точное совпадение попадают кв с балконом, в неточное - те, где балкон не указан, отсекаются квартиры с лоджиями и без балкона
  */
@RunWith(classOf[JUnitRunner])
class BalconyClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new MatchAllDocsClauseBuilder, new BalconyClauseBuilder())
  private val offers: Seq[Offer] = Seq(
    buildOffer(1, Some(BalconyType.BALCONY_TYPE_BALCONY)),
    buildOffer(2, Some(BalconyType.BALCONY_TYPE_LOGGIA)),
    buildOffer(3, Some(BalconyType.BALCONY_TYPE_BALCONY_LOGGIA)),
    buildOffer(4, None)
  )

  insertOffers(offers)

  "BalconyClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe offers.size
    }

    "search offers with balcony" in {
      val searchQuery = new SearchQuery()
      searchQuery.setBalcony(ru.yandex.realty.model.offer.BalconyType.BALCONY)
      val resultWithSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size)

      searchQuery.setShowSimilar(false)
      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size)

      resultWithSimilar.getTotal shouldBe 3
      resultWithoutSimilar.getTotal shouldBe 2
      resultWithSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(1L, 3L, 4L)
      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(1L, 3L)

      resultWithSimilar.getItems.asScala.map(offer => offer.getLongId).takeRight(1).toSet shouldBe Set(4L)

    }

    "search offers with loggia" in {
      val searchQuery = new SearchQuery()
      searchQuery.setBalcony(ru.yandex.realty.model.offer.BalconyType.LOGGIA)
      val resultWithSimilar = search(searchQuery, clauseBuilders)

      searchQuery.setShowSimilar(false)
      val resultWithoutSimilar = search(searchQuery, clauseBuilders)

      resultWithSimilar.getTotal shouldBe 3
      resultWithoutSimilar.getTotal shouldBe 2
      resultWithSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(2L, 3L, 4L)
      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(2L, 3L)

      resultWithSimilar.getItems.asScala.map(offer => offer.getLongId).takeRight(1).toSet shouldBe Set(4L)

    }

  }

  private def buildOffer(offerId: Long, balconyType: Option[BalconyType]): Offer = {
    val offer = offerGen(offerId = offerId).next
    offer.getApartmentInfo.setBalcony(BalconyType.BALCONY_TYPE_UNKNOWN)
    balconyType.foreach(b => offer.getApartmentInfo.setBalcony(b))
    offer
  }
}
