package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{BuildingInfo, Offer}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{
  IsFirstFloorClauseBuilder,
  LastFloorClauseBuilder,
  MatchAllDocsClauseBuilder
}

import scala.collection.JavaConverters._

/**
  * купить квартиру + не последний этаж - изначально было 4 квартиры: на 2-ом и 5-ом этаже в 5-ти
  */
@RunWith(classOf[JUnitRunner])
class LastFloorClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new MatchAllDocsClauseBuilder, new LastFloorClauseBuilder())
  private val offers: Seq[Offer] = Seq(
    buildOffer(1, Some(2), Some(5)),
    buildOffer(2, Some(3), None),
    buildOffer(3, Some(5), Some(5)),
    buildOffer(4, None, Some(5))
  )

  insertOffers(offers)

  "LastFloorClauseBuilder" should {

    "search for all offers" in {
      val searchQuery = new SearchQuery()
      val result = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      result.getTotal shouldBe offers.size
    }

    "search offers on last floor without similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setLastFloor(true)
      searchQuery.setShowSimilar(false)

      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(3L)
    }

    "search offers not on last floor without similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setLastFloor(false)
      searchQuery.setShowSimilar(false)

      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(1L)
    }

    "search offers on last floor with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setLastFloor(true)
      searchQuery.setShowSimilar(true)

      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(2L, 3L, 4L)
    }

    "search offers not on last floor with similar" in {
      val searchQuery = new SearchQuery()
      searchQuery.setLastFloor(false)
      searchQuery.setShowSimilar(true)

      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(1L, 2L, 4L)
    }

    "search offers on last floor with similar sort" in {
      val searchQuery = new SearchQuery()
      searchQuery.setLastFloor(true)
      searchQuery.setShowSimilar(true)

      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).takeRight(2).toSet shouldBe Set(2L, 4L)
    }

    "search offers not on last floor with similar sort" in {
      val searchQuery = new SearchQuery()
      searchQuery.setLastFloor(false)
      searchQuery.setShowSimilar(true)

      val resultWithoutSimilar = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      resultWithoutSimilar.getItems.asScala.map(offer => offer.getLongId).takeRight(2).toSet shouldBe Set(2L, 4L)
    }

  }

  private def buildOffer(offerId: Long, floor: Option[Int], floorsTotal: Option[Int]): Offer = {
    val offer = offerGen(offerId = offerId).next
    offer.getApartmentInfo.setFloors(List.empty.asJava)
    if (offer.getBuildingInfo == null) {
      offer.setBuildingInfo(new BuildingInfo())
    }
    offer.getBuildingInfo.setFloorsTotal(null)
    floor.foreach(f => offer.getApartmentInfo.setFloors(List(int2Integer(f)).asJava))
    floorsTotal.foreach(f => offer.getBuildingInfo.setFloorsTotal(f))
    offer
  }
}
