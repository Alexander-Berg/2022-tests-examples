package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.DeveloperGeoStatistic

trait ExtdataDeveloperGeoStatisticResourceStub extends ExtdataResourceStub {

  private val developerGeoStatistics: Seq[DeveloperGeoStatistic] = Seq(
    DeveloperGeoStatistic
      .newBuilder()
      .setDeveloperId(671755)
      .setRgid(475530)
      .setGeoId(10650)
      .setSubjectFederationName("Брянская")
      .build()
  )

  stubGzipped(RealtyDataType.DeveloperGeoStatistic, developerGeoStatistics)

}
