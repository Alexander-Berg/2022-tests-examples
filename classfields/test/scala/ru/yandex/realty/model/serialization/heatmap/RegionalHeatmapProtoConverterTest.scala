package ru.yandex.realty.model.serialization.heatmap

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.graphics.Color
import ru.yandex.realty.model.heatmap.{GeoGrid, GeoGridLevelInfo, GeoGridSettings, RegionalHeatmap}

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class RegionalHeatmapProtoConverterTest extends FlatSpec with Matchers {

  "RegionalHeatmapProtoConverter" should "correctly convert RegionalHeatmap to protobuf message and back" in {
    val levelInfo = GeoGridLevelInfo(level = 1, values = 1 until 2, color = Color("#f87c19"), description = "Хей")
    val settings = GeoGridSettings(name = "ecology-1", `type` = "ecology", geoId = 1, levels = Seq(levelInfo))
    val grid = new GeoGrid(
      latitude = 30f,
      longitude = 60f,
      stepLatitude = 1f,
      stepLongitude = 1f,
      values = Array(Array(1)),
      geoId = 1,
      hasRawValues = true,
      rawValues = Array(Array(1.0f))
    )
    val heatmap = RegionalHeatmap(grid = grid, settings = settings)
    val message = RegionalHeatmapProtoConverter.toMessage(heatmap)
    val newHeatmap = RegionalHeatmapProtoConverter.fromMessage(message)
    newHeatmap shouldEqual heatmap
  }
}
