package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.extdata.core.{DataType, ServerController}
import ru.yandex.realty.application.providers.{ParkComponents, PondComponents, RegionGraphComponents}
import ru.yandex.realty.application.providers.village.{VillageComponents, VillageDynamicInfoComponents}
import ru.yandex.realty.componenttest.extdata.core.{
  ComponentTestExtdataControllerProvider,
  ComponentTestSearchContextComponents,
  ExtdataResourceStub
}
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty2.extdataloader.loaders.village.VillageIndexFetcher

trait ExtdataVillageIndexResourceStub extends ExtdataResourceStub {
  self: ComponentTestExtdataControllerProvider
    with ComponentTestSearchContextComponents
    with VillageComponents
    with VillageDynamicInfoComponents
    with RegionGraphComponents
    with ParkComponents
    with PondComponents =>

  private val dataType: DataType = RealtyDataType.VillageIndex

  stubFromFetcher(
    dataType,
    new VillageIndexFetcher(
      controller.asInstanceOf[ServerController],
      villageStorageProvider,
      villageDynamicInfoProvider,
      regionGraphProvider,
      pondStorageProvider,
      parkStorageProvider,
      searchContextProvider,
      indexDir = dataIndexPath(dataType).toFile
    )
  )

}
