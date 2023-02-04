package ru.yandex.realty.model.serialization.heatmap

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.graphics.Color
import ru.yandex.realty.model.heatmap.{GeoGridLevelInfo, GeoGridSettings}
import ru.yandex.realty.model.serialization.RealtySchemaVersions

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class GeoGridSettingsProtoConverterTest extends FlatSpec with Matchers {

  "GeoGridSettingsProtoConverter" should "correctly convert to protobuf and back" in {
    val levelInfo = GeoGridLevelInfo(level = 1, values = Range(1, 2), color = Color("#f87c19"), description = "Хей")
    val settings = GeoGridSettings(name = "ecology-1", `type` = "ecology", geoId = 1, levels = Seq(levelInfo))
    val message = GeoGridSettingsProtoConverter.toMessage(settings)
    val newSettings = GeoGridSettingsProtoConverter.fromMessage(message)
    newSettings shouldEqual settings
  }

}
