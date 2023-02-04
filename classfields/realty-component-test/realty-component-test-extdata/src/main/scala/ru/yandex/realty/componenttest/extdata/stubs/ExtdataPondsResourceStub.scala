package ru.yandex.realty.componenttest.extdata.stubs

import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.geometry.GeometryType
import ru.yandex.realty.model.message.ExtDataSchema.LandmarkMessage
import ru.yandex.realty.model.message.ExtDataSchema.LandmarkMessage.LandmarkType
import ru.yandex.realty.model.message.RealtySchema.{GeometryMessage, PolygonMessage}
import ru.yandex.realty.model.serialization.RealtySchemaVersions

import scala.collection.JavaConverters._

trait ExtdataPondsResourceStub extends ExtdataResourceStub {

  private val ponds: Seq[LandmarkMessage] = {
    Seq(
      LandmarkMessage
        .newBuilder()
        .setId(141086712)
        .setName("река Сесью")
        .setAddress("сельское поселение Ловозеро")
        .setLandmarkType(LandmarkType.POND)
        .setGeometry(
          GeometryMessage
            .newBuilder()
            .setVersion(RealtySchemaVersions.GEOMETRY_VERSION)
            .setType(GeometryType.POLYGON.value())
            .setPolygon(
              PolygonMessage
                .newBuilder()
                .setVersion(RealtySchemaVersions.POLYGON_VERSION)
                .addAllLatitude(Seq(67.82547f, 67.82548f, 67.82538f).map(Float.box).asJava)
                .addAllLongitude(Seq(38.28928f, 38.290516f, 38.292297f).map(Float.box).asJava)
                .build()
            )
            .build()
        )
        .build()
    )
  }

  stubGzipped(RealtyDataType.Ponds, ponds)

}
