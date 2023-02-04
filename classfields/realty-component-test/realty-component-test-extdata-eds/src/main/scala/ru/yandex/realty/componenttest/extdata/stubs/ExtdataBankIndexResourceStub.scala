package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.extdata.core.DataType
import ru.yandex.realty.application.providers.RegionGraphComponents
import ru.yandex.realty.application.providers.bank.BanksComponents
import ru.yandex.realty.componenttest.extdata.core.{ComponentTestExtdataControllerProvider, ExtdataResourceStub}
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty2.extdataloader.loaders.bank.BankIndexFetcher

import scala.concurrent.duration._

trait ExtdataBankIndexResourceStub extends ExtdataResourceStub {
  self: ComponentTestExtdataControllerProvider with RegionGraphComponents with BanksComponents =>

  private val dataType: DataType = RealtyDataType.BankIndex

  stubFromFetcher(
    dataType,
    new BankIndexFetcher(
      controller,
      period = 1.hour,
      keepVersions = 1,
      regionGraphProvider = regionGraphProvider,
      banksProvider = banksProvider,
      indexDir = dataIndexPath(dataType).toFile
    )
  )

}
