package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.extdata.core.ExtdataReplicatedResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType

trait ExtdataVerba2ResourceStub extends ExtdataReplicatedResourceStub {

  stubGzipFromResources(RealtyDataType.Verba2, "/verba/verba2-3.xml")

}
