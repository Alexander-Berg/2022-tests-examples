package ru.yandex.realty.geohub.distance

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.flatgeoindex.GeoIndex.NearbyElement
import ru.yandex.realty.model.geometry.Polygon
import ru.yandex.realty.model.location.{GeoPoint, Park, ParkDistance, ParkType}
import ru.yandex.realty.transit.{Transit, TransitPart, Transport, Wait, Walk}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ParkDistanceServiceSpec extends SpecBase {
  private val parkPolygon = new Polygon(Array(65.03f, 65.04f, 65.04f, 65.03f), Array(63.03f, 63.03f, 63.04f, 63.04f))

  private val park = new Park(123L, "park", ParkType.PARK, "address", parkPolygon)

  private val nearestPoint = new GeoPoint(65.03f, 63.03f)

  private val parkPoint = NearbyElement(park, nearestPoint, 1000)

  "ParkDistanceServiceSpec in getDistance" should {

    val b = Transit.builder()
    b.setDist(10)
    b.setTime(100)
    b.setTransfers(2)
    b.setPoints(
      Seq[GeoPoint](
        new GeoPoint(1, 2),
        new GeoPoint(5, 6),
        new GeoPoint(65.03f, 65.04f)
      ).asJava
    )

    "return None if transit contains transport part" in {
      b.setTransitParts(Seq[TransitPart](new Wait(1), new Walk(2, 3), new Transport(5)).asJava)
      val transit = b.build()

      val result = ParkDistanceService.getDistance(parkPoint, Seq[Transit](transit))

      val expectedResult = None

      result shouldEqual expectedResult
    }

    "return Some if transit does not contain transport part" in {
      b.setTransitParts(Seq[TransitPart](new Wait(1), new Walk(2, 3)).asJava)
      val transit = b.build()

      val result = ParkDistanceService.getDistance(parkPoint, Seq[Transit](transit)).get

      val expectedResult = ParkTransit(
        parkPoint.element,
        new ParkDistance(parkPoint.element.getId, transit.getTime, transit.getWalkingDist, parkPoint.point),
        522010.7226297134
      )

      result shouldEqual expectedResult
    }

    "return Some(transit) with mim time" in {
      b.setTransitParts(Seq[TransitPart](new Wait(1), new Walk(2, 3)).asJava)
      val transit1 = b.build()
      b.setTime(50)
      b.setDist(5)
      val transit2 = b.build()

      val result = ParkDistanceService.getDistance(parkPoint, Seq[Transit](transit1, transit2)).get

      val expectedResult = ParkTransit(
        parkPoint.element,
        new ParkDistance(parkPoint.element.getId, transit2.getTime, transit2.getWalkingDist, parkPoint.point),
        522010.7226297134
      )

      result shouldEqual expectedResult
    }
  }
}
