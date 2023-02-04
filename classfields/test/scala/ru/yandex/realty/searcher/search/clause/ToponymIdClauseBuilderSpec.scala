package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.ToponymIdClauseBuilder
import ru.yandex.realty.searcher.search.clause.ToponymIdClauseBuilderSpec.{offerWithTopoyms, offers}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ToponymIdClauseBuilderSpec extends SpecBase with NRTIndexFixture {
  private val clauseBuilders = Seq(new ToponymIdClauseBuilder())
  insertOffers(offers)

  "ToponymIdClauseBuilder" should {
    "do not search offers without toponyms" in {
      val searchQuery = new SearchQuery()

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 0
    }

    "correctly search offers without toponyms" in {
      val searchQuery = new SearchQuery()
      searchQuery.setToponymId(12L)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 0
    }

    "correctly search offers with single toponym" in {
      val searchQuery = new SearchQuery()
      searchQuery.setToponymId(10L)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      result.getItems.get(0).getId shouldBe offerWithTopoyms.getId
    }

    "correctly search offers with multiple toponyms" in {
      val searchQuery = new SearchQuery()
      searchQuery.setToponymId(10L)
      searchQuery.setToponymId(11L)

      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      result.getItems.get(0).getId shouldBe offerWithTopoyms.getId
    }
  }
}

object ToponymIdClauseBuilderSpec extends NRTIndexOfferGenerator {

  val offerWithoutToponyms = buildOffer(1, Seq.empty)
  val offerWithTopoyms = buildOffer(2, Seq(10, 11))
  val offers = Seq(offerWithoutToponyms, offerWithTopoyms)

  def buildOffer(offerId: Long, toponymIds: Seq[Long]): Offer = {
    val offer = offerGen(offerId = offerId).next
    val location = new Location()
    toponymIds.map(Long.box).foreach(location.addToponymId)
    offer.setLocation(location)
    offer
  }
}
