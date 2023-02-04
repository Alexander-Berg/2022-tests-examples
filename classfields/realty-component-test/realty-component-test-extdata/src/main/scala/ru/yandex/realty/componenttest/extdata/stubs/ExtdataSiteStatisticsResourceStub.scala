package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.sitestatistics.SiteStatistics
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.SiteStatisticsStorageEntryMessage

trait ExtdataSiteStatisticsResourceStub extends ExtdataResourceStub {

  private val siteStatistics: Seq[SiteStatisticsStorageEntryMessage] = {
    SiteStatistics.all
  }

  stubGzipped(RealtyDataType.SiteStatistics, siteStatistics)

}
