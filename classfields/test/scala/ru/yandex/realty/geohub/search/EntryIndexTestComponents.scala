package ru.yandex.realty.geohub.search

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{DirectoryReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.RAMDirectory
import org.scalamock.scalatest.MockFactory
import ru.yandex.common.util.collections.MultiMap
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.application.providers.EntryIndexComponents
import ru.yandex.realty.commercial.{CommercialBuildingStorage, CommercialBuildingStorageImpl}
import ru.yandex.realty.entry.EntryDocumentsBuilder
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.microdistricts.MicroDistrictsTestComponents
import ru.yandex.realty.railway.RailwayStationsTestComponents
import ru.yandex.realty.searcher.context.{SearchContext, SearchContextImpl, SearchContextProvider}
import ru.yandex.realty.sites.SitesServiceTestComponents
import ru.yandex.realty.storage.PikRegionsStorage
import ru.yandex.realty.util.HighwayLocator
import ru.yandex.realty.villages.{VillagesStorage, VillagesStorageImpl}

import scala.concurrent.Future

trait EntryIndexTestComponents
  extends EntryIndexComponents
  with MockFactory
  with RegionGraphTestComponents
  with SitesServiceTestComponents
  with MicroDistrictsTestComponents
  with RailwayStationsTestComponents {

  val villageProvider: Provider[VillagesStorage] = () => new VillagesStorageImpl(Seq.empty)

  val commercialBuildingsProvider: Provider[CommercialBuildingStorage] = () =>
    new CommercialBuildingStorageImpl(Seq.empty)
  val highwayLocatorProvider: Provider[HighwayLocator] = () => new HighwayLocator(new MultiMap)
  val pikRegionsProvider: Provider[PikRegionsStorage] = () => new PikRegionsStorage(Map.empty)

  val documentsBuilder = new EntryDocumentsBuilder(
    sitesService,
    villageProvider,
    railwayStationsProvider,
    regionGraphProvider,
    microDistrictsProvider,
    commercialBuildingsProvider,
    highwayLocatorProvider,
    pikRegionsProvider
  )

  val memoryIndex = new RAMDirectory()
  val indexWriter = new IndexWriter(memoryIndex, new IndexWriterConfig(EntryDocumentsBuilder.analyzer))
  documentsBuilder.build().foreach(indexWriter.addDocument)
  indexWriter.close()
  val reader = DirectoryReader.open(memoryIndex)
  val indexSearcher = new IndexSearcher(reader)
  lazy val entryIndexProvider: SearchContextProvider[SearchContext] = new SearchContextProvider[SearchContext] {
    override def doWithContext[U](doWith: SearchContext => U): U = doWith(new SearchContextImpl(indexSearcher))

    override def doWithContextAsync[U](doWith: SearchContext => Future[U]): Future[U] =
      doWith(new SearchContextImpl(indexSearcher))
  }
}
