package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{BathroomUnitType, Offer}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{BathroomUnitClauseBuilder, MatchAllDocsClauseBuilder}

import scala.collection.JavaConverters._

/**
  * купить квартиру + совмещенный санузел - в точном совпадении то, что искали, в неточном - те кв, где тип санузел не указан, отсекаются кв с раздельным санузлом
  * купить квартиру + раздельный санузел - в точном совпадении то, что искали, в неточном - те кв, где тип санузел не указан, отсекаются кв с совмещенным санузлом
  */
@RunWith(classOf[JUnitRunner])
class BathroomUnitClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new MatchAllDocsClauseBuilder, new BathroomUnitClauseBuilder)
  private val offers: Seq[Offer] = Seq(
    buildOffer(1, Some(BathroomUnitType.MATCHED)),
    buildOffer(2, Some(BathroomUnitType.SEPARATED)),
    buildOffer(3, None)
  )

  insertOffers(offers)

  " BathroomUnitClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe offers.size
    }

    "search offers with matched bathroom without similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setBathroomUnit(BathroomUnitType.MATCHED)
      searchQuery.setShowSimilar(false)
      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(1L)
    }

    "search offers with matched bathroom with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setBathroomUnit(BathroomUnitType.MATCHED)
      searchQuery.setShowSimilar(true)
      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(1L, 3L)
    }

    "search offers with matched bathroom with similar sort" in {
      val searchQuery = new SearchQuery()
      searchQuery.setBathroomUnit(BathroomUnitType.MATCHED)
      searchQuery.setShowSimilar(true)
      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).takeRight(1).toSet shouldBe Set(3L)
    }

    "search offers with SEPARATED bathroom without similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setBathroomUnit(BathroomUnitType.SEPARATED)
      searchQuery.setShowSimilar(false)
      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(2L)
    }

    "search offers with SEPARATED bathroom with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setBathroomUnit(BathroomUnitType.SEPARATED)
      searchQuery.setShowSimilar(true)
      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(2L, 3L)
    }

    "search offers with SEPARATED bathroom with similar sort" in {
      val searchQuery = new SearchQuery()
      searchQuery.setBathroomUnit(BathroomUnitType.SEPARATED)
      searchQuery.setShowSimilar(true)
      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).takeRight(1).toSet shouldBe Set(3L)
    }

  }

  private def buildOffer(offerId: Long, bathroomUnitType: Option[BathroomUnitType]): Offer = {
    val offer = offerGen(offerId = offerId).next
    offer.getApartmentInfo.setBathroomUnit(BathroomUnitType.UNKNOWN)
    bathroomUnitType.foreach(b => offer.getApartmentInfo.setBathroomUnit(b))
    offer
  }
}
