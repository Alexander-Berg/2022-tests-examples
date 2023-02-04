package ru.yandex.realty.searcher.search.clause

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.graph.DocumentBuilderHelper
import ru.yandex.realty.lucene.ProtoLuceneDocumentBuilder
import ru.yandex.realty.model.offer.{BuildingInfo, Offer}
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.query.clausebuilder.{
  DeveloperIdClauseBuilder,
  FloorClauseBuilder,
  MatchAllDocsClauseBuilder
}
import ru.yandex.realty.sites.SitesGroupingService

import scala.collection.JavaConverters._

/**
  * купить квартира + застройщик - есть 2 кв из разных жк от ПИКа и 2 кв от разных ЖК от Самолета -
  * после фильтрации по самолету кв от ПИКа должны пропасть из выдачи
  */
@RunWith(classOf[JUnitRunner])
class DeveloperIdClauseBuilderSpec extends SpecBase with NRTIndexFixture with NRTIndexOfferGenerator {

  private val clauseBuilders = Seq(new MatchAllDocsClauseBuilder, new DeveloperIdClauseBuilder())

  override def buildSitesGroupingService: SitesGroupingService = {
    val sitesService: SitesGroupingService = mock[SitesGroupingService]
    (sitesService.getHouseById _).expects(*).anyNumberOfTimes().returning(null)
    (sitesService.getPhaseById _).expects(*).anyNumberOfTimes().returning(null)
    addMockSiteDeveloper(sitesService, 1, 1)
    addMockSiteDeveloper(sitesService, 2, 1)
    addMockSiteDeveloper(sitesService, 3, 3)
    addMockSiteDeveloper(sitesService, 4, 3)
    sitesService
  }

  private def addMockSiteDeveloper(sitesService: SitesGroupingService, siteId: Long, developerId: Long): Unit = {
    val s = new Site(siteId)
    s.setBuilders(List(long2Long(developerId)).asJava)
    (sitesService.getSiteById _).expects(siteId).anyNumberOfTimes().returning(s)
  }

  private val offers: Seq[Offer] = Seq(
    buildOffer(1, Some(1)),
    buildOffer(2, Some(2)),
    buildOffer(3, Some(3)),
    buildOffer(4, Some(4)),
    buildOffer(5, None)
  )

  insertOffers(offers)

  "DeveloperIdClauseBuilder" should {

    "search for all offers" in {

      val searchQuery = new SearchQuery()
      val result = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      result.getTotal shouldBe offers.size
    }

    "search offers for particular developer" in {
      val searchQuery = new SearchQuery()
      searchQuery.setDeveloperId(3)

      val result = searchWithSort(searchQuery, clauseBuilders, offers.size * 2)

      result.getItems.asScala.map(offer => offer.getLongId).toSet shouldBe Set(3L, 4L)
    }
  }

  private def buildOffer(offerId: Long, siteId: Option[Long]): Offer = {
    val offer = offerGen(offerId = offerId).next
    if (offer.getBuildingInfo == null) {
      offer.setBuildingInfo(new BuildingInfo())
    }
    offer.getBuildingInfo.setSiteId(null)
    siteId.foreach(s => offer.getBuildingInfo.setSiteId(s))
    offer
  }
}
