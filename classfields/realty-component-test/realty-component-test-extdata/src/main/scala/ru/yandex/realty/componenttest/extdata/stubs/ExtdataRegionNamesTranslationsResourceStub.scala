package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.extdata.core.ExtdataReplicatedResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType

trait ExtdataRegionNamesTranslationsResourceStub extends ExtdataReplicatedResourceStub {

  stubFromTestingResourceService(RealtyDataType.RegionNamesTranslations)
}
