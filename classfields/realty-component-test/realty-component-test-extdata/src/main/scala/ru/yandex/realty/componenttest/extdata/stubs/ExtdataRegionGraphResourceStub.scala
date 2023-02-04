package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.extdata.core.ExtdataReplicatedResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType

trait ExtdataRegionGraphResourceStub extends ExtdataReplicatedResourceStub {

  stubFromTestingResourceService(RealtyDataType.RegionGraph)

}
