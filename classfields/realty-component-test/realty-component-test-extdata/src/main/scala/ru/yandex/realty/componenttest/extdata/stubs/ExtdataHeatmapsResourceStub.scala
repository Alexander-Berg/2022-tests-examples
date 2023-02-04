package ru.yandex.realty.componenttest.extdata.stubs

import com.google.protobuf.Int32Value
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.model.message.ExtDataSchema.{
  ColorMessage,
  GeoGridLevelMessage,
  GeoGridMessage,
  GeoGridSettingsMessage,
  HeatmapMessage,
  RegionalHeatmapMessage
}
import ru.yandex.realty.model.serialization.RealtySchemaVersions

import scala.collection.JavaConverters._

trait ExtdataHeatmapsResourceStub extends ExtdataResourceStub {

  private val heatmaps: Seq[HeatmapMessage] = {
    Seq(
      HeatmapMessage
        .newBuilder()
        .setVersion(RealtySchemaVersions.HEATMAP_VERSION)
        .setName("infrastructure")
        .addRegionalHeatmaps(
          RegionalHeatmapMessage
            .newBuilder()
            .setVersion(RealtySchemaVersions.HEATMAP_VERSION)
            .setGrid(
              GeoGridMessage
                .newBuilder()
                .setVersion(RealtySchemaVersions.HEATMAP_GEOGRID_VERSION)
                .setLatitude(54.252f)
                .setLongitude(36.85573f)
                .setStepLatitude(0.0022487578f)
                .setStepLongitude(0.0039993697f)
                .setRows(2)
                .setCols(2)
                .addAllGrid(Seq(0, 0, 0, 0).map(Int.box).asJava)
                .setGeoId(1)
                .build()
            )
            .setSettings(
              GeoGridSettingsMessage
                .newBuilder()
                .setVersion(RealtySchemaVersions.HEATMAP_REGIONAL_SETTINGS_VERSION)
                .setName("Инфраструктура")
                .setType("infrastructure")
                .setGeoId(1)
                .addLevels(
                  GeoGridLevelMessage
                    .newBuilder()
                    .setVersion(RealtySchemaVersions.HEATMAP_REGIONAL_SETTINGS_VERSION)
                    .setLevel(1)
                    .setColor(
                      ColorMessage
                        .newBuilder()
                        .setRed(238)
                        .setGreen(70)
                        .setBlue(19)
                        .setAlpha(Int32Value.of(102))
                        .build()
                    )
                    .setDescription("минимальная")
                    .build()
                )
                .build()
            )
            .build()
        )
        .build()
    )
  }

  stubGzipped(RealtyDataType.Heatmaps, heatmaps)

}
