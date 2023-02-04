package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.data.sites.Site_57547
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.{GeoFactsMessage, GeoFactsStorageEntryMessage}
import ru.yandex.realty.model.serialization.RealtySchemaVersions.GEO_FACTS_VERSION

trait ExtdataGeoFactsResourceStub extends ExtdataResourceStub {

  private val geoFacts: Seq[GeoFactsStorageEntryMessage] = {
    Seq(
      GeoFactsStorageEntryMessage
        .newBuilder()
        .setKey(Site_57547.Id)
        .setValue(
          GeoFactsMessage
            .newBuilder()
            .setVersion(GEO_FACTS_VERSION)
            .build()
        )
        .build()
    )
  }

  stubGzipped(RealtyDataType.GeoFacts, geoFacts)

}
