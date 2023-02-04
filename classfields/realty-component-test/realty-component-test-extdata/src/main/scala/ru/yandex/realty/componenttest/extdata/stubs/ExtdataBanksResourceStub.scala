package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.banks.Banks
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType

trait ExtdataBanksResourceStub extends ExtdataResourceStub {

  stubGzipped(RealtyDataType.Banks, Banks.all)

}
