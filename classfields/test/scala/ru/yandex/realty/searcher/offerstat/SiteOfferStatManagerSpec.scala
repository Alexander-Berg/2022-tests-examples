package ru.yandex.realty.searcher.offerstat

import akka.http.scaladsl.server.directives.RouteDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.akka.http.PlayJsonSupport
import ru.yandex.realty.api.ProtoResponse
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.context.v2.{AuctionResultStorage, CatBoostModelStorage}
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.core.Node
import ru.yandex.realty.model.auction.{AuctionResult, DeveloperOfferFilter}
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.model.sites._
import ru.yandex.realty.proto.search.SiteOfferStat
import ru.yandex.realty.search.common.request.domain.PaidOnlyType
import ru.yandex.realty.search.offerstat.SiteOfferStatQuery
import ru.yandex.realty.searcher.context.{SearchContext, SearchContextImpl, SearchContextProvider}
import ru.yandex.realty.searcher.controllers.offerstat.{DefaultSiteOfferStatManager, SiteOfferStatManager}
import ru.yandex.realty.searcher.personalization.persistence.PersonalizationApi
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.realty.storage.CurrencyStorage
import ru.yandex.vertis.generators.ProducerProvider

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

@RunWith(classOf[JUnitRunner])
class SiteOfferStatManagerSpec
  extends AsyncSpecBase
  with Matchers
  with ProducerProvider
  with ScalatestRouteTest
  with RouteDirectives
  with PlayJsonSupport {

  implicit val ex: ExecutionContextExecutor = ExecutionContext.global

  "SiteOfferStatManager " should {

    "return empty response when sources are empty" in new EmptyResponseFixture {

      val result: ProtoResponse.SiteOfferStatResponse = siteOfferStatManager
        .getSiteOfferStatAsync(
          new SiteOfferStatQuery(
            siteId = siteId,
            primarySale = Some(true),
            rgid = Some(NodeRgid.MOSCOW_AND_MOS_OBLAST)
          )
        )
        .futureValue
      result.getResponse.getEntireStats.getPrimarySale.getAllOffersCount shouldBe 0
      result.getResponse.getEntireStats.getNonPrimarySale.getAllOffersCount shouldBe 0
    }

    "return trivial one-offer search response" in new ResponseFixture {

      val result: ProtoResponse.SiteOfferStatResponse = siteOfferStatManager
        .getSiteOfferStatAsync(
          new SiteOfferStatQuery(
            siteId = siteId,
            primarySale = Some(true),
            rgid = Some(NodeRgid.MOSCOW_AND_MOS_OBLAST)
          )
        )
        .futureValue

      result.hasError should be(false)
      result.hasResponse should be(true)

      val response: SiteOfferStat = result.getResponse
      response.hasAllFilters should be(true)
      response.hasDynamicFilters should be(true)
      response.getEntireStats.getPrimarySale.getAllOffersCount shouldBe 3
      response.getEntireStats.getNonPrimarySale.getAllOffersCount shouldBe 3
    }
  }

  trait SiteOfferStatManagerFixture {
    val companyId = 4242L
    val nbid = 17L
    val siteId: Long = 2597869L

    val entireStats: SiteOfferStat.EntireStats = SiteOfferStat.EntireStats
      .newBuilder()
      .setPrimarySale(SiteOfferStat.EntireStats.Tally.newBuilder().setAllOffersCount(3))
      .setNonPrimarySale(SiteOfferStat.EntireStats.Tally.newBuilder().setAllOffersCount(3))
      .build()

    val primaryOfferStats: SiteOfferStat.PrimaryOfferStats = SiteOfferStat.PrimaryOfferStats
      .newBuilder()
      .setTotal(SiteOfferStat.StatItem.newBuilder().setOffersCount(3))
      .build()

    val nonPrimaryOfferStats: SiteOfferStat.NonPrimaryOfferStats = SiteOfferStat.NonPrimaryOfferStats
      .newBuilder()
      .setTotal(SiteOfferStat.StatItem.newBuilder().setOffersCount(3))
      .build()

    val regionGraphMock: RegionGraph = mock[RegionGraph]

    val regionGraphProvider: Provider[RegionGraph] = () => regionGraphMock
    val currencyStorage = new CurrencyStorage(List.empty.asJava, List.empty.asJava, regionGraphProvider)
    val sitesServiceMock: SitesGroupingService = mock[SitesGroupingService]
    val auctionResultProviderMock: Provider[AuctionResultStorage] = mock[Provider[AuctionResultStorage]]
    val catBoostModelStorage = new CatBoostModelStorage(Seq.empty)
    val relevanceModelProvider: ProviderAdapter[CatBoostModelStorage] = ProviderAdapter.create(catBoostModelStorage)
    val personalizationApiMock: PersonalizationApi = mock[PersonalizationApi]
    val currencyProviderMock: Provider[CurrencyStorage] = () => currencyStorage

    val searchContextProviderMock: SearchContextProvider[SearchContextImpl] =
      mock[SearchContextProvider[SearchContextImpl]]

    val mskAndMo = new Node
    mskAndMo.setGeoId(Regions.MSK_AND_MOS_OBLAST)
    mskAndMo.setId(NodeRgid.MOSCOW_AND_MOS_OBLAST)

    (regionGraphMock
      .getNodeByGeoId(_: Int))
      .expects(0)
      .anyNumberOfTimes()
      .returning(None.orNull)

    (regionGraphMock
      .getNodeByGeoId(_: Int))
      .expects(Regions.MSK_AND_MOS_OBLAST)
      .anyNumberOfTimes()
      .returning(mskAndMo)

    (regionGraphMock
      .getNodeById(_: Long))
      .expects(*)
      .anyNumberOfTimes()
      .returning(mskAndMo)

    val dummySite = new Site(siteId)
    val location = new Location()
    location.setRegionGraphId(NodeRgid.MOSCOW_AND_MOS_OBLAST)
    dummySite.setLocation(location)

    (sitesServiceMock
      .getSiteById(_: Long))
      .expects(siteId)
      .anyNumberOfTimes()
      .returning(dummySite)

    (auctionResultProviderMock.get _)
      .expects()
      .anyNumberOfTimes()
      .returning(
        new AuctionResultStorage(
          Seq(
            AuctionResult(
              siteId,
              Vector.empty,
              None,
              new ExtendedSiteStatisticsAtom(SimpleSiteStatisticsResult.EMPTY),
              None,
              Some(DeveloperOfferFilter(companyId, 2)),
              1L
            )
          )
        )
      )

    val siteOfferStatManager: SiteOfferStatManager =
      new DefaultSiteOfferStatManager(
        searchContextProviderMock,
        sitesServiceMock,
        auctionResultProviderMock,
        regionGraphProvider,
        relevanceModelProvider,
        currencyProviderMock,
        personalizationApiMock
      )

  }

  trait EmptyResponseFixture extends SiteOfferStatManagerFixture {
    (searchContextProviderMock
      .doWithContext(_: SearchContext => SiteOfferStat))
      .expects(*)
      .anyNumberOfTimes()
      .returning(SiteOfferStat.getDefaultInstance)
  }

  trait ResponseFixture extends SiteOfferStatManagerFixture {

    val siteOfferStat: SiteOfferStat = SiteOfferStat
      .newBuilder()
      .setEntireStats(entireStats)
      .setPrimarySaleStats(primaryOfferStats)
      .setNonPrimarySaleStats(nonPrimaryOfferStats)
      .build()

    (searchContextProviderMock
      .doWithContext(_: SearchContext => SiteOfferStat))
      .expects(*)
      .anyNumberOfTimes()
      .returning(siteOfferStat)
  }
}
