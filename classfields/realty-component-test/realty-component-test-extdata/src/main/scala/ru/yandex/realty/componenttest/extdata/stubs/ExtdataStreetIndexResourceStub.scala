package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.extdata.core.ExtdataReplicatedResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType

trait ExtdataStreetIndexResourceStub extends ExtdataReplicatedResourceStub {

  stubFromTestingResourceService(RealtyDataType.StreetIndex)

}
