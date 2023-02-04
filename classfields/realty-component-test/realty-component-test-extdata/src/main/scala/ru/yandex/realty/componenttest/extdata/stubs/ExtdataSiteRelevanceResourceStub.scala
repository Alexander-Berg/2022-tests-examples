package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.sites.Site_57547
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.ExtDataSiteRelevance

trait ExtdataSiteRelevanceResourceStub extends ExtdataResourceStub {

  private val siteRelevances: Seq[ExtDataSiteRelevance] =
    Seq(
      ExtDataSiteRelevance
        .newBuilder()
        .setSiteId(Site_57547.Id)
        .setRelevance(1180)
        .build()
    )

  stubGzipped(RealtyDataType.SiteRelevance, siteRelevances)

}
