package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{IsFirstFloorClauseBuilder, MatchAllDocsClauseBuilder}

import scala.collection.JavaConverters._

/**
  * купить квартира + не первый этаж - в выдаче только квартиры со 2 этажа и выше
  */
@RunWith(classOf[JUnitRunner])
class IsFirstFloorClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new MatchAllDocsClauseBuilder, new IsFirstFloorClauseBuilder())
  private val offers: Seq[Offer] = Seq(
    buildOffer(1, Some(1)),
    buildOffer(2, Some(2)),
    buildOffer(3, None)
  )

  insertOffers(offers)

  "IsFirstFloorClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      val result = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      result.getTotal shouldBe offers.size
    }

    "search offers except first floor without similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFloorExceptFirst(true)
      searchQuery.setShowSimilar(false)

      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(2L)

    }

    "search offers except first floor with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFloorExceptFirst(true)
      searchQuery.setShowSimilar(true)

      val resultWithSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(2L, 3L)

    }

    "search offers except first floor with similar sort" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFloorExceptFirst(true)
      searchQuery.setShowSimilar(true)

      val resultWithSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithSimilar.getItems.asScala.map(offer => offer.getLongId).takeRight(1).toSet shouldBe Set(3L)

    }

  }

  private def buildOffer(offerId: Long, floor: Option[Int]): Offer = {
    val offer = offerGen(offerId = offerId).next
    offer.getApartmentInfo.setFloors(List.empty.asJava)
    floor.foreach(f => offer.getApartmentInfo.setFloors(List(int2Integer(f)).asJava))
    offer
  }
}
