package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.extdata.core.event.Event
import ru.yandex.realty.application.providers.RegionGraphComponents
import ru.yandex.realty.componenttest.extdata.core.{
  ComponentTestExtdataControllerProvider,
  ComponentTestSearchContextComponents,
  ExtdataResourceStub
}
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.context.v2.RegionGraphProviderUpdated
import ru.yandex.realty2.extdataloader.loaders.ListeningLuceneIndexFetcher
import ru.yandex.realty2.extdataloader.loaders.lucene.readers.RegionDocumentsStatisticsIndexReader

import scala.collection.JavaConverters._

trait ExtdataRegionDocumentsStatisticsResourceStub extends ExtdataResourceStub {
  self: ComponentTestExtdataControllerProvider with ComponentTestSearchContextComponents with RegionGraphComponents =>

  stubFromFetcher(
    RealtyDataType.RegionDocumentsStatistics,
    new ListeningLuceneIndexFetcher(
      controller = controller,
      searchContextProvider = searchContextProvider,
      luceneIndexReader = new RegionDocumentsStatisticsIndexReader(regionGraphProvider),
      dataType = RealtyDataType.RegionDocumentsStatistics,
      events = Set[Event](RegionGraphProviderUpdated).asJava
    )
  )

}
