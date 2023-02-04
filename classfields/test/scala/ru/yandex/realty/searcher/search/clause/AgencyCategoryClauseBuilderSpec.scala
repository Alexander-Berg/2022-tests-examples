package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.experiments.AllSupportedExperiments
import ru.yandex.realty.model.offer.{Offer, SalesAgentCategory}
import ru.yandex.realty.proto.unified.offer.rent.YandexRentInfo
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.AgencyCategoryClauseBuilder
import ru.yandex.realty.searcher.search.clause.AgencyCategoryClauseBuilderSpec._

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class AgencyCategoryClauseBuilderSpec extends SpecBase with NRTIndexFixture {

  private val clauseBuilders = Seq(new AgencyCategoryClauseBuilder())
  insertOffers(offers)

  "AgencyCategoryClauseBuilder" should {
    "search offers from agencies if agents = true" in {
      val searchQuery = new SearchQuery()
      searchQuery.setAgents(true)
      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 3
      result.getItems.asScala.map(_.getLongId).toSet shouldBe Set(1L, 2L, 8L)
    }

    "search offers from owners if agents = false" in {
      val searchQuery = new SearchQuery()
      searchQuery.setAgents(false)
      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 1
      result.getItems.asScala.map(_.getLongId).toSet shouldBe Set(3L)
    }

    "search yandex rent offers if agents = false" in {
      val searchQuery = new SearchQuery()
      searchQuery.setAgents(false)
      searchQuery.addExpFlag(AllSupportedExperiments.YA_RENT_OWNER_FILTER)
      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 2
      result.getItems.asScala.map(_.getLongId).toSet shouldBe Set(3L, 8L)
    }

    "does not filter offers if agents = null" in {
      val searchQuery = new SearchQuery()
      val result = search(searchQuery, clauseBuilders)

      result.getTotal shouldBe 0
    }
  }
}

object AgencyCategoryClauseBuilderSpec extends NRTIndexOfferGenerator {

  val agencyOffer = buildOffer(1, SalesAgentCategory.AGENCY)
  val agentOffer = buildOffer(2, SalesAgentCategory.AGENT)
  val ownerOffer = buildOffer(3, SalesAgentCategory.OWNER)
  val adAgencyOffer = buildOffer(4, SalesAgentCategory.AD_AGENCY)
  val developerOffer = buildOffer(5, SalesAgentCategory.DEVELOPER)
  val privateAgentOffer = buildOffer(6, SalesAgentCategory.PRIVATE_AGENT)
  val verifierOffer = buildOffer(7, SalesAgentCategory.VERIFIER)
  val yaRentOffer = buildOffer(8, SalesAgentCategory.AGENT, yaRent = true)

  val offers = Seq(
    agencyOffer,
    agentOffer,
    ownerOffer,
    adAgencyOffer,
    developerOffer,
    privateAgentOffer,
    verifierOffer,
    yaRentOffer
  )

  def buildOffer(offerId: Long, salesAgentCategory: SalesAgentCategory, yaRent: Boolean = false): Offer = {
    val offer = offerGen(offerId = offerId).next
    offer.getSaleAgent.setCategory(salesAgentCategory)
    if (yaRent) {
      offer.setYandexRentInfo(YandexRentInfo.getDefaultInstance)
    }
    offer
  }
}
