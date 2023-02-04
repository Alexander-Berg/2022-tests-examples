package ru.yandex.realty.searcher.response.builders.inexact.absent

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.model.offer.{SaleAgent, SalesAgentCategory}
import ru.yandex.realty.proto.search.inexact.{AgentInexact, InexactMatching}
import ru.yandex.realty.proto.unified.offer.contact.ContactInfo
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.response.builders.inexact.{
  OfferBuilderContext,
  OfferBuilderContextFixture,
  ProtoHelper
}
import ru.yandex.vertis.protobuf.ProtoInstanceProvider._

class AgentInexactEnricherSpec extends WordSpec with Matchers {

  import ProtoHelper._

  val absentInexact =
    """{
      |  absent:{}
      |
      |  }""".stripMargin
      .toProto[AgentInexact]

  val defaultInexact = AgentInexact.getDefaultInstance

  private def getSalesAgentCategoryInOffer(category: SalesAgentCategory): OfferBuilderContext = {
    new OfferBuilderContextFixture {
      val saleAgent = new SaleAgent(ContactInfo.newBuilder())
      saleAgent.setCategory(category)
      (dummyOfferBuilderContext.offer.getSaleAgent _).expects().returning(saleAgent).anyNumberOfTimes()
    }.dummyOfferBuilderContext
  }

  val unknownSalesAgencyCategoryInOffer =
    getSalesAgentCategoryInOffer(SalesAgentCategory.UNKNOWN)

  val ownerSalesAgencyCategoryInOffer =
    getSalesAgentCategoryInOffer(SalesAgentCategory.OWNER)

  val agencySalesAgencyCategoryInOffer =
    getSalesAgentCategoryInOffer(SalesAgentCategory.AGENCY)

  "AgentInexactEnricherSpec" should {

    "checkAndEnrich when getAgents == true" in new OfferBuilderContextFixture {
      val searchQuery = new SearchQuery()
      searchQuery.setAgents(true)
      val testData =
        Table(
          ("context", "expected"),
          (unknownSalesAgencyCategoryInOffer, absentInexact),
          (ownerSalesAgencyCategoryInOffer, absentInexact),
          (agencySalesAgencyCategoryInOffer, defaultInexact)
        )

      val agentInexactEnricher = new AgentInexactEnricher(searchQuery)

      forAll(testData) { (context: OfferBuilderContext, expected: AgentInexact) =>
        val builder = InexactMatching.newBuilder()
        agentInexactEnricher.checkAndEnrich(builder, context).getAgents shouldBe expected
      }
    }

    "checkAndEnrich when getAgents == false" in new OfferBuilderContextFixture {
      val searchQuery = new SearchQuery()
      searchQuery.setAgents(false)

      val testData =
        Table(
          ("context", "expected"),
          (unknownSalesAgencyCategoryInOffer, absentInexact),
          (ownerSalesAgencyCategoryInOffer, defaultInexact),
          (agencySalesAgencyCategoryInOffer, absentInexact)
        )

      val agentInexactEnricher = new AgentInexactEnricher(searchQuery)

      forAll(testData) { (context: OfferBuilderContext, expected: AgentInexact) =>
        val builder = InexactMatching.newBuilder()
        agentInexactEnricher.checkAndEnrich(builder, context).getAgents shouldBe expected
      }
    }

  }
}
