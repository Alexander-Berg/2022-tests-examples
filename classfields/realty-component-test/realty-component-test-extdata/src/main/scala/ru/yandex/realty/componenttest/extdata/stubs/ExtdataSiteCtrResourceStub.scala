package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.sites.Site_57547
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType

trait ExtdataSiteCtrResourceStub extends ExtdataResourceStub {

  private val siteCtr: Seq[String] =
    Seq(
      s"${Site_57547.Id}\t4"
    )

  stubLines(RealtyDataType.SiteCtr, siteCtr)

}
