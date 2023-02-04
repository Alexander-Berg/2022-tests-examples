package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.verba.VerbaDescriptions
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType

trait ExtdataVerbaDescriptionsResourceStub extends ExtdataResourceStub {

  stubGzipped(RealtyDataType.VerbaDescriptions, VerbaDescriptions.all)

}
