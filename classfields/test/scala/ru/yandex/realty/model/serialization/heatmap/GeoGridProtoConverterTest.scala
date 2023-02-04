package ru.yandex.realty.model.serialization.heatmap

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.heatmap.GeoGrid

/**
  * @author azakharov
  */

@RunWith(classOf[JUnitRunner])
class GeoGridProtoConverterTest extends FlatSpec with Matchers {

  "GeoGridProtoConverter" should "correctly convert to protobuf message and back" in {
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
    val message = GeoGridProtoConverter.toMessage(grid)
    val newGrid = GeoGridProtoConverter.fromMessage(message)
    newGrid shouldEqual grid
  }
}
