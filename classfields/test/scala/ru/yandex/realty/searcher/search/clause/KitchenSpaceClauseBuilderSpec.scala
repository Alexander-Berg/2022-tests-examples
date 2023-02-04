package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{HouseInfo, Offer}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{KitchenSpaceClauseBuilder, MatchAllDocsClauseBuilder}
import ru.yandex.realty.util.Range

import scala.collection.JavaConverters._

/**
  * купить квартира + кухни от 10м² - в точное совпадение попадают квартиры с кухнями от 10м²,
  * в неточное - с площадью кухни от 9м² до 10м² и те,
  * где площадь кухни не указана,
  * отсекаются кв с площадью менее 9м²
  */
@RunWith(classOf[JUnitRunner])
class KitchenSpaceClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new MatchAllDocsClauseBuilder, new KitchenSpaceClauseBuilder())
  private val offers: Seq[Offer] = Seq(
    buildOffer(1, Some(8)),
    buildOffer(2, Some(9)),
    buildOffer(3, Some(10)),
    buildOffer(4, Some(11)),
    buildOffer(5, Some(12)),
    buildOffer(6, None)
  )
  insertOffers(offers)

  "KitchenSpaceClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe offers.size
    }

    "search offers with given kitchen space" in {
      val searchQuery = new SearchQuery()
      searchQuery.setKitchenSpace(Range.create(10f, null))
      searchQuery.setShowSimilar(false)
      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(3L, 4L, 5L)
    }

    "search offers with given kitchen space and similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setKitchenSpace(Range.create(10f, null))
      val resultWithSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size)

      resultWithSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(2L, 3L, 4L, 5L, 6L)
    }

    "search offers with kitchen space and similar sort" in {
      val searchQuery = new SearchQuery()
      searchQuery.setKitchenSpace(Range.create(10f, null))
      val resultWithSimilar = searchWithSort(searchQuery, clauseBuilders, 100)
      resultWithSimilar.getItems.asScala.map(offer => offer.getLongId).takeRight(2).toSet shouldBe Set(2L, 6L)
    }

  }

  private def buildOffer(offerId: Long, kitchenSpace: Option[Float]): Offer = {
    val offer = offerGen(offerId = offerId).next
    if (offer.getHouseInfo == null) {
      offer.setHouseInfo(new HouseInfo())
    }
    offer.getHouseInfo.setKitchenSpace(null)
    kitchenSpace.foreach(s => offer.getHouseInfo.setKitchenSpace(s))
    offer
  }
}
