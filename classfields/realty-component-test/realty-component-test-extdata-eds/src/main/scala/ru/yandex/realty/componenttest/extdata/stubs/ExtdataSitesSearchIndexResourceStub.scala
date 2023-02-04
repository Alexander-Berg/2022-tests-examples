package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.extdata.core.DataType
import ru.yandex.realty.application.CompletedSiteMortgageMatcherComponents
import ru.yandex.realty.application.components.AuctionResultComponents
import ru.yandex.realty.application.providers.site.{ExtSiteStatisticsComponents, SitesServiceComponents}
import ru.yandex.realty.application.providers.{
  CompaniesComponents,
  ExpectedMetroComponents,
  ParkComponents,
  PondComponents,
  RegionGraphComponents
}
import ru.yandex.realty.componenttest.extdata.core.{
  ComponentTestExtdataControllerProvider,
  ComponentTestSearchContextComponents,
  ExtdataResourceStub
}
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty2.extdataloader.loaders.sites.{SiteIndexFetcher, SiteOffersSearcherImpl}

trait ExtdataSitesSearchIndexResourceStub extends ExtdataResourceStub {
  self: ComponentTestExtdataControllerProvider
    with ComponentTestSearchContextComponents
    with SitesServiceComponents
    with AuctionResultComponents
    with ExtSiteStatisticsComponents
    with RegionGraphComponents
    with ExpectedMetroComponents
    with CompaniesComponents
    with CompletedSiteMortgageMatcherComponents
    with PondComponents
    with ParkComponents =>

  private val dataType: DataType = RealtyDataType.NewSiteIndex
  private val siteOffersSearcher = new SiteOffersSearcherImpl(searchContextProvider)

  stubFromFetcher(
    dataType,
    new SiteIndexFetcher(
      controller,
      sitesService,
      siteOffersSearcher,
      auctionResultProvider,
      extStatisticsProvider,
      regionGraphProvider,
      expectedMetroStorageProvider,
      companiesProvider,
      pondStorageProvider,
      parkStorageProvider,
      completedSiteMortgageMatcher,
      indexDir = dataIndexPath(dataType).toFile
    )
  )

}
