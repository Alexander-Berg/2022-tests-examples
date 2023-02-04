package ru.yandex.realty.model.serialization.heatmap

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.graphics.Color
import ru.yandex.realty.model.heatmap.GeoGridLevelInfo
import ru.yandex.realty.model.serialization.RealtySchemaVersions

@RunWith(classOf[JUnitRunner])
class GeoGridLevelInfoProtoConverterTest extends FlatSpec with Matchers {

  "GeoGridLevelInfoProtoConverter" should "correctly convert GeoGridLevelInfo to protobuf message and back" in {
    val levelInfo = GeoGridLevelInfo(level = 1, values = 1 until 2, color = Color("#f87c19"), description = "Йоу")
    val message = GeoGridLevelInfoProtoConverter.toMessage(levelInfo)
    val newLevelInfo = GeoGridLevelInfoProtoConverter.fromMessage(message)
    newLevelInfo shouldEqual levelInfo
  }

}
