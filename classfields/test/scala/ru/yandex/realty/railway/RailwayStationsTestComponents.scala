package ru.yandex.realty.railway

import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.model.message.ExtDataSchema.{RailwayDirection, RailwayStation}
import ru.yandex.realty.model.message.RealtySchema.GeoPointMessage

import scala.collection.JavaConverters._

trait RailwayStationsTestComponents {
  val railwayStationsProvider: Provider[RailwayStationsStorage] = RailwayStationsTestComponents.railwayStationsProvider
}

object RailwayStationsTestComponents {

  val finlyandsky =
    buildRailwayStation(38205, "Санкт-Петербург (Финляндский вокзал)", 59.9562577015f, 30.3571142897f)
  val lanskaya = buildRailwayStation(38215, "Ланская", 59.995454f, 30.326733f)
  val udelnaya = buildRailwayStation(38525, "Удельная", 60.016983f, 30.313561f)
  val ozerki = buildRailwayStation(38417, "Озерки", 60.039047f, 30.301782f)
  val shuvalovo = buildRailwayStation(38506, "Шувалово", 60.046448f, 30.297486f)
  val pargolovo = buildRailwayStation(38510, "Парголово", 60.081148f, 30.258503f)

  val spbVybDirection = buildRailwayDirection(
    "spb_vyb",
    "Выборгское",
    10174,
    Seq(finlyandsky, lanskaya, udelnaya, ozerki, shuvalovo, pargolovo)
  )
  val railwayStationsStorage = new RailwayStationsStorage(Seq(spbVybDirection))

  val railwayStationsProvider: Provider[RailwayStationsStorage] = () => railwayStationsStorage

  private def buildRailwayStation(esr: Int, title: String, latitude: Float, longitude: Float): RailwayStation = {
    RailwayStation
      .newBuilder()
      .setEsr(esr)
      .setTitle(title)
      .setPoint(GeoPointMessage.newBuilder().setLatitude(latitude).setLongitude(longitude))
      .build()
  }

  private def buildRailwayDirection(
    code: String,
    title: String,
    geoId: Int,
    railwayStations: Seq[RailwayStation]
  ): RailwayDirection = {
    RailwayDirection
      .newBuilder()
      .setCode(code)
      .setTitle(title)
      .setGeoId(geoId)
      .addAllStations(railwayStations.asJava)
      .build()
  }
}
