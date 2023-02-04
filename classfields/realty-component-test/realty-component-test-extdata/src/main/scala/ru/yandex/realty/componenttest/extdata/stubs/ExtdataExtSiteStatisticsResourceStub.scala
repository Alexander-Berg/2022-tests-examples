package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.extsitestatistics.ExtendedSiteStatistics
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.ExtendedSiteStatisticsEntryMessage

trait ExtdataExtSiteStatisticsResourceStub extends ExtdataResourceStub {

  private val extSiteStatistics: Seq[ExtendedSiteStatisticsEntryMessage] = {
    ExtendedSiteStatistics.all
  }

  stubGzipped(RealtyDataType.ExtSiteStatistics, extSiteStatistics)

}
