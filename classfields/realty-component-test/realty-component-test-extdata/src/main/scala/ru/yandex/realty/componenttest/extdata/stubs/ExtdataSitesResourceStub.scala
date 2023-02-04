package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.sites.Sites
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.SiteMessage

trait ExtdataSitesResourceStub extends ExtdataResourceStub {

  private val sites: Seq[SiteMessage] =
    Sites.all

  stubGzipped(RealtyDataType.Sites, sites)

}
