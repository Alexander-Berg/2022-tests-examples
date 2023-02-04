package ru.yandex.realty.searcher.search.clause.flattype

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{ApartmentInfo, FlatType, Offer}
import ru.yandex.realty.proto.offer.vos.Offer.Placement
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.FlatTypeClauseBuilder
import ru.yandex.realty.searcher.search.clause.{NRTIndexFixture, NRTIndexOfferGenerator, SimpleQueryProvider}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class FlatTypeClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new FlatTypeClauseBuilder())
  private val offers: Seq[Offer] =
    Seq(
      FlatType.NEW_FLAT,
      FlatType.NEW_SECONDARY,
      FlatType.SECONDARY,
      FlatType.NEW_SECONDARY,
      FlatType.NEW_FLAT,
      FlatType.SECONDARY,
      FlatType.SECONDARY,
      FlatType.NEW_FLAT,
      FlatType.SECONDARY
    ).zipWithIndex
      .map {
        case (flatType, index) => buildOffer(index, flatType)
      }

  insertOffers(offers)

  "FlatTypeClauseBuilder" should {
    "search secondary offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFlatType(FlatType.SECONDARY)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 4
      result.getItems.asScala.exists(_.getApartmentInfo.getFlatType != FlatType.SECONDARY) shouldBe false
    }

    "search new flats" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFlatType(FlatType.NEW_FLAT)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 3
      result.getItems.asScala.exists(_.getApartmentInfo.getFlatType != FlatType.NEW_FLAT) shouldBe false
    }

    "search new secondary offers" in {
      val searchQuery = new SearchQuery()
      searchQuery.setFlatType(FlatType.NEW_SECONDARY)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 2
      result.getItems.asScala.exists(_.getApartmentInfo.getFlatType != FlatType.NEW_SECONDARY) shouldBe false
    }

  }

  private def buildOffer(offerId: Long, flatType: FlatType): Offer = {
    val offer = offerGen(offerId = offerId).next

    val apartmentInfo = new ApartmentInfo()
    apartmentInfo.setFlatType(flatType)
    offer.setApartmentInfo(apartmentInfo)

    val placementInfo = Placement.newBuilder().setPayed(true).build()
    offer.setPlacementInfo(placementInfo)

    offer
  }
}
