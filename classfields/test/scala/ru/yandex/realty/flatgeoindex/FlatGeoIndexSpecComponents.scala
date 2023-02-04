package ru.yandex.realty.flatgeoindex

import ru.yandex.realty.model.geometry.Polygon
import ru.yandex.realty.model.location.{Park, ParkType, Pond, PondType}

trait FlatGeoIndexSpecComponents {
  lazy val nearbyPolygons: Seq[Polygon] = createNearbyPolygons()

  lazy val farPolygon = new Polygon(Array(61.03f, 61.04f, 61.04f, 61.03f), Array(60.03f, 60.03f, 60.04f, 60.04f))

  lazy val nearbyPonds: Seq[Pond] = createNearbyPonds()

  lazy val farPond: Pond = createPond(123L, farPolygon)

  lazy val nearbyParks: Seq[Park] = createNearbyParks()

  lazy val farPark: Park = createPark(123L, farPolygon)

  lazy val pondIndex: FlatGeoIndex[Long, Pond] =
    FlatGeoIndex[Long, Pond]((nearbyPonds ++ Seq(farPond)).map(pond => pond.getId -> pond).toMap)(_.getGeometry)

  lazy val parkIndex: FlatGeoIndex[Long, Park] =
    FlatGeoIndex[Long, Park]((nearbyParks ++ Seq(farPark)).map(park => park.getId -> park).toMap)(_.getGeometry)

  private def createNearbyPonds(): Seq[Pond] = {
    for {
      (polygon, i) <- nearbyPolygons.zipWithIndex
    } yield createPond(i.toLong, polygon)
  }

  private def createPond(id: Long, polygon: Polygon): Pond = {
    new Pond(id, s"pond$id", PondType.POND, s"address$id", polygon)
  }

  private def createNearbyParks(): Seq[Park] = {
    for {
      (polygon, i) <- nearbyPolygons.zipWithIndex
    } yield createPark(i.toLong, polygon)
  }

  private def createPark(id: Long, polygon: Polygon): Park = {
    new Park(id, s"park$id", ParkType.PARK, s"address$id", polygon)
  }

  private def createNearbyPolygons(): Seq[Polygon] = {
    for {
      i <- Range.inclusive(1, 3)
      minLatitude = 60.0f + (i - 1) * FlatGeoIndex.DEFAULT_STEP_LATITUDE
      maxLatitude = minLatitude + i * FlatGeoIndex.DEFAULT_STEP_LATITUDE
      j <- Range.inclusive(1, 3)
      minLongitude = 60.0f + (j - 1) * FlatGeoIndex.DEFAULT_STEP_LONGITUDE
      maxLongitude = minLongitude + j * FlatGeoIndex.DEFAULT_STEP_LONGITUDE
    } yield new Polygon(
      Array(minLatitude, maxLatitude, maxLatitude, minLatitude),
      Array(minLongitude, minLongitude, maxLongitude, maxLongitude)
    )
  }
}
