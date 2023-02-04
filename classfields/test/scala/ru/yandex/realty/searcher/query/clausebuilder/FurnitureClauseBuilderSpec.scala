package ru.yandex.realty.searcher.query.clausebuilder

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.offer.{ApartmentImprovements, Offer}
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.search.clause.{NRTIndexFixture, NRTIndexOfferGenerator}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class FurnitureClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new FurnitureClauseBuilder())
  private val offers: Seq[Offer] =
    Seq(
      Some(ApartmentImprovements.ROOM_FURNITURE),
      Some(ApartmentImprovements.ROOM_FURNITURE),
      Some(ApartmentImprovements.KITCHEN_FURNITURE),
      Some(ApartmentImprovements.KITCHEN_FURNITURE),
      Some(ApartmentImprovements.NO_FURNITURE),
      Some(ApartmentImprovements.KITCHEN_FURNITURE),
      Some(ApartmentImprovements.NO_FURNITURE),
      Some(ApartmentImprovements.ROOM_FURNITURE),
      None,
      None,
      None
    ).zipWithIndex
      .map {
        case (apartmentImprovement, index) => buildOffer(index, apartmentImprovement)
      }
  insertOffers(offers)

  "FurnitureClauseBuilder" should {

    "build clause with furniture" in {
      val searchQuery = new SearchQuery()
      searchQuery.setHasFurniture(true)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 6
    }

    "build clause without furniture" in {
      val searchQuery = new SearchQuery()
      searchQuery.setHasFurniture(false)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 2
    }
  }

  private def buildOffer(offerId: Long, apartmentImprovementOpt: Option[ApartmentImprovements]): Offer = {
    val offer = offerGen(offerId = offerId).next

    apartmentImprovementOpt.foreach(apartmentImprovement => {
      offer.getApartmentInfo.setApartmentImprovements(
        Map(apartmentImprovement -> Boolean.box(true)).asJava
      )
    })

    offer
  }
}
