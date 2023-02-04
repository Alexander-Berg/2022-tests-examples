package ru.yandex.realty.searcher.search.clause

import org.apache.lucene.search.{BooleanQuery, Query, Sort}
import org.scalamock.scalatest.MockFactory
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.graph.{DocumentBuilderHelper, RegionGraph}
import ru.yandex.realty.lucene.OfferDocumentFields.ID
import ru.yandex.realty.lucene.QueryUtil.newTerm
import ru.yandex.realty.lucene.{ProtoLuceneDocumentBuilder, ProtoLuceneDocumentConverter}
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.model.sites.ExtendedSiteStatistics
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.search.common.request.domain.SearchQuery
import ru.yandex.realty.searcher.nrt.{NRTIndex, NRTIndexFactory}
import ru.yandex.realty.searcher.query.{ConstantSortProvider, QueryProvider, SortSupport}
import ru.yandex.realty.searcher.query.clausebuilder.{ClauseBuilder, RoomsRangeClauseBuilder, SiteIdClauseBuilder}
import ru.yandex.realty.searcher.{SearchHelper, SearchResult}
import ru.yandex.realty.sites.campaign.CampaignStorage
import ru.yandex.realty.sites.{CampaignService, ExtendedSiteStatisticsStorage, SitesGroupingService}
import ru.yandex.realty.storage.{CurrencyStorage, ParkStorage, PondStorage}
import ru.yandex.realty.util.lucene.DocumentReaderUtils
import ru.yandex.vertis.ops.test.TestOperationalSupport

import java.nio.file.{Files, Path}
import scala.collection.JavaConverters._

trait NRTIndexFixture extends MockFactory with RegionGraphTestComponents {

  val indexDir: Path = Files.createTempDirectory("clauseBuilders")
  val index: NRTIndex = NRTIndexFactory.createNRTIndexWithOps(indexDir)(TestOperationalSupport)
  lazy val regionGraph: RegionGraph = regionGraphProvider.get()
  val documentBuilderHelper = new DocumentBuilderHelper(regionGraphProvider)
  val sitesService: SitesGroupingService = buildSitesGroupingService
  val extStatisticsProvider: Provider[ExtendedSiteStatisticsStorage] = mock[Provider[ExtendedSiteStatisticsStorage]]
  val campaignProvider: Provider[CampaignStorage] = mock[Provider[CampaignStorage]]
  val campaignService = new CampaignService(campaignProvider)
  val pondStorageProvider: Provider[PondStorage] = mock[Provider[PondStorage]]
  val parkStorageProvider: Provider[ParkStorage] = mock[Provider[ParkStorage]]
  val baseUrl = "//localhost:80"
  val mdsUrlBuilder = new MdsUrlBuilder(baseUrl)
  lazy val pondsStorage = PondStorage.empty()
  lazy val parksStorage = ParkStorage.empty()

  protected def buildSitesGroupingService: SitesGroupingService = {
    val sitesService: SitesGroupingService = mock[SitesGroupingService]
    (sitesService.getSiteById _).expects(*).anyNumberOfTimes().returning(null)
    (sitesService.getHouseById _).expects(*).anyNumberOfTimes().returning(null)
    (sitesService.getPhaseById _).expects(*).anyNumberOfTimes().returning(null)
    sitesService
  }

  (extStatisticsProvider.get _)
    .expects()
    .anyNumberOfTimes()
    .returning(new ExtendedSiteStatisticsStorage(Map[java.lang.Long, ExtendedSiteStatistics]().asJava))
  (campaignProvider.get _).expects().anyNumberOfTimes().returning(new CampaignStorage(Seq.empty.asJava))
  (pondStorageProvider.get _).expects().anyNumberOfTimes().returning(pondsStorage)
  (parkStorageProvider.get _).expects().anyNumberOfTimes().returning(parksStorage)

  val documentBuilder =
    new ProtoLuceneDocumentBuilder(
      regionGraphProvider,
      documentBuilderHelper,
      sitesService,
      extStatisticsProvider,
      campaignService,
      pondStorageProvider,
      parkStorageProvider,
      mdsUrlBuilder
    )

  def insertOffers(offers: Seq[Offer]): Unit = {
    offers.map(documentBuilder.serialize).map(_.build).map(ProtoLuceneDocumentConverter.convert).foreach { upsert =>
      val id = DocumentReaderUtils.longValue(upsert, ID)
      index.upsert(newTerm(ID, id), upsert)
    }
    index.hardCommit()
    index.softCommit()
  }

  def search(searchQuery: SearchQuery, clauseBuilders: Seq[ClauseBuilder]): SearchResult[Offer] = {
    val queryProvider = new SimpleQueryProvider(searchQuery, clauseBuilders)
    index.doWithContext { context =>
      SearchHelper.searchAll(context.indexSearcher, queryProvider)
    }
  }

  def searchWithSort(
    searchQuery: SearchQuery,
    clauseBuilders: Seq[ClauseBuilder],
    limit: Int
  ): SearchResult[Offer] = {
    val queryProvider = new SimpleQueryProvider(searchQuery, clauseBuilders)
    val currencyStorage = new CurrencyStorage(Seq.empty.asJava, Seq.empty.asJava, regionGraphProvider)
    val sort = SortSupport.buildSort(searchQuery.getSort, searchQuery, currencyStorage, regionGraph)

    val sortProvider = new ConstantSortProvider(sort)
    index.doWithContext { context =>
      SearchHelper.search(context.indexSearcher, queryProvider, sortProvider, 0, limit)
    }
  }
}

class SimpleQueryProvider(searchQuery: SearchQuery, clauseBuilders: Seq[ClauseBuilder]) extends QueryProvider {

  override def buildQuery(): Query = {
    val booleanQuery = new BooleanQuery()
    for {
      clauseBuilder <- clauseBuilders
      query <- Option(clauseBuilder.createSubQuery(searchQuery))
    } yield booleanQuery.add(query, clauseBuilder.getOccur(searchQuery))
    booleanQuery
  }
}
