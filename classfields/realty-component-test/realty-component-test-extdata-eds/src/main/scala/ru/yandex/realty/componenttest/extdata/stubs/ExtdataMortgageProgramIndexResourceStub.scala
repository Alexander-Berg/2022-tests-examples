package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.extdata.core.DataType
import ru.yandex.realty.application.providers.RegionGraphComponents
import ru.yandex.realty.application.providers.bank.BanksComponents
import ru.yandex.realty.application.providers.site.SitesServiceComponents
import ru.yandex.realty.componenttest.extdata.core.{
  ComponentTestExtdataControllerProvider,
  ComponentTestSearchContextComponents,
  ExtdataResourceStub
}
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty2.extdataloader.loaders.mortgage.MortgageProgramIndexFetcher

import scala.concurrent.duration._

trait ExtdataMortgageProgramIndexResourceStub extends ExtdataResourceStub {
  self: ComponentTestExtdataControllerProvider
    with RegionGraphComponents
    with BanksComponents
    with SitesServiceComponents
    with ComponentTestSearchContextComponents =>

  private val dataType: DataType = RealtyDataType.MortgageProgramIndex

  stubFromFetcher(
    dataType,
    new MortgageProgramIndexFetcher(
      controller,
      period = 1.hour,
      keepVersions = 1,
      regionGraphProvider = regionGraphProvider,
      banksProvider = banksProvider,
      sitesService = sitesService,
      indexDir = dataIndexPath(dataType).toFile
    )
  )

}
