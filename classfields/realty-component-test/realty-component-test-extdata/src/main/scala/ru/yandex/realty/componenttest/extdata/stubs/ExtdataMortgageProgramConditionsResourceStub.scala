package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.mortgageprogramconditions.MortgageProgramConditions
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType

trait ExtdataMortgageProgramConditionsResourceStub extends ExtdataResourceStub {

  stubGzipped(RealtyDataType.MortgageProgramConditions, MortgageProgramConditions.all)

}
