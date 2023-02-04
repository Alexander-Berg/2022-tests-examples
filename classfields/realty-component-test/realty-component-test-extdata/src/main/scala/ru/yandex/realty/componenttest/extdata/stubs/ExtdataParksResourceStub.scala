package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.geometry.GeometryType
import ru.yandex.realty.model.message.ExtDataSchema.LandmarkMessage
import ru.yandex.realty.model.message.ExtDataSchema.LandmarkMessage.LandmarkType
import ru.yandex.realty.model.message.RealtySchema.{GeometryMessage, PolygonMessage}
import ru.yandex.realty.model.serialization.RealtySchemaVersions

import scala.collection.JavaConverters._

trait ExtdataParksResourceStub extends ExtdataResourceStub {

  private val parks: Seq[LandmarkMessage] = {
    Seq(
      LandmarkMessage
        .newBuilder()
        .setId(121377502)
        .setName("Цаговский лес")
        .setAddress("город Жуковский")
        .setLandmarkType(LandmarkType.PARK)
        .setGeometry(
          GeometryMessage
            .newBuilder()
            .setVersion(RealtySchemaVersions.GEOMETRY_VERSION)
            .setType(GeometryType.POLYGON.value())
            .setPolygon(
              PolygonMessage
                .newBuilder()
                .setVersion(RealtySchemaVersions.POLYGON_VERSION)
                .addAllLatitude(Seq(67.82569f, 67.82612f, 67.82634f).map(Float.box).asJava)
                .addAllLongitude(Seq(38.295578f, 38.297096f, 38.298275f).map(Float.box).asJava)
                .build()
            )
            .build()
        )
        .build()
    )
  }

  stubGzipped(RealtyDataType.Parks, parks)

}
