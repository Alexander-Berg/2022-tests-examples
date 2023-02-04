package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.companies.Companies
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.CompanyMessage

trait ExtdataCompaniesResourceStub extends ExtdataResourceStub {

  private val companies: Seq[CompanyMessage] =
    Companies.all

  stubGzipped(RealtyDataType.Companies, companies)

}
